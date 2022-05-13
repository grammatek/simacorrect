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
        ): Array<SuggestionsInfo> {
            val suggestionList = mutableListOf<SuggestionsInfo>()

            try {
                var textToCorrect = ""
                for (text in textInfos) {
                    textToCorrect += "${text.text} "
                }
                val response = ConnectionManager.correctSentence(textToCorrect)
                val annotation = response!!.result?.get(0)?.get(0)?.annotations ?: return arrayOf()
                if (annotation.isEmpty()) {
                    return arrayOf()
                }

                val tokens = response.result?.get(0)?.get(0)?.tokens ?: return arrayOf()

                // Assume all tokens (words) are "un-annotated" to reduce the complexity of
                // iterating through all the tokens and annotations.
                for (i in tokens.indices) {
                    suggestionList.add(
                        SuggestionsInfo(
                            0, arrayOf(tokens[i].x.toString()),
                            textInfos[i].cookie, textInfos[i].sequence
                        )
                    )
                }
                for(i in annotation.indices) {
                    // if a word has more than 1 annotation attached to it,
                    // most likely one of those annotations has a suggest = null
                    if (annotation[i].suggest == null) {
                        continue
                    }
                    // TODO: Likely not necessary to split when 'onGetSentenceSuggestionsMultiple' has been reworked.
                    val annotationSuggestion = annotation[i].suggest!!.replace("\\s+".toRegex(), " ").trim().split(" ").toMutableList()

                    for(j in annotation[i].start!!..annotation[i].end!!) {
                        // TODO: Subject to change with 'onGetSentenceSuggestionsMultiple' changes.
                        //  Currently this makes breaking words into two impossible.
                        val suggestion = annotationSuggestion.removeFirst()
                        var flag = 0
                        // TODO: GreynirCorrect contains all the annotation.codes but it's unclear which
                        //  are grammar errors. However 'P_WRONG' covers a good amount of them, if not all.
                        flag = if (annotation[i].code!!.contains("P_WRONG")) {
                            SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_GRAMMAR_ERROR
                        } else {
                            SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO
                        }
                        Log.d(TAG, "flag: $flag, suggestion: $suggestion")
                        suggestionList[j] = SuggestionsInfo(flag, arrayOf(suggestion), textInfos[j].cookie, textInfos[j].sequence)
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Exception: $e")
                return arrayOf()
            }

            for (i in suggestionList.indices) {
                suggestionList[i].setCookieAndSequence(textInfos[i].cookie, textInfos[i].sequence)
            }
            return suggestionList.toTypedArray()
        }

        override fun onGetSentenceSuggestionsMultiple(
            textInfos: Array<out TextInfo>?,
            suggestionsLimit: Int,
        ): Array<SentenceSuggestionsInfo> {
            Log.d(TAG, "onGetSentenceSuggestionsMultiple: ${textInfos?.size}")
            return super.onGetSentenceSuggestionsMultiple(textInfos, suggestionsLimit)
        }

        override fun onGetSuggestions(textInfo: TextInfo?, suggestionsLimit: Int): SuggestionsInfo {
            TODO("Not yet implemented")
        }

        companion object {
            private val TAG = SimaCorrectSpellCheckerService::class.java.simpleName
        }
    }
}