package com.englishbondhu.app.ai

import com.englishbondhu.app.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class TutorMessage(val text: String, val fromLearner: Boolean)

interface AiTutor {
    suspend fun reply(message: String, sourceLanguage: String, level: String): String
}

/** Keyword-based offline fallback used when no AI backend or Gemini key is reachable. */
class GuidedTutor : AiTutor {
    override suspend fun reply(message: String, sourceLanguage: String, level: String): String {
        val text = message.trim().lowercase()
        return when {
            text.contains("introduce") || text.contains("পরিচয়") ->
                "বলুন: “Hello, I am [আপনার নাম]. Nice to meet you.” এবার নিজের নাম দিয়ে চেষ্টা করুন।"
            text.contains("difference") || text.contains("পার্থক্য") ->
                "‘I am’ নিজের অবস্থা বা পরিচয়ের জন্য। ‘I have’ কোনো কিছু আপনার আছে বোঝাতে। উদাহরণ: I am happy. I have a book."
            text.contains("practice") || text.contains("অনুশীলন") ->
                "চলুন অনুশীলন করি: ‘আমার এক গ্লাস পানি দরকার’—ইংরেজিতে লিখুন।"
            else -> "আমি সহজ বাংলা ব্যাখ্যা দেব। আজ আপনি পরিচয়, দৈনন্দিন কথা, নাকি উচ্চারণ অনুশীলন করতে চান?"
        }
    }
}

class NetworkAiTutor(
    private val baseUrl: String = BuildConfig.AI_BASE_URL,
    private val fallback: AiTutor = GuidedTutor(),
) : AiTutor {
    override suspend fun reply(message: String, sourceLanguage: String, level: String): String =
        withContext(Dispatchers.IO) {
            if (BuildConfig.GEMINI_API_KEY.isNotBlank()) {
                return@withContext runCatching { directGeminiReply(message, level) }
                    .getOrElse {
                        "Gemini-এর সঙ্গে যোগাযোগ করা যায়নি। Offline practice চালু আছে।\n\n" +
                            fallback.reply(message, sourceLanguage, level)
                    }
            }
            runCatching {
                val connection = (URL("${baseUrl.trimEnd('/')}/v1/tutor").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10_000
                    readTimeout = 45_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
                try {
                    val payload = JSONObject()
                        .put("message", message)
                        .put("sourceLanguage", sourceLanguage)
                        .put("targetLanguage", "en")
                        .put("level", level)
                        .toString()
                    connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                    val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
                    val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                    if (connection.responseCode !in 200..299) error("Tutor server returned ${connection.responseCode}")
                    JSONObject(body).getString("reply")
                } finally {
                    connection.disconnect()
                }
            }.getOrElse {
                "AI সার্ভারে পৌঁছানো যায়নি। Offline practice চালু আছে।\n\n" +
                    fallback.reply(message, sourceLanguage, level)
            }
        }

    private fun directGeminiReply(message: String, level: String): String {
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" +
            "${BuildConfig.GEMINI_TEXT_MODEL}:generateContent"
        val schema = JSONObject()
            .put("type", "object")
            .put("properties", JSONObject()
                .put("correctedEnglish", JSONObject().put("type", "string"))
                .put("explanationBn", JSONObject().put("type", "string"))
                .put("encouragementBn", JSONObject().put("type", "string"))
                .put("nextPromptEnglish", JSONObject().put("type", "string")))
            .put("required", org.json.JSONArray(listOf(
                "correctedEnglish", "explanationBn", "encouragementBn", "nextPromptEnglish",
            )))
        val system = """
            You are English Bondhu, a warm and precise English tutor for Bengali speakers at CEFR $level.
            Understand Bengali and English. Correct every English grammar or natural-wording mistake.
            Explain the important point in very simple Bengali. Never claim to score pronunciation from text.
            End with one short, realistic English question. Keep the response concise.
        """.trimIndent()
        val payload = JSONObject()
            .put("systemInstruction", JSONObject().put("parts", org.json.JSONArray()
                .put(JSONObject().put("text", system))))
            .put("contents", org.json.JSONArray().put(JSONObject()
                .put("role", "user")
                .put("parts", org.json.JSONArray().put(JSONObject().put("text", message)))))
            .put("generationConfig", JSONObject()
                .put("temperature", 0.25)
                .put("maxOutputTokens", 900)
                .put("responseMimeType", "application/json")
                .put("responseJsonSchema", schema))

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 45_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("x-goog-api-key", BuildConfig.GEMINI_API_KEY)
        }
        try {
            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (connection.responseCode !in 200..299) error("Gemini returned ${connection.responseCode}")
            val reply = TutorReplyFormatter.format(JSONObject(TutorReplyFormatter.candidateText(body)))
            check(reply.isNotBlank()) { "Gemini returned an empty tutor reply" }
            return reply
        } finally {
            connection.disconnect()
        }
    }
}

/** Pure parsing/formatting helpers, kept separate so they can be unit-tested without a network. */
object TutorReplyFormatter {
    /**
     * Joins every non-thought text part of the first candidate. Structured JSON output can be
     * split across several parts, so reading only the first part is not safe.
     */
    fun candidateText(responseBody: String): String {
        val parts = JSONObject(responseBody).getJSONArray("candidates").getJSONObject(0)
            .getJSONObject("content").getJSONArray("parts")
        return (0 until parts.length())
            .mapNotNull { parts.optJSONObject(it) }
            .filterNot { it.optBoolean("thought", false) }
            .joinToString("") { it.optString("text") }
    }

    /** Mirrors `format_tutor_reply` in backend/server.py so both paths look the same in the chat. */
    fun format(result: JSONObject): String = buildList {
        result.optString("correctedEnglish").trim().takeIf { it.isNotBlank() }?.let { add("Correct English: $it") }
        result.optString("explanationBn").trim().takeIf { it.isNotBlank() }?.let(::add)
        result.optString("encouragementBn").trim().takeIf { it.isNotBlank() }?.let(::add)
        result.optString("nextPromptEnglish").trim().takeIf { it.isNotBlank() }?.let { add("Your turn: $it") }
    }.joinToString("\n\n")
}
