package com.deinprojekt.data.repository

import com.deinprojekt.data.models.News
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class NewsRepository {

    private val db = FirebaseFirestore.getInstance()

    suspend fun getLatestNews(): List<News> {
        return try {
            db.collection("news")
                .orderBy("timestamp")
                .limit(5)
                .get()
                .await()
                .toObjects(News::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
