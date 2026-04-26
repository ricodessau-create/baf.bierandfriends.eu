package baf.bierandfriends.eu.data.repository

import baf.bierandfriends.eu.data.models.MarketItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class MarketRepository {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    suspend fun getMarketItems(): List<MarketItem> {
        return try {
            db.collection("market")
                .orderBy("createdAt")
                .get()
                .await()
                .toObjects(MarketItem::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun createMarketItem(item: MarketItem) {
        db.collection("market")
            .document(item.id)
            .set(item)
            .await()
    }

    suspend fun getMarketItemById(id: String): MarketItem? {
        return try {
            db.collection("market")
                .document(id)
                .get()
                .await()
                .toObject(MarketItem::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateMarketItem(item: MarketItem) {
        db.collection("market")
            .document(item.id)
            .set(item)
            .await()
    }

    suspend fun deleteMarketItem(id: String) {
        db.collection("market")
            .document(id)
            .delete()
            .await()
    }

    suspend fun deleteImage(id: String) {
        val ref = storage.getReference("market_images/$id.jpg")
        try {
            ref.delete().await()
        } catch (e: Exception) {
            // Bild existiert nicht → ignorieren
        }
    }
}
