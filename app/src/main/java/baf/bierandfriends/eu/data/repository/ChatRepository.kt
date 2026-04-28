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
            db.collection("public_chat")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .limitToLast(50)
                .get()
                .await()
                .toObjects(ChatMessage::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun sendPublicMessage(text: String, authorName: String, authorRank: String) {
        val uid = auth.currentUser?.uid ?: return
        val message = ChatMessage(
            text = text,
            authorUid = uid,
            authorName = authorName,
            authorRank = authorRank,
            createdAt = Timestamp.now()
        )
        db.collection("public_chat").add(message).await()
    }

    suspend fun getPrivateMessages(otherUid: String): List<PrivateMessage> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        val chatId = if (uid < otherUid) "${uid}_${otherUid}" else "${otherUid}_${uid}"
        return try {
            db.collection("private_chats")
                .document(chatId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .await()
                .toObjects(PrivateMessage::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun sendPrivateMessage(text: String, receiverUid: String, senderName: String) {
        val uid = auth.currentUser?.uid ?: return
        val chatId = if (uid < receiverUid) "${uid}_${receiverUid}" else "${receiverUid}_${uid}"
        val message = PrivateMessage(
            text = text,
            senderUid = uid,
            senderName = senderName,
            receiverUid = receiverUid,
            createdAt = Timestamp.now()
        )
        db.collection("private_chats")
            .document(chatId)
            .collection("messages")
            .add(message)
            .await()
    }
}
