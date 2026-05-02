package baf.bierandfriends.eu.data.repository

import baf.bierandfriends.eu.data.models.MarketItem
import baf.bierandfriends.eu.util.SupabaseHelper
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MarketRepository {

    private val firestore = FirebaseFirestore.getInstance()

    // ---------------------------------------------------------
    // MARKT – FIRESTORE (BLEIBT UNVERÄNDERT)
    // ---------------------------------------------------------

    suspend fun getMarketItems(): List<MarketItem> = withContext(Dispatchers.IO) {
        return@withContext try {
            firestore.collection("market")
                .orderBy("createdAt")
                .get()
                .await()
                .toObjects(MarketItem::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
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
            e.printStackTrace()
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
    // SUPABASE – BILDER (FIXED)
    // ---------------------------------------------------------

    suspend fun uploadImage(bytes: ByteArray, id: String): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            SupabaseHelper.uploadMarketImage(bytes, id)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun deleteImage(fullPath: String) = withContext(Dispatchers.IO) {
        try {
            SupabaseHelper.deleteImage(fullPath)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
