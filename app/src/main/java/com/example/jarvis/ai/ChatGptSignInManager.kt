package com.example.jarvis.ai

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.math.BigInteger
import java.net.InetAddress
import java.net.ServerSocket
import java.net.URLDecoder
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.RSAPublicKeySpec
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Sign in with ChatGPT for the OpenAI open-source client flow.
 * Tokens are protected by AndroidX EncryptedSharedPreferences.
 * No client secret, API key, cookies, or private endpoints are used.
 */
class ChatGptSignInManager(context: Context) {
    data class Account(val email: String, val planUsageEnabled: Boolean)

    companion object {
        private const val AUTH_URL = "https://auth.openai.com/api/accounts/authorize"
        private const val TOKEN_URL = "https://auth.openai.com/api/accounts/oauth/token"
        private const val JWKS_URL = "https://auth.openai.com/.well-known/jwks.json"
        private const val RESOURCE = "https://api.openai.com/v1"
        private const val REQUIRED_SCOPE = "chatgpt.tokens.use.direct"
        private const val CALLBACK_PATH = "/auth/callback"
    }

    private val app = context.applicationContext
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .callTimeout(40, TimeUnit.SECONDS)
        .build()
    private val refreshMutex = Mutex()

    private val prefs by lazy {
        val master = MasterKey.Builder(app)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            app,
            "naeem_jarvis_chatgpt",
            master,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun isConnected(): Boolean = prefs.getString("refresh_token", null) != null
    fun hasPlanUsage(): Boolean =
        isConnected() && prefs.getBoolean("plan_usage_enabled", false)
    fun connectedEmail(): String = prefs.getString("email", "").orEmpty()

    suspend fun signIn(activity: Activity): Account = withContext(Dispatchers.IO) {
        val hostId = prefs.getString("host_id", null) ?: "urn:uuid:${UUID.randomUUID()}".also {
            prefs.edit().putString("host_id", it).commit()
        }
        val state = randomToken(32)
        val nonce = randomToken(32)
        val verifier = randomToken(64)
        val challenge = b64Url(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))

        ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { server ->
            server.soTimeout = 180_000
            val redirectUri = "http://127.0.0.1:${server.localPort}$CALLBACK_PATH"
            val auth = Uri.parse(AUTH_URL).buildUpon()
                .appendQueryParameter("client_id", "dynamic_agent_client")
                .appendQueryParameter("agent_name_hint", "NAEEM JARVIS")
                .appendQueryParameter("ext_agent_host_id", hostId)
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("redirect_uri", redirectUri)
                .appendQueryParameter("scope", "openid profile email offline_access resource.invoke $REQUIRED_SCOPE")
                .appendQueryParameter("resource", RESOURCE)
                .appendQueryParameter("state", state)
                .appendQueryParameter("nonce", nonce)
                .appendQueryParameter("code_challenge_method", "S256")
                .appendQueryParameter("code_challenge", challenge)
                .build()

            withContext(Dispatchers.Main) {
                activity.startActivity(Intent(Intent.ACTION_VIEW, auth).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }

            val callback = server.accept()
            callback.soTimeout = 10_000
            val requestLine = BufferedReader(InputStreamReader(callback.getInputStream(), Charsets.UTF_8)).readLine()
                ?: throw IllegalStateException("OpenAI callback was empty.")
            val target = requestLine.split(" ").getOrNull(1)
                ?: throw IllegalStateException("OpenAI callback was invalid.")
            val query = Uri.parse("http://127.0.0.1$target").queryParameterNames.associateWith {
                Uri.parse("http://127.0.0.1$target").getQueryParameter(it).orEmpty()
            }
            val html = if (query["state"] == state && query["error"] == null) {
                "<html><body><h2>NAEEM JARVIS</h2><p>Sign-in received. Return to the app.</p></body></html>"
            } else {
                "<html><body><h2>NAEEM JARVIS</h2><p>Sign-in was not completed. Return to the app.</p></body></html>"
            }
            OutputStreamWriter(callback.getOutputStream(), Charsets.UTF_8).use {
                it.write("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nConnection: close\r\nContent-Length: ${html.toByteArray().size}\r\n\r\n$html")
                it.flush()
            }
            callback.close()

            if (query["state"] != state) throw SecurityException("OAuth state did not match.")
            query["error"]?.let { throw IllegalStateException("ChatGPT sign-in was not authorized: $it") }
            val code = query["code"] ?: throw IllegalStateException("OpenAI did not return an authorization code.")
            val issuedClientId = query["client_id"]?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("OpenAI did not return the issued client ID. Registration is incomplete.")

            val token = exchangeCode(code, issuedClientId, verifier, redirectUri)
            val idToken = token.optString("id_token")
            if (idToken.isBlank()) throw SecurityException("OpenAI did not return an ID token.")
            val identity = verifyIdToken(idToken, issuedClientId, nonce)
            val scopes = token.optString("scope").split(" ").filter { it.isNotBlank() }.toSet()
            val refresh = token.optString("refresh_token")
            val access = token.optString("access_token")
            if (refresh.isBlank() || access.isBlank()) throw IllegalStateException("OpenAI did not return renewable credentials.")

            val planEnabled = REQUIRED_SCOPE in scopes
            prefs.edit()
                .putString("client_id", issuedClientId)
                .putString("email", identity.optString("email"))
                .putString("subject", identity.getString("sub"))
                .putString("id_token", idToken)
                .putString("access_token", access)
                .putString("refresh_token", refresh)
                .putString("scope", scopes.joinToString(" "))
                .putLong("expires_at", System.currentTimeMillis() + token.optLong("expires_in", 3600L) * 1000L)
                .putBoolean("plan_usage_enabled", planEnabled)
                .commit()
            Account(identity.optString("email"), planEnabled)
        }
    }

    suspend fun validAccessToken(): String = refreshMutex.withLock {
        withContext(Dispatchers.IO) {
            if (!hasPlanUsage()) throw IllegalStateException("ChatGPT plan usage permission is not enabled for this connection.")
            val access = prefs.getString("access_token", null).orEmpty()
            val expiry = prefs.getLong("expires_at", 0L)
            if (access.isNotBlank() && System.currentTimeMillis() < expiry - 60_000L) return@withContext access

            val clientId = prefs.getString("client_id", null)
                ?: throw IllegalStateException("ChatGPT connection is missing its client registration.")
            val refresh = prefs.getString("refresh_token", null)
                ?: throw IllegalStateException("ChatGPT connection needs sign-in again.")
            val body = FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("client_id", clientId)
                .add("refresh_token", refresh)
                .add("resource", RESOURCE)
                .build()
            val request = Request.Builder().url(TOKEN_URL).post(body).build()
            val json = http.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw IllegalStateException("ChatGPT token refresh failed (${response.code}): ${text.take(300)}")
                JSONObject(text)
            }
            val newAccess = json.optString("access_token")
            val newRefresh = json.optString("refresh_token")
            if (newAccess.isBlank() || newRefresh.isBlank()) throw IllegalStateException("ChatGPT did not rotate the session credentials.")
            prefs.edit()
                .putString("access_token", newAccess)
                .putString("refresh_token", newRefresh)
                .putLong("expires_at", System.currentTimeMillis() + json.optLong("expires_in", 3600L) * 1000L)
                .commit()
            newAccess
        }
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        val refresh = prefs.getString("refresh_token", null)
        val clientId = prefs.getString("client_id", null)
        if (!refresh.isNullOrBlank() && !clientId.isNullOrBlank()) {
            try {
                val config = http.newCall(Request.Builder()
                    .url("https://auth.openai.com/.well-known/openid-configuration").build())
                    .execute().use { JSONObject(it.body?.string().orEmpty()) }
                val endpoint = config.optString("revocation_endpoint")
                if (endpoint.isNotBlank()) {
                    val body = FormBody.Builder()
                        .add("token", refresh)
                        .add("token_type_hint", "refresh_token")
                        .add("client_id", clientId)
                        .build()
                    http.newCall(Request.Builder().url(endpoint).post(body).build()).execute().close()
                }
            } catch (_: Exception) {
                // Local sign-out still clears credentials; user can revoke from ChatGPT settings.
            }
        }
        prefs.edit().clear().commit()
    }

    private fun exchangeCode(code: String, clientId: String, verifier: String, redirectUri: String): JSONObject {
        val body = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("client_id", clientId)
            .add("code", code)
            .add("code_verifier", verifier)
            .add("redirect_uri", redirectUri)
            .add("resource", RESOURCE)
            .build()
        val request = Request.Builder().url(TOKEN_URL).post(body).build()
        return http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IllegalStateException("ChatGPT code exchange failed (${response.code}): ${text.take(400)}")
            JSONObject(text)
        }
    }

    private fun verifyIdToken(jwt: String, clientId: String, expectedNonce: String): JSONObject {
        val parts = jwt.split(".")
        if (parts.size != 3) throw SecurityException("OpenAI ID token format was invalid.")
        val decoder = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        val header = JSONObject(String(Base64.decode(parts[0], decoder), Charsets.UTF_8))
        val claims = JSONObject(String(Base64.decode(parts[1], decoder), Charsets.UTF_8))
        if (header.optString("alg") != "RS256") throw SecurityException("Unsupported ID-token signature algorithm.")
        val jwks = http.newCall(Request.Builder().url(JWKS_URL).build()).execute().use {
            if (!it.isSuccessful) throw SecurityException("Could not load OpenAI signing keys.")
            JSONObject(it.body?.string().orEmpty()).getJSONArray("keys")
        }
        val kid = header.optString("kid")
        val jwk = (0 until jwks.length()).map { jwks.getJSONObject(it) }
            .firstOrNull { it.optString("kid") == kid && it.optString("kty") == "RSA" }
            ?: throw SecurityException("OpenAI signing key was not found.")
        val modulus = BigInteger(1, Base64.decode(jwk.getString("n"), decoder))
        val exponent = BigInteger(1, Base64.decode(jwk.getString("e"), decoder))
        val publicKey = KeyFactory.getInstance("RSA").generatePublic(RSAPublicKeySpec(modulus, exponent))
        val verifier = Signature.getInstance("SHA256withRSA")
        verifier.initVerify(publicKey)
        verifier.update("${parts[0]}.${parts[1]}".toByteArray(Charsets.US_ASCII))
        if (!verifier.verify(Base64.decode(parts[2], decoder))) throw SecurityException("OpenAI ID-token signature was invalid.")

        if (claims.optString("iss") != "https://auth.openai.com") throw SecurityException("OpenAI ID-token issuer did not match.")
        val audience = claims.opt("aud")
        val audienceMatches = when (audience) {
            is String -> audience == clientId
            is org.json.JSONArray -> (0 until audience.length()).any { audience.optString(it) == clientId }
            else -> false
        }
        if (!audienceMatches) throw SecurityException("OpenAI ID-token audience did not match this registration.")
        if (claims.optLong("exp", 0L) <= System.currentTimeMillis() / 1000L) throw SecurityException("OpenAI ID token has expired.")
        if (claims.optString("nonce") != expectedNonce) throw SecurityException("OpenAI ID-token nonce did not match.")
        if (claims.optString("sub").isBlank()) throw SecurityException("OpenAI ID token has no subject.")
        return claims
    }

    private fun randomToken(bytes: Int): String {
        val data = ByteArray(bytes)
        SecureRandom().nextBytes(data)
        return b64Url(data)
    }

    private fun b64Url(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
}
