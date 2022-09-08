package org.grammatek.rettritun.spellcheckerservice

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.grammatek.apis.CorrectApi
import org.grammatek.models.CorrectRequest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

typealias AnnotationIndices = YfirlesturAnnotation.AnnotationIndices

@Serializable
data class TestCases (
    val input: String,
    val suggestions: List<String>,
)

@RunWith(RobolectricTestRunner::class)
class YfirlesturAnnotationTest {
    private val api = CorrectApi("http://localhost:5002")
    private val file = File("src/test/res/YfirlesturAnnotationTestCases.json").readText()
    private val _testCases = Json.decodeFromString<List<TestCases>>(file)
    private val _yfirlesturRulesToIgnore = listOf("Z002")

    @Test
    fun `do expected and actual suggestions match`() {
        val suggestionLimit = 5
        val dictionary = arrayListOf<String>()

        for (testCase in _testCases) {
            val request = CorrectRequest(testCase.input, ignoreRules = _yfirlesturRulesToIgnore, ignoreWordlist = dictionary)
            val response = api.correctApiPost(request)
            val suggestionsInfo = YfirlesturAnnotation(response, testCase.input).getSuggestionsForAnnotatedWords(suggestionLimit)
            val actualSuggestions: MutableList<String> = arrayListOf()
            for (suggestionInfo in suggestionsInfo) {
                for (j in 0 until suggestionInfo.suggestionsCount) {
                    val suggestion = suggestionInfo.getSuggestionAt(j)
                    actualSuggestions.add(suggestion)
                }
            }
            assertThat(actualSuggestions).containsExactlyElementsIn(testCase.suggestions)
        }
    }

    @Test
    fun `do expected and actual token start and end indices match`() {
        val suggestionLimit = 5
        val dictionary = arrayListOf<String>()

        for (testCase in _testCases) {
            val request = CorrectRequest(testCase.input, ignoreRules = _yfirlesturRulesToIgnore, ignoreWordlist = dictionary)
            val response = api.correctApiPost(request)
            val ylAnnotation = YfirlesturAnnotation(response, testCase.input)
            ylAnnotation.getSuggestionsForAnnotatedWords(suggestionLimit)

            val tokenIndices = ylAnnotation.tokensIndices.flatten() // merge sentences for simplicity.
            val suggestionsIndices = ylAnnotation.suggestionsIndices

            for (t in suggestionsIndices) {
                val startChar = tokenIndices.find { it.startChar == t.startChar }?.startChar ?: -1
                val endChar = tokenIndices.find { it.endChar == t.endChar }?.endChar ?: -1
                assertThat(t.startChar).isEqualTo(startChar)
                assertThat(t.endChar).isEqualTo(endChar)
            }
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
            val request = CorrectRequest(texts[i], ignoreRules = _yfirlesturRulesToIgnore, ignoreWordlist = dictionary)
            val response = api.correctApiPost(request)
            val ylAnnotations = YfirlesturAnnotation(response, texts[i])
            ylAnnotations.getSuggestionsForAnnotatedWords(suggestionLimit)
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

        for (testCase in _testCases) {
            val request = CorrectRequest(testCase.input, ignoreRules = _yfirlesturRulesToIgnore, ignoreWordlist = dictionary)
            val response = api.correctApiPost(request)
            val ylAnnotation = YfirlesturAnnotation(response, testCase.input)
            ylAnnotation.getSuggestionsForAnnotatedWords(suggestionLimit)

            val ylAnnotationTokenCount = ylAnnotation.tokensIndices.flatten().count()
            assertThat(ylAnnotationTokenCount).isEqualTo(response.stats?.numTokens)
        }
    }

    @Test
    fun `is suggestion limit enforced`() {
        val dictionary = arrayListOf<String>()
        for (testCase in _testCases) {
            var suggestionLimit = (0..2).random()
            val request = CorrectRequest(testCase.input, ignoreRules = _yfirlesturRulesToIgnore, ignoreWordlist = dictionary)
            val response = api.correctApiPost(request)
            val ylAnnotation = YfirlesturAnnotation(response, testCase.input)
            var suggestionInfo = ylAnnotation.getSuggestionsForAnnotatedWords(suggestionLimit)
            for (suggestions in suggestionInfo) {
                assertThat(suggestions.suggestionsCount).isAtMost(suggestionLimit)
            }
        }
    }

    @Test
    fun `multiple whitespaces don't throw off indices for annotations`() {
        val suggestionLimit = 5
        val text = "   Afhverju  er   það  eki  hægt"
        val startIndices = listOf(3, 23)
        val endIndices = listOf(10, 25)
        val dictionary = arrayListOf<String>()
        val request = CorrectRequest(text, ignoreRules = _yfirlesturRulesToIgnore, ignoreWordlist = dictionary)
        val response = api.correctApiPost(request)
        val ylAnnotation = YfirlesturAnnotation(response, text)
        ylAnnotation.getSuggestionsForAnnotatedWords(suggestionLimit)

        for (i in ylAnnotation.suggestionsIndices.indices) {
            assertThat(ylAnnotation.suggestionsIndices[i].startChar).isEqualTo(startIndices[i])
            assertThat(ylAnnotation.suggestionsIndices[i].endChar).isEqualTo(endIndices[i])
        }
    }

    @Test
    fun `special characters don't throw off annotation indices`() {
        // Yfirlestur's python binary is compiled with different support than of Android.
        // As a consequence, Yfirlestur and Android count codepoints differently i.e. some
        // special characters have different lengths in Yfirlestur, from Android.
        val suggestionLimit = 0
        val text = "\uD83E\uDD76\u200B er kallt og \uD83E\uDD75 er heittt"
        val dictionary = arrayListOf<String>()
        val request = CorrectRequest(text, ignoreRules = _yfirlesturRulesToIgnore, ignoreWordlist = dictionary)
        val response = api.correctApiPost(request)
        val textLengthByYfirlestur = response.stats!!.numChars
        val textLengthByAndroid = response.text!!.length
        val expectedAnnotationIndices = listOf(
            AnnotationIndices(7, 11),
            AnnotationIndices(22, 27),
        )
        val ylAnnotation = YfirlesturAnnotation(response, text)
        ylAnnotation.getSuggestionsForAnnotatedWords(suggestionLimit)
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
}