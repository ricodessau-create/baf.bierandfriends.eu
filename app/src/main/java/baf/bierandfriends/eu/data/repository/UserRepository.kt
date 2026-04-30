package baf.bierandfriends.eu.data.repository

import baf.bierandfriends.eu.data.models.UserProfile
import baf.bierandfriends.eu.util.SupabaseHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun getUserProfile(): UserProfile? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            val doc = db.collection("users").document(uid).get().await()
            if (doc.exists()) {
                doc.toObject(UserProfile::class.java)
            } else {
                val defaultProfile = UserProfile(
                    username = auth.currentUser?.displayName ?: "Spieler",
                    email = auth.currentUser?.email ?: "",
                    rank = "malzbier",
                    photoUrl = auth.currentUser?.photoUrl?.toString() ?: ""
                )
                db.collection("users").document(uid).set(defaultProfile).await()
                defaultProfile
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUserProfileById(uid: String): UserProfile? {
        return try {
            db.collection("users").document(uid).get().await()
                .toObject(UserProfile::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateUserProfile(profile: UserProfile) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).set(profile).await()
    }

    /**
     * Profilbild via Supabase Storage hochladen.
     * Gibt die öffentliche URL zurück.
     */
    suspend fun uploadAvatar(bytes: ByteArray): String {
        val uid = auth.currentUser?.uid
            ?: throw Exception("Nicht eingeloggt")
        return SupabaseHelper.uploadProfileImage(bytes, uid)
    }

    suspend fun generateSyncToken(): String {
        val uid = auth.currentUser?.uid ?: return ""
        val token = (100000..999999).random().toString()
        db.collection("sync_tokens").document(token)
            .set(mapOf("uid" to uid)).await()
        return token
    }

    suspend fun ignoreUser(targetUid: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("ignored_users").document(uid)
            .collection("list").document(targetUid)
            .set(mapOf("uid" to targetUid)).await()
    }

    suspend fun unignoreUser(targetUid: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("ignored_users").document(uid)
            .collection("list").document(targetUid)
            .delete().await()
    }

    suspend fun isUserIgnored(targetUid: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            db.collection("ignored_users").document(uid)
                .collection("list").document(targetUid)
                .get().await().exists()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getAllUsers(): List<UserProfile> {
        return try {
            db.collection("users").get().await()
                .toObjects(UserProfile::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
