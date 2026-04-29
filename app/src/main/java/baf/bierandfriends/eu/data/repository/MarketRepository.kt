package baf.bierandfriends.eu.data.repository

import baf.bierandfriends.eu.data.models.MarketItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class MarketRepository {

    private val db = FirebaseFirestore.getInstance()

    // Supabase Storage
    private val supabaseUrl = "https://ghobutfhqaoopvlznrqr.supabase.co"
    private val supabaseKey = "sb_publishable_wPIM_MdaMrfj-Ls..."   // Falls du Secret Key nutzt → NICHT in der App speichern!
    private val bucket = "market"

    private val client = OkHttpClient()

    suspend fun getMarketItems(): List<MarketItem> {
        return try {
            db.collection("market").get().await().toObjects(MarketItem::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getItemsByType(type: String): List<MarketItem> {
        return try {
            db.collection("market")
                .whereEqualTo("type", type)
                .get()
                .await()
                .toObjects(MarketItem::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun createMarketItem(item: MarketItem) {
        db.collection("market").document(item.id).set(item).await()
    }

    suspend fun getMarketItemById(id: String): MarketItem? {
        return try {
            db.collection("market").document(id).get().await().toObject(MarketItem::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateMarketItem(item: MarketItem) {
        db.collection("market").document(item.id).set(item).await()
    }

    suspend fun deleteMarketItem(id: String) {
        db.collection("market").document(id).delete().await()
    }

    /**
     * Upload einer Datei zu Supabase Storage
     */
    suspend fun uploadImage(file: File, path: String): String? = withContext(Dispatchers.IO) {
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
     * Löschen einer Datei aus Supabase Storage
     */
    suspend fun deleteImage(path: String): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$supabaseUrl/storage/v1/object/$bucket/$path")
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $supabaseKey")
            .delete()
            .build()

        val response = client.newCall(request).execute()
        return@withContext response.isSuccessful
    }
}
