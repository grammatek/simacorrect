package org.grammatek.simacorrect.spellcheckerservice

import android.os.Build
import android.view.textservice.SuggestionsInfo
import org.grammatek.models.Annotations
import org.grammatek.models.YfirlesturResponse
import java.lang.Exception
import java.lang.NullPointerException

/**
 * Resolves Yfirlestur's response and constructs
 * [SuggestionsInfo] for annotated words.
 *
 * @param [response] is the entire JSON response from Yfirlestur's API.
 */
class YfirlesturAnnotation(
    response: YfirlesturResponse?,
    text: String?,
    _response: YfirlesturResponse? = response,
    _originalText: String = response?.text ?: throw NullPointerException("original text null"),
    private val _unalteredOriginalText: String? = text
) {
    private val _annotations: List<List<Annotations>>
    val suggestionsIndices: MutableList<AnnotationIndices> = mutableListOf()
    val tokensIndices: MutableList<MutableList<AnnotationIndices>> = mutableListOf()
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
    class AnnotationIndices (
        val startChar: Int,
        val endChar: Int,
    ) {
        var length: Int = endChar - startChar + 1
    }

    /**
     * Creates a [Key] data class from the
     * annotation.start and annotation.end indices.
     * Used to identify which annotations belong together.
     */
    private fun Annotations.toKey() = Key(start, end)
    data class Key(val startIndex: Int?, val endIndex: Int?)

    /**
     * Iterates through all the [_annotations] and builds
     * a list of [SuggestionsInfo] using the data in the [_annotations] as
     * well as determining the type of spelling error.
     *
     * @returns List<SuggestionsInfo>
     */
    fun getSuggestionsForAnnotatedWords(suggestionsLimit: Int, dict: ArrayList<String>): List<SuggestionsInfo> {
        val suggestionList = mutableListOf<SuggestionsInfo>()
        for (i in _annotations.indices) {
            // Group annotations in a list that have the same start AND end index.
            // Necessary to distinguish between cases where single word annotations
            // are inside of multi word annotations.
            val sentenceAnnotations = _annotations[i].groupBy { it.toKey() }
            for((_, tokenAnnotations) in sentenceAnnotations) {
                val suggestions = mutableListOf<String>()
                val flags = mutableListOf<Int>()
                var startChar = 0
                var endChar = 0

                for (annotation in tokenAnnotations) {
                    val startCharIndexedForUnalteredText = tokensIndices[i][annotation.start!!].startChar
                    val endCharIndexedForUnalteredText = tokensIndices[i][annotation.end!!].endChar
                    val word = _unalteredOriginalText!!.substring(startCharIndexedForUnalteredText, endCharIndexedForUnalteredText + 1)
                    if(annotation.code == null || dict.contains(word)) {
                        continue
                    }

                    val flag = determineSuggestionFlag(annotation.code.toString())
                    if(flag == 0) {
                        continue
                    }
                    flags.add(flag)

                    var suggestion = annotation.suggest
                    val annotationTokenStartIndex = annotation.start!!
                    val annotationTokenEndIndex = annotation.end!!

                    startChar = tokensIndices[i][annotationTokenStartIndex].startChar
                    endChar = tokensIndices[i][annotationTokenEndIndex].endChar
                    if (suggestions.size < suggestionsLimit && suggestion != null) {
                        // The request sent to Yfirlestur is capitalized before being sent to get a
                        // useful spell/grammar correction, this is the code that makes sure we put
                        // the text back to lowercase (if it was so in the first place).
                        if (annotation.start == 0 && _unalteredOriginalText[startChar].isLowerCase()) {
                            suggestion = suggestion.replaceFirstChar { it.lowercaseChar() }
                        }
                        suggestions.add(suggestion)
                    }
                }
                if(flags.isNotEmpty()) {
                    val flag = flags.distinct().sum()
                    suggestionList.add(SuggestionsInfo(flag, suggestions.toTypedArray()))
                    suggestionsIndices.add(
                        AnnotationIndices(startChar, endChar)
                    )
                }
            }
        }
        return suggestionList
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
                // If we don't recognize a code we still want to annotate it.
                SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO
            }
        }
    }

    init {
        // Find which first N characters get trimmed by Yfirlestur.
        val indexOfFirstCharacter = _unalteredOriginalText!!.lowercase().indexOf(_originalText.lowercase())
        if (indexOfFirstCharacter < 0) {
            throw Exception("input text and yfirlestur text differ")
        }

        // Group annotations by sentence
        val annotationList: ArrayList<List<Annotations>> = arrayListOf()
        for (results in _response?.result!!) {
            for (r in results) {
                val annotations = arrayListOf<Annotations>()
                for (annotation in r.annotations!!) {
                    annotations.add(annotation)
                }
                annotationList.add(annotations)
            }
        }
        _annotations = annotationList.toList()

        // Start by iterating through the tokens from yfirlestur and creating our own
        // start and end indices since we can't rely on the indices given.
        for (results in _response.result!!) {
            var startChar = 0
            var endChar = -1 // to account for index starting at 0
            for (r in results) {
                val tokens = r.tokens!!.groupBy { it.i }
                val annotationIndices = mutableListOf<AnnotationIndices>()
                for (t in tokens) {
                    // If 2 items get grouped it's because the token before is being split into two.
                    // In that case we can safely assume that the latter of the grouped tokens is the
                    // correct one. See https://github.com/mideind/Yfirlestur/issues/11 for details.
                    val tokenOriginal = t.value.last().o!!
                    if (t.value.size > 1) {
                        annotationIndices.add(
                            AnnotationIndices(startChar + indexOfFirstCharacter, endChar + indexOfFirstCharacter)
                        )
                    }
                    endChar += tokenOriginal.length
                    startChar = endChar - tokenOriginal.trim().length + 1
                    annotationIndices.add(
                        AnnotationIndices(startChar + indexOfFirstCharacter, endChar + indexOfFirstCharacter)
                    )
                }
                tokensIndices.add(annotationIndices)
            }
        }
    }
}
