package baf.bierandfriends.eu.data.repository

import baf.bierandfriends.eu.data.models.MarketItem
import baf.bierandfriends.eu.util.SupabaseHelper
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class MarketRepository {

    private val db = FirebaseFirestore.getInstance()

    suspend fun getMarketItems(): List<MarketItem> {
        return try {
            db.collection("market").get().await()
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
            db.collection("market").document(id).get().await()
                .toObject(MarketItem::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteMarketItem(id: String) {
        db.collection("market").document(id).delete().await()
    }

    /**
     * Marktbild via Supabase Storage hochladen.
     * Gibt die öffentliche URL zurück.
     */
    suspend fun uploadImage(bytes: ByteArray, itemId: String): String {
        return SupabaseHelper.uploadMarketImage(bytes, itemId)
    }
}
