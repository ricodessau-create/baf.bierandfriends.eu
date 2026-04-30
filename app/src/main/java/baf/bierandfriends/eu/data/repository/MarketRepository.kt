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
import okhttp3.RequestBody.Companion.toRequestBody

class MarketRepository {

    private val firestore = FirebaseFirestore.getInstance()

    // Supabase
    private val supabaseUrl = "https://ghobutfhqaoopvlznrqr.supabase.co"
    private val supabaseKey = "sb-pub-..."   // dein PUBLIC Key
    private val bucket = "market"

    private val client = OkHttpClient()

    // ---------------------------------------------------------
    // MARKT – FIRESTORE
    // ---------------------------------------------------------

    suspend fun getMarketItems(): List<MarketItem> = withContext(Dispatchers.IO) {
        return@withContext try {
            firestore.collection("market")
                .orderBy("createdAt")
                .get()
                .await()
                .toObjects(MarketItem::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getMarketItemById(id: String): MarketItem? = withContext(Dispatchers.IO) {
        return@withContext try {
            firestore.collection("market")
                .document(id)
                .get()
                .await()
                .toObject(MarketItem::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun createMarketItem(item: MarketItem) = withContext(Dispatchers.IO) {
        firestore.collection("market")
            .document(item.id)
            .set(item)
            .await()
    }

    suspend fun deleteMarketItem(id: String) = withContext(Dispatchers.IO) {
        firestore.collection("market")
            .document(id)
            .delete()
            .await()
    }

    // ---------------------------------------------------------
    // SUPABASE – BILDER (ByteArray statt File)
    // ---------------------------------------------------------

    suspend fun uploadImage(bytes: ByteArray, id: String): String? = withContext(Dispatchers.IO) {
        val path = "$id.jpg"

        val body = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())

        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "$id.jpg", body)
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
        } else null
    }

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
