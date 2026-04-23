package com.deinprojekt.data.repository

import com.deinprojekt.data.models.MarketItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class MarketRepository {

    private val db = FirebaseFirestore.getInstance()

    suspend fun getMarketItems(): List<MarketItem> {
        return try {
            db.collection("market")
                .orderBy("createdAt") // Timestamp-Sortierung
                .get()
                .await()
                .toObjects(MarketItem::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
