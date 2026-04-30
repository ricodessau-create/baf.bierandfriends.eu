package baf.bierandfriends.eu.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object SupabaseHelper {

    private const val SUPABASE_URL = "https://ghobutfhqaoopvlznrqr.supabase.co"
    private const val SUPABASE_ANON_KEY = "sb_publishable_wPIM_MdaMrfj-LsBkQWEhg_sU0PROr0"
    const val BUCKET_AVATARS = "avatars"
    const val BUCKET_MARKET = "market"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    // Gecachter Auth-Token (anonymous Supabase Session)
    private var cachedToken: String? = null

    /**
     * Holt ein 'authenticated' JWT via Supabase Anonymous Sign-In.
     * Damit kann die Policy auth.role() = 'authenticated' erfüllt werden.
     */
    private suspend fun getAuthToken(): String = withContext(Dispatchers.IO) {
        cachedToken?.let { return@withContext it }

        val body = "{}".toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$SUPABASE_URL/auth/v1/signup")
            .post(body)
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Content-Type", "application/json")
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Auth Antwort leer")

        if (!response.isSuccessful) {
            Log.e("SupabaseHelper", "Auth fehlgeschlagen: ${response.code} $responseBody")
            throw Exception("Supabase Auth fehlgeschlagen: ${response.code}")
        }

        val json = JSONObject(responseBody)
        val token = json.optString("access_token")
            .ifEmpty { throw Exception("Kein access_token in Antwort: $responseBody") }

        cachedToken = token
        Log.d("SupabaseHelper", "Supabase Auth erfolgreich")
        token
    }

    /**
     * Lädt Profilbild in den 'avatars' Bucket hoch.
     */
    suspend fun uploadProfileImage(bytes: ByteArray, userId: String): String {
        val path = "profile_images/$userId.jpg"
        return upload(bytes, BUCKET_AVATARS, path)
    }

    /**
     * Lädt Markt-Bild in den 'market' Bucket hoch.
     */
    suspend fun uploadMarketImage(bytes: ByteArray, itemId: String): String {
        val path = "market_images/$itemId.jpg"
        return upload(bytes, BUCKET_MARKET, path)
    }

    private suspend fun upload(bytes: ByteArray, bucket: String, path: String): String {
        return withContext(Dispatchers.IO) {
            val token = getAuthToken()
            val url = "$SUPABASE_URL/storage/v1/object/$bucket/$path"
            val body = bytes.toRequestBody("image/jpeg".toMediaType())

            val request = Request.Builder()
                .url(url)
                .put(body)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "image/jpeg")
                .addHeader("x-upsert", "true")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e("SupabaseHelper", "Upload fehlgeschlagen: ${response.code} $responseBody")
                // Token zurücksetzen, falls abgelaufen
                if (response.code == 401 || response.code == 403) {
                    cachedToken = null
                }
                throw Exception("Upload fehlgeschlagen (${response.code}): $responseBody")
            }

            Log.d("SupabaseHelper", "Upload erfolgreich: $path")
            getPublicUrl(bucket, path)
        }
    }

    fun getPublicUrl(bucket: String, path: String): String {
        return "$SUPABASE_URL/storage/v1/object/public/$bucket/$path"
    }

    /**
     * Token zurücksetzen (z.B. bei Logout)
     */
    fun resetToken() {
        cachedToken = null
    }
}
