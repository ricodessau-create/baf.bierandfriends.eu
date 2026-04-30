package baf.bierandfriends.eu.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import com.google.firebase.firestore.FirebaseFirestore

class MarketRepository {

    private val supabaseUrl = "https://ghobutfhqaoopvlznrqr.supabase.co"
    private val supabaseKey = "sb_publishable_wPIM_MdaMrfj-Ls..."   // NICHT Secret Key!
    private val bucket = "market"

    private val client = OkHttpClient()
    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Holt ein MarketItem aus Firestore
     */
    suspend fun getMarketItemById(id: String): MarketItem? = withContext(Dispatchers.IO) {
        val doc = firestore.collection("market").document(id).get().await()
        return@withContext doc.toObject(MarketItem::class.java)
    }

    /**
     * Löscht ein MarketItem aus Firestore
     */
    suspend fun deleteMarketItem(id: String) = withContext(Dispatchers.IO) {
        firestore.collection("market").document(id).delete().await()
    }

    /**
     * Upload einer Datei zu Supabase Storage
     */
    suspend fun uploadImage(file: File, id: String): String? = withContext(Dispatchers.IO) {
        val path = "$id.jpg"
        val mediaType = "image/jpeg".toMediaTypeOrNull()
        val body = file.asRequestBody(mediaType)

        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, body)
            .build()

        val request = Request.Builder()
            .url("$supabaseUrl/storage/v1/object/$bucket/$path")
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $supabaseKey")
            .post(multipart)
            .build()

        val response = client.newCall(request).execute()

        return@withContext if (response.isSuccessful) {
            "$supabaseUrl/storage/v1/object/public/$bucket/$path"
        } else {
            null
        }
    }

    /**
     * Löscht ein Bild aus Supabase Storage
     */
    suspend fun deleteImage(id: String) = withContext(Dispatchers.IO) {
        val path = "$id.jpg"

        val request = Request.Builder()
            .url("$supabaseUrl/storage/v1/object/$bucket/$path")
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $supabaseKey")
            .delete()
            .build()

        client.newCall(request).execute()
    }
}
