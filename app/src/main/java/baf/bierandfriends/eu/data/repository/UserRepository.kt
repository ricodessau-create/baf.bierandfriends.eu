package baf.bierandfriends.eu.data.repository

import android.net.Uri
import baf.bierandfriends.eu.data.models.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // ... bestehende Funktionen (getUserProfile, etc.) ...

    // NEU: Lädt Bild hoch und aktualisiert das Profil
    suspend fun uploadProfileImage(imageUri: Uri): String? {
        val uid = auth.currentUser?.uid ?: return null
        val storageRef = storage.reference.child("profile_images/$uid.jpg")

        return try {
            // 1. Bild hochladen
            storageRef.putFile(imageUri).await()
            // 2. Download-URL holen
            val downloadUrl = storageRef.downloadUrl.await().toString()
            // 3. URL im Firestore-Profil speichern
            firestore.collection("users").document(uid)
                .update("profileImageUrl", downloadUrl).await()
            
            downloadUrl
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
