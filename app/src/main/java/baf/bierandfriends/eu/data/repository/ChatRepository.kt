package baf.bierandfriends.eu.data.repository

import baf.bierandfriends.eu.data.models.ChatMessage
import baf.bierandfriends.eu.data.models.PrivateMessage
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class ChatRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun getPublicMessages(): List<ChatMessage> {
        return try {
            // Ohne orderBy – kein Index nötig
            val docs = db.collection("public_chat")
                .get().await()
            val messages = docs.documents.mapNotNull { doc ->
                doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
            }
            // In der App nach Zeit sortieren
            messages.sortedBy { it.createdAt?.seconds ?: 0L }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun sendPublicMessage(text: String, authorName: String, authorRank: String) {
        val uid = auth.currentUser?.uid ?: return
        val message = hashMapOf(
            "text" to text,
            "authorUid" to uid,
            "authorName" to authorName,
            "authorRank" to authorRank,
            "createdAt" to Timestamp.now(),
            "id" to "",
            "photoUrl" to ""
        )
        db.collection("public_chat").add(message).await()
    }

    suspend fun getPrivateMessages(otherUid: String): List<PrivateMessage> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        val chatId = if (uid < otherUid) "${uid}_${otherUid}" else "${otherUid}_${uid}"
        return try {
            val docs = db.collection("private_chats")
                .document(chatId)
                .collection("messages")
                .get().await()
            val messages = docs.documents.mapNotNull { doc ->
                doc.toObject(PrivateMessage::class.java)?.copy(id = doc.id)
            }
            messages.sortedBy { it.createdAt?.seconds ?: 0L }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun sendPrivateMessage(text: String, receiverUid: String, senderName: String) {
        val uid = auth.currentUser?.uid ?: return
        val chatId = if (uid < receiverUid) "${uid}_${receiverUid}" else "${receiverUid}_${uid}"
        val message = hashMapOf(
            "text" to text,
            "senderUid" to uid,
            "senderName" to senderName,
            "receiverUid" to receiverUid,
            "createdAt" to Timestamp.now(),
            "id" to ""
        )
        db.collection("private_chats")
            .document(chatId)
            .collection("messages")
            .add(message).await()
    }
}
