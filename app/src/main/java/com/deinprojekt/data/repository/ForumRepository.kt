package com.deinprojekt.data.repository

import com.deinprojekt.data.models.ForumPost
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ForumRepository {

    private val db = FirebaseFirestore.getInstance()

    suspend fun getLatestPosts(): List<ForumPost> {
        return try {
            db.collection("forum")
                .orderBy("createdAt")
                .limit(10)
                .get()
                .await()
                .toObjects(ForumPost::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
