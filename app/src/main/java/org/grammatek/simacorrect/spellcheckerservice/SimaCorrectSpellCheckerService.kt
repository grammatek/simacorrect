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
            val suggestionList: Array<SuggestionsInfo>

            try {
                val textToCorrect = textInfos.joinToString(separator = " ") { it.text }
                val response = ConnectionManager.correctSentence(textToCorrect)
                val ylAnnotation = YfirlesturAnnotation(response)
                suggestionList = ylAnnotation.getSuggestionsForAnnotatedWords().toTypedArray()
            } catch (e: Exception) {
                Log.e(TAG, "onGetSuggestionsMultiple: Exception: $e")
                return emptyArray()
            }

            for(sl in suggestionList) {
                sl.setCookieAndSequence(textInfos[0].cookie, sl.sequence)
            }
            return suggestionList
        }

        override fun onGetSentenceSuggestionsMultiple(
            textInfos: Array<out TextInfo?>?,
            suggestionsLimit: Int,
        ): Array<SentenceSuggestionsInfo> {
            Log.d(TAG, "onGetSentenceSuggestionsMultiple: ${textInfos?.size}")
            if(textInfos == null || textInfos.isEmpty()) {
                return emptyArray()
            }

            // TODO: What do we need here exactly.
            //  SentenceSuggestionsInfo which contains
            //      SuggestionInfo[], Offsets[], Lengths[]

            val suggestionList: Array<SuggestionsInfo>
            val offsets: IntArray
            val lengths: IntArray

            val textInfosArray: Array<TextInfo> = textInfos as Array<TextInfo>
            try {
                val textToCorrect = textInfosArray.joinToString(separator = " ") { it.text }
                val response = ConnectionManager.correctSentence(textToCorrect)

                val ylAnnotation = YfirlesturAnnotation(response)
                suggestionList = ylAnnotation.getSuggestionsForAnnotatedWords().toTypedArray()
                offsets = ylAnnotation.offsets.toIntArray()
                lengths = ylAnnotation.lengths.toIntArray()
                for (i in suggestionList.indices) {
                    Log.d(TAG, "suggestion: ${suggestionList[i].getSuggestionAt(0)}, offset: ${offsets[i]}, length: ${lengths[i]}")
                }

                for(sl in suggestionList) {
                    sl.setCookieAndSequence(textInfosArray[0].cookie, sl.sequence)
                }

            } catch (e: Exception) {
                Log.d(TAG, "Exception: $e")
                return arrayOf()
            }
//            Log.d(TAG, "suggestion: ${suggestionList[0].getSuggestionAt(0)}, offset: ${offsets[0]}, length: ${lengths[0]}")

            val sad = arrayOf(SentenceSuggestionsInfo(suggestionList, offsets, lengths))
            val bla = super.onGetSentenceSuggestionsMultiple(textInfos, suggestionsLimit)
//            if(bla.isNotEmpty() && bla != null) {
//                Log.d(TAG, "suggestion: ${bla[0].getSuggestionsInfoAt(0).getSuggestionAt(0)}, offset: ${bla[0].getOffsetAt(0)}, length: ${bla[0].getLengthAt(0)}")
//            }
            return bla


//            return arrayOf(SentenceSuggestionsInfo(suggestionList, offsets, lengths))

//            val retval = arrayOf<SentenceSuggestionsInfo>()
////            val splitTextInfos = arrayOfNulls<TextInfo>(itemsSize)
//            val splitTextInfos = arrayOfNulls<TextInfo>(1)
//            for(i in textInfos.indices) {
//                splitTextInfos[i] = textInfos[i]
//                Log.d(TAG, "text: ${textInfos[i].text}")
//            }
//            val suggestions = onGetSuggestionsMultiple(splitTextInfos, 5, true)
//
//            retval[0] = SentenceSuggestionsInfo(suggestions, intArrayOf(), intArrayOf())
//            return retval
//            return super.onGetSentenceSuggestionsMultiple(textInfos, suggestionsLimit)
//            val suggestionsInfos: MutableList<SuggestionsInfo> = ArrayList()
//
//            for (element in textInfos) {
//                // Convert the sentence into an array of words
//                val words = element.text.split("\\s+").toTypedArray()
//                for (word in words) {
//                    val tmp = TextInfo(word)
//                    // Generate suggestions for each word
//                    suggestionsInfos.add(onGetSuggestions(tmp, suggestionsLimit))
//                }
//            }
//            return arrayOf(
//                SentenceSuggestionsInfo(
//                    suggestionsInfos.toTypedArray(),
//                    IntArray(suggestionsInfos.size),
//                    IntArray(suggestionsInfos.size)
//                )
//            )


//            return super.onGetSentenceSuggestionsMultiple(textInfos, suggestionsLimit)
        }

//        private fun getSplitWords(originalTextInfo: TextInfo): SentenceTextInfoParams {
////            val wordIterator: WordIterator = mWordIterator
//            val originalText: CharSequence = originalTextInfo.text
//            val cookie = originalTextInfo.cookie
//            val start = 0
//            val end = originalText.length
//            val wordItems2: ArrayList<SentenceWordItem> = ArrayList()
//
//            val bi = BreakIterator.getWordInstance()
//            bi.setText(originalTextInfo.text)
//
////            wordIterator.setCharSequence(originalText, 0, originalText.length)
////            var wordEnd: Int = wordIterator.following(start)
//            var wordEnd: Int = bi.following(start)
//            var wordStart =
//                if (wordEnd == BreakIterator.DONE) {
//                    BreakIterator.DONE
//                } else {
//                    // TODO: Get beginning of wordEnd
////                    bi.preceding(wordEnd) not working
//                    bi.previous()
//                }
////            var wordStart =
////                if (wordEnd == BreakIterator.DONE) {
////                    BreakIterator.DONE
////                } else {
////                    wordIterator.getBeginning(wordEnd)
////                }
//
////            Log.d(TAG, "word start = $wordStart, word end = $wordEnd original text = $originalText")
//
//            while (wordStart <= end && wordEnd != BreakIterator.DONE && wordStart != BreakIterator.DONE) {
//                if (wordEnd >= start && wordEnd > wordStart) {
//                    val query = originalText.subSequence(wordStart, wordEnd)
//                    Log.d(TAG, "query = $query, word start = $wordStart, word end = $wordEnd original text = $originalText")
//                    val ti = TextInfo(
//                        query, 0, query.length, cookie,
//                        query.hashCode()
//                    )
//                    wordItems2.add(
//                        SentenceWordItem(
//                            ti,
//                            wordStart,
//                            wordEnd
//                        )
//                    )
//                    Log.d(TAG,"Adapter: word (" + (wordItems2.size - 1) + ") " + query)
//                }
////                wordEnd = wordIterator.following(wordEnd)
//                wordEnd = bi.following(wordEnd)
//                if (wordEnd == BreakIterator.DONE) {
//                    break
//                }
////                wordStart = wordIterator.getBeginning(wordEnd)
//                wordStart = bi.previous()
//
//            }
//            return SentenceTextInfoParams(originalTextInfo, wordItems2)
//        }

//        fun reconstructSuggestions(
//            originalTextInfoParams: SentenceTextInfoParams?, results: Array<SuggestionsInfo?>?
//        ): SentenceSuggestionsInfo? {
//            if (results == null || results.isEmpty()) {
//                return null
//            }
//            if (DBG) {
//                Log.w(TAG, "Adapter: onGetSuggestions: got " + results.size)
//            }
//            if (originalTextInfoParams == null) {
//                if (DBG) {
//                    Log.w(TAG, "Adapter: originalTextInfoParams is null.")
//                }
//                return null
//            }
//            for (r in results) {
//                Log.d(TAG, "result: ${r?.getSuggestionAt(0)}")
//            }
//
//
//            val originalCookie = originalTextInfoParams.mOriginalTextInfo.cookie
//            val originalSequence = originalTextInfoParams.mOriginalTextInfo.sequence
//            val querySize = originalTextInfoParams.mSize
//            val offsets = IntArray(querySize)
//            val lengths = IntArray(querySize)
//            val reconstructedSuggestions = arrayOfNulls<SuggestionsInfo>(querySize)
//            for (i in 0 until querySize) {
//                val item = originalTextInfoParams.mItems[i]
//                var result: SuggestionsInfo? = null
//                for (j in results.indices) {
//                    val cur = results[j]
//                    Log.d(TAG, "before If statement $cur, ${cur?.sequence}, ${item.mTextInfo.sequence} ")
//                    if (cur != null && cur.sequence == item.mTextInfo.sequence) {
//                        result = cur
//                        result.setCookieAndSequence(originalCookie, originalSequence)
//                        break
//                    }
//                }
//                offsets[i] = item.mStart
//                lengths[i] = item.mLength
//                Log.d(TAG, "reconstructedSuggestion[$i] = $result")
//                reconstructedSuggestions[i] = result
//                    ?: SuggestionsInfo(0, null)
//                if (DBG) {
//                    val size = reconstructedSuggestions[i]!!.suggestionsCount
//                    Log.w(
//                        TAG,
//                        "reconstructedSuggestions(" + i + ")" + size + ", first = "
//                                + (if (size > 0) reconstructedSuggestions[i]!!.getSuggestionAt(0) else "<none>") + ", offset = " + offsets[i] + ", length = "
//                                + lengths[i]
//                    )
//                }
//            }
//            return SentenceSuggestionsInfo(reconstructedSuggestions, offsets, lengths)
//        }

        /**
         * Container for originally queried TextInfo and parameters
         */
        class SentenceTextInfoParams(
            val mOriginalTextInfo: TextInfo,
            items: ArrayList<SentenceWordItem>
        ) {
            val mItems: java.util.ArrayList<SentenceWordItem> = items
            val mSize: Int = items.size
        }

        class SentenceWordItem(
            val mTextInfo: TextInfo,
            val mStart: Int,
            val end: Int
        ) {
            val mLength: Int = end - mStart
        }

        override fun onGetSuggestions(textInfo: TextInfo?, suggestionsLimit: Int): SuggestionsInfo {
            TODO("Not yet implemented")
        }

        companion object {
            private val TAG = SimaCorrectSpellCheckerService::class.java.simpleName
        }
    }
}