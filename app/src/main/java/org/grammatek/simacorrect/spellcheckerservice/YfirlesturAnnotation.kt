package org.grammatek.simacorrect.spellcheckerservice

import android.util.Log
import android.view.textservice.SuggestionsInfo
import org.grammatek.models.Annotations
import org.grammatek.models.YfirlesturResponse
import java.lang.NullPointerException

/**
 * Resolves Yfirlestur's response and constructs
 * [SuggestionsInfo] for annotated words.
 *
 * @param [response] is the entire JSON response from Yfirlestur's API.
 */
class YfirlesturAnnotation(
    response: YfirlesturResponse?,
    private val _annotations: List<Annotations> = response?.result?.get(0)?.get(0)?.annotations ?: throw NullPointerException(),
    private val _originalText: String = response?.text ?: throw NullPointerException()
) {
    val suggestionsIndexes: MutableList<SuggestionIndexes> = mutableListOf()

    class SuggestionIndexes(
        val startChar: Int,
        val endChar: Int,
    )

    /**
     * Creates a [Key] data class from the
     * annotation.start and annotation.end indexes.
     * Used to identify which annotations belong together.
     */
    private fun Annotations.toKey() = Key(start, end)
    data class Key(val startIndex: Int?, val endIndex: Int?)

    /**
     * Iterates through all the [_annotations] and builds
     * a [SuggestionsInfo] using data in the [_annotations] as
     * well as determining the type of spelling error.
     *
     * @returns List<SuggestionsInfo>
     */
    fun getSuggestionsForAnnotatedWords(): List<SuggestionsInfo> {
        // Group annotations in a list that have the same start AND end index.
        val annotationsList = _annotations.groupBy { it.toKey() }
        val suggestionList = mutableListOf<SuggestionsInfo>()

        for((_, annotations) in annotationsList) {
            var flag = 0
            val suggestions = mutableListOf<String>()
            var startChar = -1
            var endChar = -1
            var sequence = 0
            for (annotation in annotations) {
                if(annotation.suggest == null || annotation.code == null) {
                    continue
                }
                sequence = getSequence(annotation.startChar!!, annotation.endChar!!)
                flag = determineSuggestionFlag(annotation.code.toString())

                val suggestion = correctSuggestion(
                    annotation.suggest.toString(), annotation.end!!-annotation.start!!
                )
                // Avoid duplicate suggestions
                if(!suggestions.contains(suggestion)) {
                    Log.d(TAG, "adding: $suggestion as a suggestion at index: ${annotation.start}")
                    // Yfirlestur takes will identify whitespaces as start of annotation (while android does not)
                    startChar = if (annotation.startChar!! != 0){
                        annotation.startChar!! + 1
                    } else {
                        annotation.startChar!!
                    }
                    endChar = annotation.endChar!!
                    suggestions.add(suggestion)
                }
            }
            if(suggestions.isNotEmpty()) {
                // We assign the cookie to 0 and re-assign it upstream where we have access to it.
                suggestionList.add(SuggestionsInfo(flag, suggestions.toTypedArray(), 0, sequence))
                if(startChar >= 0 && endChar >= 0) {
                    suggestionsIndexes.add(
                        SuggestionIndexes(startChar, endChar)
                    )
                }
            }
        }
        return suggestionList
    }

    /**
     * Finds the character sequence for given start and end index.
     *
     * @param startChar
     * @param endChar
     * @return The character sequence hashed.
     */
    private fun getSequence(startChar: Int, endChar: Int): Int {
        val sequenceString = _originalText.substring(startChar, endChar+1).trim()
        return sequenceString.hashCode()
    }

    /**
     * Corrects the suggestion if deemed incorrect.
     *
     * @param suggestion The suggestion provided by Yfirlestur
     * @param length The character length of the suggestion
     * @return The part of the [suggestion] which we evaluate to be correct
     */
    private fun correctSuggestion(suggestion: String, length: Int): String {
        // Split suggestion into tokens (words) to help determine which words of the
        // suggestion are meant to ACTUALLY be suggested. This is due to Yfirlestur's
        // way of recommending for multiple word annotation for grammar errors.
        // See https://github.com/mideind/Yfirlestur/issues/7 for clarity.
        val suggestionSplitIntoWords = suggestion.trim().split(" ").toMutableList()
        var validatedSuggestion = ""
        for(i in 0 until length + 1) {
            validatedSuggestion += "${suggestionSplitIntoWords[i]} "
        }
        return validatedSuggestion.trim()
    }

    /**
     * Determines to the best of its knowledge, the type of spelling error.
     *
     * @param code The GreynirCorrect's spelling error identifier
     * @return The type of spelling error
     */
    private fun determineSuggestionFlag(code: String): Int {
        // TODO: GreynirCorrect contains all the annotation.codes but it's unclear which
        //  are grammar errors. However 'P_WRONG' covers a good amount of them, if not all.
        return if (code.contains("P_WRONG")) {
            SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_GRAMMAR_ERROR
        } else {
            // we can assume it's a typo if it's not a grammar error.
            SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO
        }
    }

    companion object {
        private val TAG = YfirlesturAnnotation::class.java.simpleName
    }
}