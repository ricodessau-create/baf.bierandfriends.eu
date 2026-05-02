package baf.bierandfriends.eu.data.repository

import baf.bierandfriends.eu.data.models.Ticket
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class TicketRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun createTicket(ticket: Ticket) {
        db.collection("tickets")
            .add(ticket)
            .await()
    }

    suspend fun getMyTickets(): List<Ticket> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        return try {
            db.collection("tickets")
                .whereEqualTo("authorUid", uid)
                .get()
                .await()
                .toObjects(Ticket::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAllTickets(): List<Ticket> {
        return try {
            db.collection("tickets")
                .get()
                .await()
                .toObjects(Ticket::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateTicketStatus(id: String, status: String) {
        try {
            db.collection("tickets")
                .document(id)
                .update("status", status)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun addTicketMessage(ticketId: String, text: String, authorName: String) {
        val uid = auth.currentUser?.uid ?: return
        val msg = hashMapOf(
            "text" to text,
            "authorUid" to uid,
            "authorName" to authorName,
            "createdAt" to Timestamp.now()
        )
        db.collection("tickets")
            .document(ticketId)
            .collection("messages")
            .add(msg)
            .await()
    }

    suspend fun getTicketMessages(ticketId: String): List<Map<String, Any>> {
        return try {
            val snapshot = db.collection("tickets")
                .document(ticketId)
                .collection("messages")
                .get()
                .await()
            snapshot.documents.map { doc ->
                val map = doc.data ?: emptyMap()
                map + mapOf("id" to doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
