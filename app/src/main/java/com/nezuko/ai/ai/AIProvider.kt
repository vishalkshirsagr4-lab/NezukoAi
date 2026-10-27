package com.nezuko.ai.ai

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

enum class ProviderKind { GEMINI, OPENROUTER }

data class ProviderConfig(
    val kind: ProviderKind,
    val apiKey: String,
    val model: String
)

class ProviderException(val userMessage: String, val statusCode: Int? = null) : Exception(userMessage)

interface AIProvider {
    suspend fun generate(config: ProviderConfig, prompt: String, context: List<Pair<String, String>>): String
    suspend fun test(config: ProviderConfig): Boolean
}

class HttpAIProvider : AIProvider {
    override suspend fun generate(config: ProviderConfig, prompt: String, context: List<Pair<String, String>>): String {
        require(config.apiKey.isNotBlank()) { "Add an API key in AI settings first." }
        require(config.model.isNotBlank()) { "Enter a model identifier." }
        return when (config.kind) {
            ProviderKind.GEMINI -> gemini(config, prompt, context)
            ProviderKind.OPENROUTER -> openRouter(config, prompt, context)
        }
    }

    override suspend fun test(config: ProviderConfig): Boolean {
        val result = generate(config, "Reply with the single word OK.", emptyList())
        return result.isNotBlank()
    }

    private fun request(url: String, key: String, body: JSONObject, openRouter: Boolean): String {
        val conn = (URL(url).openConnection() as HttpURLConnection)
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            if (openRouter) {
                conn.setRequestProperty("Authorization", "Bearer $key")
                conn.setRequestProperty("HTTP-Referer", "https://localhost/nezuko-ai")
                conn.setRequestProperty("X-Title", "Nezuko AI")
            } else conn.setRequestProperty("x-goog-api-key", key)
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val raw = stream?.bufferedReader()?.use { it.readText() }.orEmpty().take(1_000_000)
            if (code !in 200..299) {
                val msg = when (code) {
                    400, 404 -> "The model or request is invalid. Check the model identifier."
                    401, 403 -> "Authentication failed. Check the API key and provider access."
                    408, 504 -> "The provider timed out. Please try again."
                    429 -> "Provider rate limit or quota reached. Wait or check your provider plan."
                    in 500..599 -> "The AI provider is temporarily unavailable ($code)."
                    else -> "AI request failed (HTTP $code)."
                }
                throw ProviderException(msg, code)
            }
            return raw
        } catch (e: ProviderException) {
            throw e
        } catch (e: java.net.SocketTimeoutException) {
            throw ProviderException("The request timed out. Check your connection and retry.")
        } catch (e: java.net.UnknownHostException) {
            throw ProviderException("Cannot reach the provider. Check your internet connection.")
        } catch (e: Exception) {
            throw ProviderException("Network request failed. Please retry.")
        } finally {
            conn.disconnect()
        }
    }

    private fun gemini(c: ProviderConfig, prompt: String, context: List<Pair<String, String>>): String {
        val contents = org.json.JSONArray()
        (context + listOf("user" to prompt)).takeLast(20).forEach { (role, text) ->
            contents.put(JSONObject().put("role", if (role == "assistant") "model" else "user")
                .put("parts", org.json.JSONArray().put(JSONObject().put("text", text))))
        }
        val body = JSONObject().put("contents", contents)
            .put("generationConfig", JSONObject().put("temperature", 0.7).put("maxOutputTokens", 1024))
        val raw = request("https://generativelanguage.googleapis.com/v1beta/models/${c.model}:generateContent", c.apiKey, body, false)
        val parts = JSONObject(raw).optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
        val answer = (0 until (parts?.length() ?: 0)).mapNotNull { parts?.optJSONObject(it)?.optString("text") }.joinToString("\n").trim()
        if (answer.isBlank()) throw ProviderException("The provider returned no text. Try another model.")
        return answer
    }

    private fun openRouter(c: ProviderConfig, prompt: String, context: List<Pair<String, String>>): String {
        val messages = org.json.JSONArray()
        messages.put(JSONObject().put("role", "system").put("content",
            "You are Nezuko AI, a helpful Android assistant. Never claim a phone action succeeded unless a tool result confirms it. Do not claim you executed device actions; this chat provider has no direct device access."))
        (context + listOf("user" to prompt)).takeLast(20).forEach { (role, text) ->
            messages.put(JSONObject().put("role", if (role == "assistant") "assistant" else "user").put("content", text))
        }
        val body = JSONObject().put("model", c.model).put("messages", messages).put("temperature", 0.7).put("max_tokens", 1024)
        val raw = request("https://openrouter.ai/api/v1/chat/completions", c.apiKey, body, true)
        val answer = JSONObject(raw).optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content").orEmpty().trim()
        if (answer.isBlank()) throw ProviderException("The provider returned no text. Try another model.")
        return answer
    }
}
