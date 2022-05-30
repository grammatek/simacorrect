package org.grammatek.simacorrect.spellcheckerservice

import android.os.Build
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
    private val _annotations: List<Annotations> = response?.result?.get(0)?.get(0)?.annotations ?: throw NullPointerException("annotations null"),
    private val _originalText: String = response?.text ?: throw NullPointerException("original text null")
) {
    val suggestionsIndexes: MutableList<SuggestionIndexes> = mutableListOf()
    // Credit to https://github.com/hinrikur/gc_wagtail for classifying spelling error codes
    private val yfirlesturCodes: Map<String, String> = mapOf(
        "C" to "grammar", // Compound error
        "N" to "grammar", // Punctuation error - N
        "P" to "grammar", // Phrase error - P
        "W" to "grammar", // Spelling suggestion - W (not used in GreynirCorrect atm)
        "Z" to "typo", // Capitalization error - Z
        "A" to "typo", // Abbreviation - A
        "S" to "typo", // Spelling error - S
        "U" to "inactive", // Unknown word - U (nothing can be done)
        "E" to "inactive" // Error in parsing step
    )

    /**
     * For storing the starting index, end index and length for all annotations.
     * Used by the spell checker service to determine where to place annotations.
     */
    class SuggestionIndexes(
        val startChar: Int,
        endChar: Int,
    ) {
        var length: Int = endChar - startChar + 1
    }

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
        // Necessary to distinguish between cases where single word annotations
        // are inside of multi word annotations.
        val annotationsList = _annotations.groupBy { it.toKey() }
        val suggestionList = mutableListOf<SuggestionsInfo>()

        for((_, annotations) in annotationsList) {
            val suggestions = mutableListOf<String>()
            var flag = 0
            var startChar = 0
            var endChar = 0
            var sequence = 0
            for (annotation in annotations) {
                if(annotation.code == null) {
                    continue
                }
                sequence = getSequence(annotation.startChar!!, annotation.endChar!!)
                flag = determineSuggestionFlag(annotation.code.toString())

                val suggestion: String = correctSuggestion(
                    annotation.suggest ?: "", annotation.end!!-annotation.start!!
                )

                Log.d(TAG, "adding: $suggestion as a suggestion at index: ${annotation.start}")
                // Take into account that Yfirlestur includes whitespaces in their annotation
                startChar = if (annotation.startChar!! != 0) {
                    annotation.startChar!! + 1
                } else {
                    annotation.startChar!!
                }
                endChar = annotation.endChar!!
                if (suggestion.isNotEmpty() && annotation.suggest != null) {
                    suggestions.add(suggestion)
                }
            }
            // We assign the cookie to 0 and re-assign it upstream where we have access to it.
            suggestionList.add(SuggestionsInfo(flag, suggestions.toTypedArray(), 0, sequence))
            suggestionsIndexes.add(
                SuggestionIndexes(startChar, endChar)
            )
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
        if(suggestion == "") {
            return suggestion
        }
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
        // First character of the code indicates which type of error.
        return when {
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) &&
            yfirlesturCodes[code[0].toString()] == "grammar" -> {
                SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_GRAMMAR_ERROR
            }
            yfirlesturCodes[code[0].toString()] == "typo" -> {
                SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO
            }
            // Inactive are cases where we annotate but don't have suggestions.
            yfirlesturCodes[code[0].toString()] == "inactive" -> {
                SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO
            }
            else -> {
                0 // do nothing
            }
        }
    }

    companion object {
        private val TAG = YfirlesturAnnotation::class.java.simpleName
    }
}
