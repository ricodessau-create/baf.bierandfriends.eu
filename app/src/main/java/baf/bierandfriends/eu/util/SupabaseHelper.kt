package baf.bierandfriends.eu.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object SupabaseHelper {

    private const val SUPABASE_URL = "https://ghobutfhqaoopvlznrqr.supabase.co"
    private const val SUPABASE_KEY = "sb_publishable_wPIM_MdaMrfj-LsBkQWEhg_sU0PROr0"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    suspend fun uploadProfileImage(bytes: ByteArray, userId: String): String {
        return upload(bytes, "avatars", "profile_images/$userId.jpg")
    }

    suspend fun uploadMarketImage(bytes: ByteArray, itemId: String): String {
        return upload(bytes, "market", "market_images/$itemId.jpg")
    }

    private suspend fun upload(bytes: ByteArray, bucket: String, path: String): String {
        return withContext(Dispatchers.IO) {
            val url = "$SUPABASE_URL/storage/v1/object/$bucket/$path"
            val body = bytes.toRequestBody("image/jpeg".toMediaType())

            val request = Request.Builder()
                .url(url)
                .put(body)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .addHeader("Content-Type", "image/jpeg")
                .addHeader("x-upsert", "true")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e("Supabase", "Upload fehlgeschlagen: ${response.code} $responseBody")
                throw Exception("Upload fehlgeschlagen (${response.code}): $responseBody")
            }

            Log.d("Supabase", "Upload OK: $path")
            "$SUPABASE_URL/storage/v1/object/public/$bucket/$path"
        }
    }

    fun resetToken() {}
}
