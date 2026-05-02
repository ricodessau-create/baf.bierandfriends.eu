package baf.bierandfriends.eu.util

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID

object SupabaseHelper {

    private const val SUPABASE_URL = "https://ghobutfhqaoopvlznrqr.supabase.co"
    private const val SUPABASE_KEY = "sb-pub-..." 

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
        if (!response.isSuccessful) throw Exception("Avatar upload failed: ${response.code} - $body")
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
        if (!response.isSuccessful) throw Exception("Market upload failed: ${response.code} - $body")
        return "$SUPABASE_URL/storage/v1/object/public/$bucket/$fileName"
    }

    suspend fun deleteImage(fullPath: String): Boolean {
        val request = Request.Builder()
            .url("$SUPABASE_URL/storage/v1/object/$fullPath")
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_KEY")
            .delete()
            .build()
        val response = client.newCall(request).execute()
        response.body?.close()
        return response.isSuccessful
    }

    suspend fun resetToken(token: String?): Boolean {
        if (token.isNullOrBlank()) return false
        val url = "$SUPABASE_URL/rest/v1/sync_tokens?token=eq.$token"
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_KEY")
            .addHeader("Prefer", "return=representation")
            .delete()
            .build()
        val response = client.newCall(request).execute()
        response.body?.close()
        return response.isSuccessful
    }
}
