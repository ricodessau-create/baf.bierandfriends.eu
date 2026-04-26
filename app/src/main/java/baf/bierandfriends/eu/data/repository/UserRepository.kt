package baf.bierandfriends.eu.data.repository

import baf.bierandfriends.eu.data.models.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun getUserProfile(): UserProfile? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            db.collection("users")
                .document(uid)
                .get()
                .await()
                .toObject(UserProfile::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateUserProfile(profile: UserProfile) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users")
            .document(uid)
            .set(profile)
            .await()
    }

    suspend fun generateSyncToken(): String {
        val uid = auth.currentUser?.uid ?: return ""
        val token = (100000..999999).random().toString()
        db.collection("sync_tokens")
            .document(token)
            .set(mapOf("uid" to uid))
            .await()
        return token
    }
}
