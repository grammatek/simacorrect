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
            textInfos: Array<TextInfo>,
            suggestionsLimit: Int, sequentialWords: Boolean
        ): Array<SuggestionsInfo?> {
            Log.d(TAG, "onGetSuggestionsMultiple: ${textInfos.size}")
            return super.onGetSuggestionsMultiple(textInfos, suggestionsLimit, sequentialWords)
        }

        override fun onGetSentenceSuggestionsMultiple(
            textInfos: Array<out TextInfo>?,
            suggestionsLimit: Int,
        ): Array<SentenceSuggestionsInfo> {
            Log.d(TAG, "onGetSentenceSuggestionsMultiple: ${textInfos?.size}")
            return super.onGetSentenceSuggestionsMultiple(textInfos, suggestionsLimit)
        }

        /**
         * Fetches spell checking suggestions.
         *
         * @param [textInfo] contains the text that this method fetches suggestions for.
         * @param [suggestionsLimit] the limit of suggestions given.
         * @return [SuggestionsInfo]
         */
        override fun onGetSuggestions(textInfo: TextInfo, suggestionsLimit: Int): SuggestionsInfo {
            Log.d(TAG, "onGetSuggestions: " + textInfo.text)
            // The API we're calling returns a value with the first letter in uppercase and since we're
            // calling it for each word we need to compare the original word with the first char in uppercase
            // against the corrected word.
            var flags = 0
            val suggestions = mutableListOf<String>()
            if(_userDict.contains(textInfo.text)) {
                flags = SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY
            }
            else {
                val correctedWord = ConnectionManager.correctWord(textInfo.text)
                if(correctedWord != "") {
                    flags = SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO
                    suggestions.add(correctedWord)
                }
            }
            return SuggestionsInfo(flags, suggestions.toTypedArray(), textInfo.cookie, textInfo.sequence)
        }

        companion object {
            private val TAG = SimaCorrectSpellCheckerService::class.java.simpleName
        }
    }
}