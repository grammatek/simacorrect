package org.grammatek.simacorrect.spellcheckerservice

import com.google.common.truth.Truth.assertThat
import org.grammatek.apis.UsersApi
import org.grammatek.models.CorrectRequest
import org.grammatek.models.CorrectResponse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

typealias AnnotationIndices = YfirlesturAnnotation.AnnotationIndices

@RunWith(RobolectricTestRunner::class)
class YfirlesturAnnotationTest {
    private val api = UsersApi("http://localhost:5002")
    private val testCases: List<String> = listOf(
        "Einn af drengjunum fóru í sund af gefnu tilefni.",
        "Mig hlakkaði til.",
        "eg dreimi um að leita af mindinni",
        "Eg dreymdi kött.",
        "Páli, sem hefur verið landsliðsmaður í fótbolta í sjö ár, langaði að horfa á sjónvarpið.",
        "Páli, vini mínum, langaði að horfa á sjónvarpið.",
        "Hestinum Skjóna vantaði hamar.",
        "Önnu kveið # fyrir skóladeginum.",
        "Er afhverju eitt eða tvö orð?"
    )

    @Test
    fun `we only get expected suggestions, nothing more, nothing less`() {
        val suggestionLimit = 5
        val dictionary = arrayListOf<String>()
        val testCasesExpectedSuggestions: List<List<String>> = listOf(
            listOf("fór", "að gefnu tilefni"),
            listOf("Ég"),
            listOf("ég", "mig", "dreymi", "að", "myndinni"),
            listOf("Ég", "Mig"),
            listOf("Pál, sem hefur verið landsliðsmaður í fótbolta í sjö ár"),
            listOf("Pál, vin minn"),
            listOf("Hestinn skjóna"),
            listOf("Anna"),
            listOf("af hverju"),
        )
        for (i in testCases.indices) {
            val response = api.correctApiPost(CorrectRequest(testCases[i]))
            val suggestionsInfo = YfirlesturAnnotation(response, testCases[i]).getSuggestionsForAnnotatedWords(suggestionLimit, dictionary)
            val actualSuggestions: MutableList<String> = arrayListOf()
            for (suggestionInfo in suggestionsInfo) {
                for (j in 0 until suggestionInfo.suggestionsCount) {
                    val suggestion = suggestionInfo.getSuggestionAt(j)
                    actualSuggestions.add(suggestion)
                }
            }
            assertThat(actualSuggestions).containsExactlyElementsIn(testCasesExpectedSuggestions[i])
        }
    }

    @Test
    fun `Yfirlestur updates suggestions as more context is revealed`() {
        val suggestionLimit = 5
        val dictionary = arrayListOf<String>()
        val texts = listOf(
            "Zwart",
            "Zwart er",
            "Zwart er vedrid",
            "Zwart er vedrid i",
            "Zwart er vedrid i i",
            "Zwart er vedrid i i dag"
        )
        val expectedAnnotations = listOf(
            listOf(),
            listOf(),
            listOf(AnnotationIndices(0, 14)),
            listOf(AnnotationIndices(9, 14), AnnotationIndices(16, 16)),
            listOf(AnnotationIndices(9, 14), AnnotationIndices(16, 18)),
            listOf(AnnotationIndices(9, 14), AnnotationIndices(16, 18)),
        )

        for (i in texts.indices) {
            val response: CorrectResponse = api.correctApiPost(CorrectRequest(texts[i]))
            val ylAnnotations = YfirlesturAnnotation(response, texts[i])
            ylAnnotations.getSuggestionsForAnnotatedWords(suggestionLimit, dictionary)
            val annotations = ylAnnotations.suggestionsIndices

            assertThat(annotations.size).isEqualTo(expectedAnnotations[i].size)
            for (j in annotations.indices) {
                assertThat(annotations[j].startChar).isEqualTo(expectedAnnotations[i][j].startChar)
                assertThat(annotations[j].endChar).isEqualTo(expectedAnnotations[i][j].endChar)
            }
        }
    }

    @Test
    fun `does Yfirlestur token count match our constructed token count`() {
        val suggestionLimit = 5
        val dictionary = arrayListOf<String>()
        for (testCase in testCases) {
            val response = api.correctApiPost(CorrectRequest(testCase))
            val ylAnnotation = YfirlesturAnnotation(response, testCase)
            ylAnnotation.getSuggestionsForAnnotatedWords(suggestionLimit, dictionary)
            var size = 0
            for (tokensIndex in ylAnnotation.tokensIndices) {
                size += tokensIndex.size
            }
            assertThat(size).isEqualTo(response.stats?.numTokens ?: 0)
        }
    }

    @Test
    fun `is dictionary taken into account for suggestions`() {
        val suggestionLimit = 5
        val text = "Eg dreymi"
        val response = api.correctApiPost(CorrectRequest(text))
        val dictionary = arrayListOf<String>()
        var suggestionsInfo = YfirlesturAnnotation(response, text).getSuggestionsForAnnotatedWords(suggestionLimit, dictionary)
        assertThat(suggestionsInfo).isNotEmpty()

        dictionary.add("Eg")
        suggestionsInfo = YfirlesturAnnotation(response, text).getSuggestionsForAnnotatedWords(suggestionLimit, dictionary)
        assertThat(suggestionsInfo).isEmpty()

        dictionary.clear()
        suggestionsInfo = YfirlesturAnnotation(response, text).getSuggestionsForAnnotatedWords(suggestionLimit, dictionary)
        assertThat(suggestionsInfo).isNotEmpty()
    }

    @Test
    fun `is suggestion limit enforced`() {
        var suggestionLimit = 0
        val text = "eg dreimi um að leita af mindinni"
        val response = api.correctApiPost(CorrectRequest(text))
        val dictionary = arrayListOf<String>()

        val ylAnnotation = YfirlesturAnnotation(response, text)
        var suggestionInfo = ylAnnotation.getSuggestionsForAnnotatedWords(suggestionLimit, dictionary)
        for (suggestions in suggestionInfo) {
            assertThat(suggestions.suggestionsCount).isAtMost(suggestionLimit)
        }

        suggestionLimit = 1
        suggestionInfo = ylAnnotation.getSuggestionsForAnnotatedWords(suggestionLimit, dictionary)
        for (suggestions in suggestionInfo) {
            assertThat(suggestions.suggestionsCount).isAtMost(suggestionLimit)
        }
    }

    @Test
    fun `multiple whitespaces don't throw off indices for annotations`() {
        val suggestionLimit = 5
        val text = "   Afhverju  er   það  eki  hægt"
        val startIndices = listOf(3, 23)
        val endIndices = listOf(10, 25)
        val dictionary = arrayListOf<String>()
        val response = api.correctApiPost(CorrectRequest(text))
        val ylAnnotation = YfirlesturAnnotation(response, text)
        ylAnnotation.getSuggestionsForAnnotatedWords(suggestionLimit, dictionary)
        for (i in ylAnnotation.suggestionsIndices.indices) {
            assertThat(ylAnnotation.suggestionsIndices[i].startChar).isEqualTo(startIndices[i])
            assertThat(ylAnnotation.suggestionsIndices[i].endChar).isEqualTo(endIndices[i])
        }
    }

    @Test
    fun `capitalization for the first word of a sentence to be ignored by Yfirlestur`() {
        val suggestionLimit = 5
        val capitalizedText = "Mig langar ekki út."
        val nonCapitalizedText = "mig langar ekki út."
        val dictionary = arrayListOf<String>()
        val response = api.correctApiPost(CorrectRequest(capitalizedText))
        // The spell checker service takes care of capitalizing the first letter
        // so we have to test it differently e.g. see if the same sentence has
        // different suggestions if the first word is capitalized or not.
        val capitalizedSuggestionsInfo = YfirlesturAnnotation(response, capitalizedText).getSuggestionsForAnnotatedWords(suggestionLimit, dictionary)
        val nonCapitalizedSuggestionsInfo = YfirlesturAnnotation(response, nonCapitalizedText).getSuggestionsForAnnotatedWords(suggestionLimit, dictionary)

        assertThat(capitalizedSuggestionsInfo).isEqualTo(nonCapitalizedSuggestionsInfo)
    }

    @Test
    fun `special characters don't throw off annotation indices`() {
        // Yfirlestur's python binary is compiled with different support than of Android.
        // As a consequence, Yfirlestur and Android count codepoints differently i.e. some
        // special characters have different lengths in Yfirlestur, from Android.
        val suggestionLimit = 0
        val text = "\uD83E\uDD76\u200B er kallt og \uD83E\uDD75 er heittt"
        val response = api.correctApiPost(CorrectRequest(text))
        val dictionary = arrayListOf<String>()
        val textLengthByYfirlestur = response.stats!!.numChars
        val textLengthByAndroid = response.text!!.length
        val expectedAnnotationIndices = listOf(
            AnnotationIndices(7, 11),
            AnnotationIndices(22, 27),
        )
        val ylAnnotation = YfirlesturAnnotation(response, text)
        ylAnnotation.getSuggestionsForAnnotatedWords(suggestionLimit, dictionary)
        val annotations = ylAnnotation.suggestionsIndices

        assertThat(textLengthByYfirlestur).isNotEqualTo(textLengthByAndroid)
        for (i in annotations.indices) {
            val startCharFromResponse = response.result?.get(0)?.get(0)?.annotations?.get(i)?.startChar ?: -1
            val endCharFromResponse = response.result?.get(0)?.get(0)?.annotations?.get(i)?.endChar ?: -1
            assertThat(annotations[i].startChar).isNotEqualTo(startCharFromResponse)
            assertThat(annotations[i].endChar).isNotEqualTo(endCharFromResponse)

            assertThat(annotations[i].startChar).isEqualTo(expectedAnnotationIndices[i].startChar)
            assertThat(annotations[i].endChar).isEqualTo(expectedAnnotationIndices[i].endChar)
        }
    }

    @Test
    fun `capitalization ignored for new sentences`() {
        val suggestionLimit = 5
        val text = "Það er víst. það er víst"
        val dictionary = arrayListOf<String>()
        val response = api.correctApiPost(CorrectRequest(text))
        val ylAnnotation = YfirlesturAnnotation(response, text)
        val suggestion = ylAnnotation.getSuggestionsForAnnotatedWords(suggestionLimit, dictionary)

        assertThat(suggestion.count()).isEqualTo(0)
    }
}