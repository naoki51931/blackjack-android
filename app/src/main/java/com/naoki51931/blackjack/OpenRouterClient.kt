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
            val system = """あなたはブラックジャック卓のAIディーラー。日本語で短く自然に会話する。現在の卓とカード状況を見て、ディーラー自身が心理戦の行動を決める。行動は必ず「勝負する」「勝負しない」「嘘をつく」「正直に話す」の4つから1つを自分で選ぶ。返答の先頭に【勝負する】のように選択を表示し、その後に1〜3文でプレイヤーへ話す。「嘘をつく」を選んだ場合は自分の手が強い・弱いなどについてブラフしてよい。「正直に話す」場合も伏せ札そのものを直接公開してはいけない。プレイヤーの発言にもブラフがあり得るので鵜呑みにしない。難易度が強い場合はカード状況、確率、発言の矛盾を慎重に分析して選択する。弱い場合は読み違い、判断ミス、ブラフに引っかかることがある。ゲームの実際のカードや勝敗は改ざんしない。"""
            val messages = JSONArray()
                .put(JSONObject().put("role", "system").put("content", system))
                .put(JSONObject().put("role", "user").put("content", "現在の卓: $situation\nプレイヤー: $playerMessage\nディーラー自身の4択も判断して返答して。"))
            val body = JSONObject().put("model", MODEL).put("messages", messages).put("temperature", 0.9).put("max_tokens", 200)
            val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"; connectTimeout = 15000; readTimeout = 30000; doOutput = true
                setRequestProperty("Authorization", "Bearer $apiKey"); setRequestProperty("Content-Type", "application/json"); setRequestProperty("X-Title", "Blackjack 21 Android")
            }
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode; val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty(); connection.disconnect()
            if (code !in 200..299) return@withContext "通信エラー($code): ${runCatching { JSONObject(text).optJSONObject("error")?.optString("message") }.getOrNull() ?: "OpenRouterに接続できません"}"
            JSONObject(text).getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content").ifBlank { "【勝負する】……次の一手をどうぞ。" }
        } catch (e: Exception) { "通信エラー: ${e.message ?: "接続できません"}" }
    }
}
