package org.grammatek.simacorrect.spellcheckerservice

import android.content.ContentResolver
import android.database.ContentObserver
import android.database.Cursor
import android.provider.UserDictionary.Words
import android.service.textservice.SpellCheckerService
import android.util.Log
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import org.grammatek.models.YfirlesturResponse
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
                })
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
                val suggestionsIndexes: Array<YfirlesturAnnotation.SuggestionIndexes>

                try {
                    val response = ConnectionManager.correctSentence(textInfos[i].text)
                    val ylAnnotation = YfirlesturAnnotation(response)
                    suggestionList = ylAnnotation.getSuggestionsForAnnotatedWords(suggestionsLimit).toTypedArray()
                    suggestionsIndexes = ylAnnotation.suggestionsIndexes.toTypedArray()
                } catch (e: Exception) {
                    Log.e(TAG, "onGetSentenceSuggestionsMultiple: ${e.message} ${e.stackTrace.joinToString("\n")}")
                    return emptyArray()
                }

                retval[i] = reconstructSuggestions(
                    suggestionList,
                    suggestionsIndexes,
                    textInfos[i]
                )
            }
            return retval
        }

        fun reconstructSuggestions(
            results: Array<SuggestionsInfo>,
            resultsIndexes: Array<YfirlesturAnnotation.SuggestionIndexes>,
            originalTextInfo: TextInfo
        ): SentenceSuggestionsInfo? {
            if (results.isEmpty()) {
                return null
            }
            val offsets = IntArray(results.size)
            val lengths = IntArray(results.size)
            val reconstructedSuggestions = arrayOfNulls<SuggestionsInfo>(results.size)

            for (i in results.indices) {
                offsets[i] = resultsIndexes[i].startChar
                lengths[i] = resultsIndexes[i].length
                val result: SuggestionsInfo = results[i]
                result.setCookieAndSequence(originalTextInfo.cookie, originalTextInfo.sequence)
                reconstructedSuggestions[i] = result
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