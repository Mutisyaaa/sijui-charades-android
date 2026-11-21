package com.sijui.charades.data

import android.content.Context
import android.content.SharedPreferences
import com.sijui.charades.data.model.ApiErrorResponse
import com.sijui.charades.data.model.AppConfig
import com.sijui.charades.data.model.AuthResponse
import com.sijui.charades.data.model.AuthUser
import com.sijui.charades.data.model.BulkCardsResponse
import com.sijui.charades.data.model.CharadesDeck
import com.sijui.charades.data.model.CreateOrJoinRoomResponse
import com.sijui.charades.data.model.NaniCard
import com.sijui.charades.data.model.NaniCategoryCount
import com.sijui.charades.data.model.NaniDeck
import com.sijui.charades.data.model.NaniRoom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val latestVersion: String,
    val downloadUrl: String,
)

class SijuiApiClient(context: Context) {

    companion object {
        const val DEFAULT_BASE_URL = "https://sijui-game.onrender.com"
        const val GITHUB_LATEST_RELEASE_API =
            "https://api.github.com/repos/Mutisyaaa/sijui-charades-android/releases/latest"
        private const val PREFS_NAME = "sijui_prefs"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_SESSION_COOKIE = "session_cookie"
        private const val KEY_BEST_SCORE = "best_score"
        private const val KEY_TILT_ENABLED = "tilt_enabled"
        private const val KEY_APP_CONFIG = "cached_app_config"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val appContext: Context = context.applicationContext

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val session = cookies.firstOrNull { it.name == "sijui_session" } ?: return
            if (session.value.isBlank() || session.expiresAt <= System.currentTimeMillis()) {
                prefs.edit().remove(KEY_SESSION_COOKIE).apply()
            } else {
                prefs.edit().putString(KEY_SESSION_COOKIE, session.value).apply()
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val token = prefs.getString(KEY_SESSION_COOKIE, null) ?: return emptyList()
            return listOf(
                Cookie.Builder()
                    .name("sijui_session")
                    .value(token)
                    .domain(url.host)
                    .path("/")
                    .build(),
            )
        }
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL)?.trimEnd('/') ?: DEFAULT_BASE_URL
        set(value) {
            val cleaned = value.trim().trimEnd('/').ifBlank { DEFAULT_BASE_URL }
            prefs.edit().putString(KEY_BASE_URL, cleaned).apply()
        }

    var bestScore: Int
        get() = prefs.getInt(KEY_BEST_SCORE, 0)
        private set(value) {
            prefs.edit().putInt(KEY_BEST_SCORE, value).apply()
        }

    fun recordScoreIfBest(score: Int): Boolean {
        if (score > bestScore) {
            bestScore = score
            return true
        }
        return false
    }

    var tiltEnabled: Boolean
        get() = prefs.getBoolean(KEY_TILT_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_TILT_ENABLED, value).apply()
        }

    suspend fun checkForAppUpdate(currentVersionName: String): AppUpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url(GITHUB_LATEST_RELEASE_API)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "SijuiCharadesAndroid/$currentVersionName")
                .get()
                .build()
            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return@runCatching null
                val obj = json.parseToJsonElement(res.body?.string().orEmpty()).jsonObject
                val tag = obj["tag_name"]?.jsonPrimitive?.content?.trim().orEmpty()
                if (tag.isBlank() || !isVersionNewer(tag, currentVersionName)) return@runCatching null
                val assets = obj["assets"]?.jsonArray.orEmpty()
                val apkUrl = assets.firstOrNull { el ->
                    val name = el.jsonObject["name"]?.jsonPrimitive?.content.orEmpty()
                    name.startsWith("sijui-charades") && name.endsWith(".apk")
                }?.jsonObject?.get("browser_download_url")?.jsonPrimitive?.content
                    ?: obj["html_url"]?.jsonPrimitive?.content
                    ?: return@runCatching null
                AppUpdateInfo(latestVersion = tag, downloadUrl = apkUrl)
            }
        }.getOrNull()
    }

    private fun isVersionNewer(remoteTag: String, localVersion: String): Boolean {
        val r = remoteTag.trimStart('v', 'V').split('.').map { it.toIntOrNull() ?: 0 }
        val l = localVersion.trimStart('v', 'V').split('.').map { it.toIntOrNull() ?: 0 }
        val len = maxOf(r.size, l.size)
        for (i in 0 until len) {
            val rv = r.getOrElse(i) { 0 }
            val lv = l.getOrElse(i) { 0 }
            if (rv != lv) return rv > lv
        }
        return false
    }

    fun resolveImageUrl(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        if (raw.startsWith("data:") || raw.startsWith("http://") || raw.startsWith("https://")) {
            return raw
        }
        return "$baseUrl/${raw.trimStart('/')}"
    }

    fun proxyImageUrl(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        if (raw.startsWith("data:")) return raw
        if (raw.startsWith("/")) return "$baseUrl$raw"
        return "$baseUrl/api/nani/proxy?url=${URLEncoder.encode(raw, "UTF-8")}"
    }

    private suspend fun execute(request: Request): String = withContext(Dispatchers.IO) {
        client.newCall(request).execute().use { response ->
            val bodyStr = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val apiError = runCatching {
                    json.decodeFromString<ApiErrorResponse>(bodyStr).error
                }.getOrNull()
                throw IOException(apiError ?: "Server error (${response.code})")
            }
            bodyStr
        }
    }

    private fun get(path: String): Request =
        Request.Builder().url("$baseUrl$path").get().build()

    private fun post(path: String, payload: JsonObject = buildJsonObject {}): Request =
        Request.Builder()
            .url("$baseUrl$path")
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

    private fun put(path: String, payload: String): Request =
        Request.Builder()
            .url("$baseUrl$path")
            .put(payload.toRequestBody(JSON_MEDIA_TYPE))
            .build()

    private fun delete(path: String): Request =
        Request.Builder().url("$baseUrl$path").delete().build()

    // --- Live App Config ---
    fun loadCachedAppConfig(): AppConfig = runCatching {
        val raw = prefs.getString(KEY_APP_CONFIG, null) ?: return AppConfig()
        json.decodeFromString<AppConfig>(raw)
    }.getOrDefault(AppConfig())

    suspend fun fetchAppConfig(): AppConfig {
        val body = execute(get("/api/app-config"))
        val config: AppConfig = json.decodeFromString(body)
        prefs.edit().putString(KEY_APP_CONFIG, body).apply()
        return config
    }

    suspend fun saveAdminAppConfig(config: AppConfig): AppConfig {
        val encoded = json.encodeToString(AppConfig.serializer(), config)
        val body = execute(put("/api/admin/app-config", encoded))
        val saved: AppConfig = json.decodeFromString(body)
        prefs.edit().putString(KEY_APP_CONFIG, body).apply()
        return saved
    }

    // --- Health ---
    suspend fun pingHealth(): Boolean = runCatching {
        execute(get("/api/health"))
        true
    }.getOrDefault(false)

    // --- Classic Charades ---
    suspend fun fetchDecks(): List<CharadesDeck> {
        val body = execute(get("/api/decks"))
        val decks: List<CharadesDeck> = json.decodeFromString(body)
        if (decks.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                runCatching {
                    java.io.File(appContext.filesDir, "cached_decks.json").writeText(body)
                }
            }
        }
        return decks
    }

    fun loadCachedDecks(): List<CharadesDeck> = runCatching {
        val file = java.io.File(appContext.filesDir, "cached_decks.json")
        if (!file.exists()) return emptyList()
        json.decodeFromString<List<CharadesDeck>>(file.readText())
    }.getOrDefault(emptyList())

    // --- Nani Huyu? Multiplayer ---
    suspend fun fetchNaniDecks(): List<NaniDeck> {
        val body = execute(get("/api/nani/decks"))
        return json.decodeFromString(body)
    }

    suspend fun createNaniRoom(hostName: String, deckId: String): CreateOrJoinRoomResponse {
        val body = execute(
            post(
                "/api/nani/rooms",
                buildJsonObject {
                    put("hostName", hostName)
                    put("deckId", deckId)
                },
            ),
        )
        return json.decodeFromString(body)
    }

    suspend fun joinNaniRoom(roomCode: String, playerName: String): CreateOrJoinRoomResponse {
        val code = URLEncoder.encode(roomCode.trim().uppercase(), "UTF-8")
        val body = execute(
            post(
                "/api/nani/rooms/$code/join",
                buildJsonObject {
                    put("playerName", playerName)
                },
            ),
        )
        return json.decodeFromString(body)
    }

    suspend fun pollNaniRoom(roomCode: String, playerId: String): NaniRoom {
        val code = URLEncoder.encode(roomCode.trim().uppercase(), "UTF-8")
        val pid = URLEncoder.encode(playerId, "UTF-8")
        val body = execute(get("/api/nani/rooms/$code?playerId=$pid"))
        return json.decodeFromString(body)
    }

    suspend fun startNaniRound(roomCode: String, playerId: String, deckId: String? = null): NaniRoom {
        val code = URLEncoder.encode(roomCode.trim().uppercase(), "UTF-8")
        val body = execute(
            post(
                "/api/nani/rooms/$code/start",
                buildJsonObject {
                    put("playerId", playerId)
                    if (!deckId.isNullOrBlank()) put("deckId", deckId)
                },
            ),
        )
        return json.decodeFromString(body)
    }

    suspend fun submitNaniGuess(roomCode: String, playerId: String): NaniRoom {
        val code = URLEncoder.encode(roomCode.trim().uppercase(), "UTF-8")
        val body = execute(
            post(
                "/api/nani/rooms/$code/guess",
                buildJsonObject {
                    put("playerId", playerId)
                },
            ),
        )
        return json.decodeFromString(body)
    }

    suspend fun restartNaniRound(roomCode: String, playerId: String): NaniRoom {
        val code = URLEncoder.encode(roomCode.trim().uppercase(), "UTF-8")
        val body = execute(
            post(
                "/api/nani/rooms/$code/restart",
                buildJsonObject {
                    put("playerId", playerId)
                },
            ),
        )
        return json.decodeFromString(body)
    }

    suspend fun leaveNaniRoom(roomCode: String, playerId: String) {
        val code = URLEncoder.encode(roomCode.trim().uppercase(), "UTF-8")
        runCatching {
            execute(
                post(
                    "/api/nani/rooms/$code/leave",
                    buildJsonObject {
                        put("playerId", playerId)
                    },
                ),
            )
        }
    }

    // --- Auth & Admin Studio ---
    suspend fun login(email: String, password: String): AuthUser {
        val body = execute(
            post(
                "/api/login",
                buildJsonObject {
                    put("email", email)
                    put("password", password)
                },
            ),
        )
        return json.decodeFromString<AuthResponse>(body).user
    }

    suspend fun currentUser(): AuthUser? = runCatching {
        val body = execute(get("/api/me"))
        json.decodeFromString<AuthResponse>(body).user
    }.getOrNull()

    suspend fun logout() {
        runCatching { execute(post("/api/logout")) }
        prefs.edit().remove(KEY_SESSION_COOKIE).apply()
    }

    suspend fun fetchAdminDecks(): List<CharadesDeck> {
        val body = execute(get("/api/admin/decks"))
        return json.decodeFromString(body)
    }

    suspend fun createAdminDeck(
        name: String,
        description: String,
        icon: String,
        theme: String,
        coverUrl: String? = null,
    ): CharadesDeck {
        val body = execute(
            post(
                "/api/admin/decks",
                buildJsonObject {
                    put("name", name)
                    put("description", description)
                    put("icon", icon)
                    put("theme", theme)
                    if (!coverUrl.isNullOrBlank()) put("coverUrl", coverUrl)
                    put("isPublished", true)
                },
            ),
        )
        return json.decodeFromString(body)
    }

    suspend fun deleteAdminDeck(deckId: String) {
        execute(delete("/api/admin/decks/$deckId"))
    }

    suspend fun bulkAddCards(deckId: String, words: List<String>): Int {
        val body = execute(
            post(
                "/api/admin/decks/$deckId/cards/bulk",
                buildJsonObject {
                    put("cards", buildJsonArray { words.forEach { add(JsonPrimitive(it)) } })
                },
            ),
        )
        return json.decodeFromString<BulkCardsResponse>(body).count
    }

    suspend fun fetchNaniCategories(): List<NaniCategoryCount> {
        val body = execute(get("/api/nani/categories"))
        return json.decodeFromString(body)
    }

    suspend fun fetchCustomNaniCharacters(): List<NaniCard> {
        val body = execute(get("/api/nani/custom-characters"))
        return json.decodeFromString(body)
    }

    suspend fun createNaniCategory(name: String) {
        execute(
            post(
                "/api/admin/nani/categories",
                buildJsonObject { put("name", name) },
            ),
        )
    }

    suspend fun uploadNaniCharacter(category: String, name: String, imageDataUrl: String): NaniCard {
        val body = execute(
            post(
                "/api/admin/nani/characters",
                buildJsonObject {
                    put("category", category)
                    put("name", name)
                    put("image_url", imageDataUrl)
                },
            ),
        )
        return json.decodeFromString(body)
    }

    suspend fun deleteNaniCharacter(id: String) {
        execute(delete("/api/admin/nani/characters/$id"))
    }
}
