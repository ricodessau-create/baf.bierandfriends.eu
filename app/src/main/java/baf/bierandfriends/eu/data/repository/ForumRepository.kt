package baf.bierandfriends.eu.data.repository

import baf.bierandfriends.eu.data.models.ForumPost
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ForumRepository {

    private val db = FirebaseFirestore.getInstance()

    suspend fun getLatestPosts(): List<ForumPost> {
        return try {
            db.collection("forum")
                .orderBy("createdAt")
                .get()
                .await()
                .toObjects(ForumPost::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun createPost(post: ForumPost) {
        db.collection("forum")
            .add(post)
            .await()
    }

    suspend fun deletePost(id: String) {
        db.collection("forum")
            .document(id)
            .delete()
            .await()
    }
}
