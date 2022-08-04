package org.grammatek.simacorrect.spellcheckerservice

import android.content.ContentResolver
import android.database.ContentObserver
import android.database.Cursor
import android.provider.UserDictionary.Words
import android.service.textservice.SpellCheckerService
import android.util.Log
import android.view.textservice.*
import org.grammatek.simacorrect.network.ConnectionManager

/**
 * Implements Simacorrect spell checking as a SpellCheckerService
 */
class SimaCorrectSpellCheckerService : SpellCheckerService() {
    override fun createSession(): Session {
        return AndroidSpellCheckerSession(contentResolver)
    }

    private class AndroidSpellCheckerSession(contentResolver: ContentResolver): Session() {
        private lateinit var _locale: String
        private var _contentResolver: ContentResolver = contentResolver
        private var _userDict: ArrayList<String> = ArrayList()

        override fun onCreate() {
            _locale = locale
            loadUserDictionary()

            // Register a listener for changes in the user dictionary.
            _contentResolver.registerContentObserver(
                Words.CONTENT_URI,
                false,
                object : ContentObserver(null) {
                    override fun onChange(selfChange: Boolean) {
                        Log.d(TAG, "Observed changes in user dictionary")
                        loadUserDictionary()
                    }
                }
            )
        }

        /**
         * Synchronously loads the user's dictionary into the spell checker session.
         */
        @Synchronized
        private fun loadUserDictionary() {
            Log.d(TAG, "loadUserDictionary")
            // from user dictionary, query for words with locale = "_locale"
            val cursor: Cursor = _contentResolver.query(Words.CONTENT_URI, arrayOf(Words.WORD),
                "${Words.LOCALE} = ?", arrayOf(_locale), null) ?: return
            val index = cursor.getColumnIndex(Words.WORD)
            val words = ArrayList<String>()
            while (cursor.moveToNext()) {
                words.add(cursor.getString(index))
            }
            cursor.close()
            _userDict = words
        }

        override fun onGetSuggestionsMultiple(
            textInfos: Array<TextInfo?>,
            suggestionsLimit: Int, sequentialWords: Boolean
        ): Array<SuggestionsInfo> {
            TODO("Not to be implemented")
        }

        override fun onGetSentenceSuggestionsMultiple(
            textInfos: Array<out TextInfo>?,
            suggestionsLimit: Int,
        ): Array<SentenceSuggestionsInfo?> {
            Log.d(TAG, "onGetSentenceSuggestionsMultiple: ${textInfos?.size}")
            if(textInfos == null || textInfos.isEmpty()) {
                return emptyArray()
            }
            val retval = arrayOfNulls<SentenceSuggestionsInfo>(textInfos.size)
            for (i in textInfos.indices) {
                val suggestionList: Array<SuggestionsInfo>
                val suggestionsIndices: Array<YfirlesturAnnotation.AnnotationIndices>
                try {
                    val text = textInfos[i].text!!
                    if (text.isBlank()) {
                        continue
                    }

                    val response = ConnectionManager.correctSentence(text)
                    val ylAnnotation = YfirlesturAnnotation(response, textInfos[i].text)
                    suggestionList = ylAnnotation.getSuggestionsForAnnotatedWords(suggestionsLimit, _userDict).toTypedArray()
                    suggestionsIndices = ylAnnotation.suggestionsIndices.toTypedArray()
                } catch (e: Exception) {
                    Log.e(TAG, "onGetSentenceSuggestionsMultiple: ${e.message} ${e.stackTrace.joinToString("\n")}")
                    return emptyArray()
                }

                retval[i] = reconstructSuggestions(
                    suggestionList,
                    suggestionsIndices,
                    textInfos[i]
                )
            }
            return retval
        }

        fun reconstructSuggestions(
            results: Array<SuggestionsInfo>,
            resultsIndices: Array<YfirlesturAnnotation.AnnotationIndices>,
            originalTextInfo: TextInfo
        ): SentenceSuggestionsInfo? {
            if (results.isEmpty() || originalTextInfo.text.isEmpty()) {
                return null
            }
            val offsets = IntArray(results.size)
            val lengths = IntArray(results.size)
            val reconstructedSuggestions = arrayOfNulls<SuggestionsInfo>(results.size)
            for (i in results.indices) {
                offsets[i] = resultsIndices[i].startChar
                lengths[i] = resultsIndices[i].length
                val result: SuggestionsInfo = results[i]

                if (result.suggestionsCount == 0) {
                    reconstructedSuggestions[i] = SuggestionsInfo(0, null)
                } else {
                    reconstructedSuggestions[i] = result
                    result.setCookieAndSequence(originalTextInfo.cookie, originalTextInfo.sequence)
                }
            }
            return SentenceSuggestionsInfo(reconstructedSuggestions, offsets, lengths)
        }

        override fun onGetSuggestions(textInfo: TextInfo?, suggestionsLimit: Int): SuggestionsInfo {
            TODO("Not to be implemented")
        }

        companion object {
            private val TAG = SimaCorrectSpellCheckerService::class.java.simpleName
        }
    }
}