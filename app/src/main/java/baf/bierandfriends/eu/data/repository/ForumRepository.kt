package baf.bierandfriends.eu.data.repository

import baf.bierandfriends.eu.data.models.ForumPost
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ForumRepository {

    private val db = FirebaseFirestore.getInstance()

    suspend fun getLatestPosts(): List<ForumPost> {
        return try {
            val snapshot = db.collection("forum")
                .get()
                .await()
            val posts = snapshot.toObjects(ForumPost::class.java)
            val ids = snapshot.documents.map { it.id }
            posts.mapIndexed { i, p -> p.copy(id = ids[i]) }
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
