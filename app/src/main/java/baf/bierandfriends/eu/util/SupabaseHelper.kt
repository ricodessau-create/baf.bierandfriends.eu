package baf.bierandfriends.eu.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID

object SupabaseHelper {

    private const val SUPABASE_URL = "https://ghobutfhqaoopvlznrqr.supabase.co"
    private const val SUPABASE_ANON_KEY = "sb_publishable_wPIM_MdaMrfj-LsBkQWEhg_sU0PROr0"
    private const val BUCKET = "baf-images"

    private val client = OkHttpClient()

    suspend fun uploadImage(bytes: ByteArray, folder: String, filename: String? = null): String {
        return withContext(Dispatchers.IO) {
            val name = filename ?: "${UUID.randomUUID()}.jpg"
            val path = "$folder/$name"
            val url = "$SUPABASE_URL/storage/v1/object/$BUCKET/$path"

            val body = bytes.toRequestBody("image/jpeg".toMediaType())

            val request = Request.Builder()
                .url(url)
                .post(body)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .addHeader("Content-Type", "image/jpeg")
                .addHeader("x-upsert", "true")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                throw Exception("Upload fehlgeschlagen: ${response.code} ${response.body?.string()}")
            }

            getPublicUrl(path)
        }
    }

    fun getPublicUrl(path: String): String {
        return "$SUPABASE_URL/storage/v1/object/public/$BUCKET/$path"
    }
}
