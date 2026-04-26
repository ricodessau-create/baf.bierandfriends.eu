package baf.bierandfriends.eu.data.repository

import baf.bierandfriends.eu.data.models.Ticket
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
                .orderBy("createdAt")
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
                .orderBy("createdAt")
                .get()
                .await()
                .toObjects(Ticket::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateTicketStatus(id: String, status: String) {
        db.collection("tickets")
            .document(id)
            .update("status", status)
            .await()
    }
}
