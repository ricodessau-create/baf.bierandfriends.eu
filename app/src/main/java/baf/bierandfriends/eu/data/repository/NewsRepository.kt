package baf.bierandfriends.eu.data.repository

import baf.bierandfriends.eu.data.models.News
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class NewsRepository {

    private val db = FirebaseFirestore.getInstance()

    suspend fun getLatestNews(): List<News> {
        return try {
            db.collection("news")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .await()
                .toObjects(News::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun createNews(news: News) {
        db.collection("news")
            .add(news)
            .await()
    }

    suspend fun deleteNews(id: String) {
        db.collection("news")
            .document(id)
            .delete()
            .await()
    }
}
