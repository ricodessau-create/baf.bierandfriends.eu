package com.deinprojekt.data.repository

import com.deinprojekt.data.models.MarketItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class MarketRepository {

    private val db = FirebaseFirestore.getInstance()

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
}
