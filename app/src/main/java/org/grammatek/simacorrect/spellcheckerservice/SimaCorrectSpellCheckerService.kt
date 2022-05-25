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
import java.text.BreakIterator

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
            val suggestionList: Array<SuggestionsInfo>

            try {
                val textToCorrect = textInfos.joinToString(separator = " ") { it!!.text }
                val response = ConnectionManager.correctSentence(textToCorrect)
                val ylAnnotation = YfirlesturAnnotation(response)
                suggestionList = ylAnnotation.getSuggestionsForAnnotatedWords().toTypedArray()
                for(sl in suggestionList) {
                    sl.setCookieAndSequence(textInfos[0]!!.cookie, sl.sequence)
                }
            } catch (e: Exception) {
                Log.e(TAG, "onGetSuggestionsMultiple: Exception: $e")
                return emptyArray()
            }
            return suggestionList
        }

        private fun getSplitWords(originalTextInfo: TextInfo): SentenceTextInfoParams {
            val cookie = originalTextInfo.cookie
            val wordItems = ArrayList<SentenceWordItem>()
            val source = originalTextInfo.text
            val stringToExamine: String = originalTextInfo.text
            val boundary = BreakIterator.getWordInstance()
            boundary.setText(stringToExamine)

            var idx = 0
            while(boundary.next() != BreakIterator.DONE) {
                val word = source.subSequence(idx, boundary.current())
                // check if first character of string
                if(Character.isLetterOrDigit(word[0])) {
                    Log.d(TAG,"word: $word, idx: $idx, length: ${word.length}, bound: ${boundary.current()}")
                    val ti = TextInfo(
                        word, 0, word.length, cookie,
                        word.hashCode()
                    )
                    wordItems.add(SentenceWordItem(ti, idx, boundary.current()))
                }
                idx = boundary.current()
            }
            return SentenceTextInfoParams(originalTextInfo, wordItems)
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
                val textInfoParams = getSplitWords(textInfos[i])
                val mItems: ArrayList<SentenceWordItem> = textInfoParams.mItems

                val itemsSize = mItems.size
                val splitTextInfos = arrayOfNulls<TextInfo>(itemsSize)
                for (j in 0 until itemsSize) {
                    splitTextInfos[j] = mItems[j].mTextInfo
                }

                // TODO: Using the information from onGetSuggestionsMultiple, we must change the "textInfoParams: SentenceTextInfoParams"

                val suggestionList: Array<SuggestionsInfo>
                val suggestionsIndexes: Array<YfirlesturAnnotation.SuggestionIndexes>

                try {
                    val textToCorrect = splitTextInfos.joinToString(separator = " ") { it!!.text }
                    val response = ConnectionManager.correctSentence(textToCorrect)
                    val ylAnnotation = YfirlesturAnnotation(response)
                    suggestionList = ylAnnotation.getSuggestionsForAnnotatedWords().toTypedArray()
                    for(sl in suggestionList) {
                        sl.setCookieAndSequence(textInfos[0].cookie, sl.sequence)
                    }
                    suggestionsIndexes = ylAnnotation.suggestionsIndexes.toTypedArray()
                } catch (e: Exception) {
                    Log.e(TAG, "onGetSentenceSuggestionsMultiple: Exception: $e")
                    return emptyArray()
                }

                retval[i] = reconstructSuggestions(
                    textInfoParams,
                    suggestionList,
                    suggestionsIndexes,
                )
            }
            return retval
        }

        fun reconstructSuggestions(
            originalTextInfoParams: SentenceTextInfoParams?,
            results: Array<SuggestionsInfo>,
            resultsIndexes: Array<YfirlesturAnnotation.SuggestionIndexes>,
        ): SentenceSuggestionsInfo? {
            if (results.isEmpty()) {
                return null
            }
            if (originalTextInfoParams == null) {
                if (DBG) {
                    Log.w(TAG, "Adapter: originalTextInfoParams is null.")
                }
                return null
            }

            val originalCookie = originalTextInfoParams.mOriginalTextInfo.cookie
            val originalSequence = originalTextInfoParams.mOriginalTextInfo.sequence

            val offsets = IntArray(results.size)
            val lengths = IntArray(results.size)
            val reconstructedSuggestions = arrayOfNulls<SuggestionsInfo>(results.size)

            for (i in results.indices) {
                offsets[i] = resultsIndexes[i].startChar
                lengths[i] = resultsIndexes[i].endChar - resultsIndexes[i].startChar + 1 // TODO: add length to 'resultsIndexes'
                val result: SuggestionsInfo = results[i]

                result.setCookieAndSequence(originalCookie, originalSequence)
                reconstructedSuggestions[i] = result
                for(j in 0 until result.suggestionsCount) {
                    Log.d(TAG, "suggestion[$j]: ${result.getSuggestionAt(j)}")
                }
                Log.d(TAG, "seq: ${results[i].sequence}, seq2 ${result.sequence}")
                Log.d(TAG, "suggestion: ${result.getSuggestionAt(0)}, offset: ${offsets[i]}, length: ${lengths[i]}")
            }
            return SentenceSuggestionsInfo(reconstructedSuggestions, offsets, lengths)
        }

        /**
         * Container for originally queried TextInfo and parameters
         */
        class SentenceTextInfoParams(
            val mOriginalTextInfo: TextInfo,
            items: ArrayList<SentenceWordItem>
        ) {
            val mItems: ArrayList<SentenceWordItem> = items
            val mSize: Int = items.size
        }

        class SentenceWordItem(
            val mTextInfo: TextInfo,
            val mStart: Int,
            var end: Int
        ) {
            var mLength: Int = end - mStart
        }

        override fun onGetSuggestions(textInfo: TextInfo?, suggestionsLimit: Int): SuggestionsInfo {
            TODO("Not yet implemented")
        }

        companion object {
            private val TAG = SimaCorrectSpellCheckerService::class.java.simpleName
            private const val DBG = false
        }
    }
}