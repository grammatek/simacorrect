package org.grammatek.simacorrect.spellcheckerservice

import android.service.textservice.SpellCheckerService
import android.util.Log
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import android.view.textservice.SentenceSuggestionsInfo
import org.grammatek.apis.DevelopersApi

class SimaCorrectSpellCheckerService : SpellCheckerService() {
    override fun createSession(): Session {
        return AndroidSpellCheckerSession()
    }

    private class AndroidSpellCheckerSession: Session() {
        private lateinit var mLocale: String
        override fun onCreate() {
            mLocale = locale
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
                retval[i]!!.setCookieAndSequence(
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

            // The API we're calling returns a value with the first letter in uppercase.
            // We need to compare against that so we also cast our first letter of the word to uppercase.
            val text: String = textInfo.text.replaceFirstChar {
                it.uppercase()
            }
            val api = DevelopersApi()
            val response = api.correctApiPost(text)
            val correctedText = response.result?.get(0)?.get(0)?.corrected
            var flags = 0
            val suggestions = mutableListOf<String>()

            // Check original word against corrected word. Note that correctedText
            // always begins with a uppercase letter (Yfirlestur specifications).
            if (text != correctedText) {
                flags = SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO
                val retval = correctedText.toString().replaceFirstChar {
                    it.lowercase()
                }
                suggestions.add(retval)
                Log.d(TAG, retval)
            }
            return SuggestionsInfo(flags, suggestions.toTypedArray())
        }

        companion object {
            private val TAG = SimaCorrectSpellCheckerService::class.java.simpleName
            private const val DBG = true
        }
    }
}