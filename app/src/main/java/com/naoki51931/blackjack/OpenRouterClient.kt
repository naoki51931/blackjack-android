package com.naoki51931.blackjack

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object OpenRouterClient {
    private const val ENDPOINT = "https://openrouter.ai/api/v1/chat/completions"
    private const val MODEL = "google/gemini-3-flash-preview"

    suspend fun dealerReply(apiKey: String, situation: String, playerMessage: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext "OpenRouter APIキーをタイトル画面で設定してくれ。"
        try {
            val system = """あなたはブラックジャック卓のAIディーラー。日本語で短く自然に会話する。現在の公開情報だけを知り、伏せ札の内容はプレイヤーに絶対に漏らさない。プレイヤーは会話でブラフ（嘘）をつくことがある。発言を鵜呑みにせず、カード状況と発言から心理戦をする。返答は1〜3文、カジノのディーラーらしい口調。ゲームの勝敗そのものを改ざんしない。"""
            val messages = JSONArray()
                .put(JSONObject().put("role", "system").put("content", system))
                .put(JSONObject().put("role", "user").put("content", "現在の卓: $situation\nプレイヤー: $playerMessage"))
            val body = JSONObject()
                .put("model", MODEL)
                .put("messages", messages)
                .put("temperature", 0.9)
                .put("max_tokens", 180)

            val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 30000
                doOutput = true
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("X-Title", "Blackjack 21 Android")
            }
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            connection.disconnect()
            if (code !in 200..299) return@withContext "通信エラー($code): ${runCatching { JSONObject(text).optJSONObject("error")?.optString("message") }.getOrNull() ?: "OpenRouterに接続できません"}"
            JSONObject(text).getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content").ifBlank { "……次の一手をどうぞ。" }
        } catch (e: Exception) {
            "通信エラー: ${e.message ?: "接続できません"}"
        }
    }
}
