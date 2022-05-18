package org.grammatek.simacorrect.spellcheckerservice

import android.util.Log
import android.view.textservice.SuggestionsInfo
import org.grammatek.models.Annotations

class YfirlesturAnnotation(
    private val _annotations: List<Annotations>,
    private val _originalText: String
) {
    data class Key(val startIndex: Int?, val endIndex: Int?)
    private fun Annotations.toKey() = Key(start, end)

    fun getSuggestionsForAnnotatedWords(): List<SuggestionsInfo> {
        // Group annotations in a list that have the same start AND end index.
        val annotationsList = _annotations.groupBy { it.toKey() }
        val suggestionList = mutableListOf<SuggestionsInfo>()

        for((_, annotations) in annotationsList) {
            var flag = 0
            val suggestions = mutableListOf<String>()
            var sequence = 0
            for (annotation in annotations) {
                if(annotation.suggest == null || annotation.code == null) {
                    continue
                }
                sequence = getSequence(annotation.startChar!!, annotation.endChar!!)
                flag = determineSuggestionFlag(annotation.code.toString())

                val suggestion = validateSuggestion(
                    annotation.suggest.toString(), annotation.end!!-annotation.start!!
                )
                // Avoid duplicate suggestions
                if(!suggestions.contains(suggestion)) {
                    Log.d(TAG, "adding: $suggestion as a suggestion at index: ${annotation.start}")
                    suggestions.add(suggestion)
                }
            }
            if(suggestions.isNotEmpty()){
                // We assign the cookie to 0 and re-assign it upstream where we have access to it.
                suggestionList.add(SuggestionsInfo(flag, suggestions.toTypedArray(), 0, sequence))
            }
        }
        return suggestionList
    }

    private fun getSequence(startChar: Int, endChar: Int): Int {
        val sequenceString = _originalText.substring(startChar, endChar+1).trim()
        return sequenceString.hashCode()
    }

    private fun validateSuggestion(suggestion: String, length: Int): String {
        // split suggestion into tokens (words) to help determine which words of the
        // suggestion are meant to ACTUALLY be suggested. This is due to Yfirlestur's
        // way of recommending for multiple word annotation for grammar errors.
        // see https://github.com/mideind/Yfirlestur/issues/7 for clarity.
        val suggestionSplitIntoWords = suggestion.trim().split(" ").toMutableList()
        var validatedSuggestion = ""
        for(i in 0 until length + 1) {
            validatedSuggestion += "${suggestionSplitIntoWords[i]} "
        }
        return validatedSuggestion.trim()
    }

    private fun determineSuggestionFlag(code: String): Int {
        // TODO: GreynirCorrect contains all the annotation.codes but it's unclear which
        //  are grammar errors. However 'P_WRONG' covers a good amount of them, if not all.
        return if (code.contains("P_WRONG")) {
            SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_GRAMMAR_ERROR
        } else {
            SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO
        }
    }

    companion object {
        private val TAG = YfirlesturAnnotation::class.java.simpleName
    }
}