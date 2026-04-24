package com.deinprojekt.data.repository

import com.deinprojekt.data.models.UserProfile
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

    suspend fun saveSyncToken(token: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("sync_tokens")
            .document(token)
            .set(mapOf("uid" to uid))
            .await()
    }

    suspend fun updateSyncData(uuid: String, rankIngame: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .update(
                mapOf(
                    "uuid" to uuid,
                    "rank_ingame" to rankIngame,
                    "synced" to true
                )
            ).await()
    }
}
