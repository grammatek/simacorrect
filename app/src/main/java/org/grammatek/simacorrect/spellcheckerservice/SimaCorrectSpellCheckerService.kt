package org.grammatek.simacorrect.spellcheckerservice

import android.database.Cursor
import android.content.ContentResolver
import android.service.textservice.SpellCheckerService
import android.util.Log
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import android.view.textservice.SentenceSuggestionsInfo
import org.grammatek.apis.DevelopersApi
import java.lang.Exception
import java.lang.NullPointerException
import android.provider.UserDictionary.Words
import android.database.ContentObserver

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

            // Register a listener for changes in the user dictionary,
            // reload the user dictionary if we detect changes
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

        @Synchronized
        private fun loadUserDictionary() {
            Log.d(TAG, "loadUserDictionary")
            // from user dictionary, query for words with locale = "_locale"
            val cursor: Cursor? = _contentResolver.query(Words.CONTENT_URI, arrayOf(Words.WORD),
                "${Words.LOCALE} = ?", arrayOf(_locale), null) ?: return
            val index = cursor?.getColumnIndex(Words.WORD) ?: return
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
        ): Array<SuggestionsInfo?>? {
            Log.d(TAG, "onGetSuggestionsMultiple: " + textInfos[0].text)
            val length = textInfos.size
            val retval = arrayOfNulls<SuggestionsInfo>(length)

            for (i in 0 until length) {
                retval[i] = onGetSuggestions(textInfos[i], suggestionsLimit)
                retval[i]?.setCookieAndSequence(
                    textInfos[i].cookie, textInfos[i].sequence
                )
            }
            return retval
        }

        override fun onGetSentenceSuggestionsMultiple(
            textInfos: Array<out TextInfo>?,
            suggestionsLimit: Int,
        ): Array<SentenceSuggestionsInfo> {
            Log.d(TAG, "onGetSentenceSuggestionsMultiple:")
            return super.onGetSentenceSuggestionsMultiple(textInfos, suggestionsLimit)
        }

        override fun onGetSuggestions(textInfo: TextInfo, suggestionsLimit: Int): SuggestionsInfo {
            Log.d(TAG, "onGetSuggestions: " + textInfo.text)
            // The API we're calling returns a value with the first letter in uppercase and since we're
            // calling it for each word we need to compare the original word with the first char in uppercase
            // against the corrected word.
            val text: String = textInfo.text.replaceFirstChar {
                it.uppercase()
            }
            var flags = 0
            val suggestions = mutableListOf<String>()

            if(_userDict.contains(textInfo.text)) {
                flags = SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY
            }
            else {
                try {
                    val api = DevelopersApi()
                    val response = api.correctApiPost(text)
                    val correctedText = response.result?.get(0)?.get(0)?.corrected
                        ?: throw NullPointerException("Received null value from response corrected")

                    if (text != correctedText) {
                        flags = SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO
                        // Return word with the first char as uppercase if it was so originally.
                        // Only necessary because of the specifications set by the API endpoint.
                        val retval = correctedText.replaceFirstChar {
                            it.lowercase()
                        }
                        suggestions.add(retval)
                    }

                } catch (e: Exception) {
                    println("Exception: ${e.printStackTrace()}")
                }
            }
            return SuggestionsInfo(flags, suggestions.toTypedArray(), textInfo.cookie, textInfo.sequence)
        }

        companion object {
            private val TAG = SimaCorrectSpellCheckerService::class.java.simpleName
            private const val DBG = true
        }
    }
}