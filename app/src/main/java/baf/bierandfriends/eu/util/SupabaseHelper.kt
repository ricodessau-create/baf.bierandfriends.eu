package baf.bierandfriends.eu.util

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID

object SupabaseHelper {

    private const val SUPABASE_URL = "https://ghobutfhqaoopvlznrqr.supabase.co"
    private const val SUPABASE_KEY = "sb-pub-..." // DEIN PUBLIC KEY

    private val client = OkHttpClient()

    // ---------------------------------------------------------
    // AVATAR UPLOAD
    // ---------------------------------------------------------
    suspend fun uploadProfileImage(bytes: ByteArray, userId: String): String {
        val fileName = "$userId-${UUID.randomUUID()}.jpg"
        val bucket = "avatars"

        val request = Request.Builder()
            .url("$SUPABASE_URL/storage/v1/object/$bucket/$fileName")
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_KEY")
            .addHeader("Content-Type", "image/jpeg")
            .post(bytes.toRequestBody("image/jpeg".toMediaType()))
            .build()

        val response = client.newCall(request).execute()

        val responseBody = response.body?.string()

        if (!response.isSuccessful) {
            throw Exception("Upload fehlgeschlagen: ${response.code} - $responseBody")
        }

        return "$SUPABASE_URL/storage/v1/object/public/$bucket/$fileName"
    }

    // ---------------------------------------------------------
    // MARKET IMAGE UPLOAD
    // ---------------------------------------------------------
    suspend fun uploadMarketImage(bytes: ByteArray, itemId: String): String {
        val fileName = "$itemId-${UUID.randomUUID()}.jpg"
        val bucket = "market"

        val request = Request.Builder()
            .url("$SUPABASE_URL/storage/v1/object/$bucket/$fileName")
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_KEY")
            .addHeader("Content-Type", "image/jpeg")
            .post(bytes.toRequestBody("image/jpeg".toMediaType()))
            .build()

        val response = client.newCall(request).execute()

        val responseBody = response.body?.string()

        if (!response.isSuccessful) {
            throw Exception("Upload fehlgeschlagen: ${response.code} - $responseBody")
        }

        return "$SUPABASE_URL/storage/v1/object/public/$bucket/$fileName"
    }

    // ---------------------------------------------------------
    // DELETE IMAGE
    // ---------------------------------------------------------
    fun deleteImage(path: String) {
        val request = Request.Builder()
            .url("$SUPABASE_URL/storage/v1/object/$path")
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_KEY")
            .delete()
            .build()

        client.newCall(request).execute()
    }
}
