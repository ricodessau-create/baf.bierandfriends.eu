package baf.bierandfriends.eu.util

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID

object SupabaseHelper {

    private const val SUPABASE_URL = "https://ghobutfhqaoopvlznrqr.supabase.co"
    private const val SUPABASE_KEY = "sb-pub-..." // dein Key

    private val client = OkHttpClient()

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
        val body = response.body?.string()

        if (!response.isSuccessful) {
            throw Exception("Avatar Upload fehlgeschlagen: ${response.code} - $body")
        }

        return "$SUPABASE_URL/storage/v1/object/public/$bucket/$fileName"
    }

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
        val body = response.body?.string()

        if (!response.isSuccessful) {
            throw Exception("Market Upload fehlgeschlagen: ${response.code} - $body")
        }

        return "$SUPABASE_URL/storage/v1/object/public/$bucket/$fileName"
    }

    fun deleteImage(fullPath: String) {
        val request = Request.Builder()
            .url("$SUPABASE_URL/storage/v1/object/$fullPath")
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_KEY")
            .delete()
            .build()

        client.newCall(request).execute()
    }
}
