package baf.bierandfriends.eu.data.repository

import baf.bierandfriends.eu.data.models.Event
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class EventsRepository {

    private val db = FirebaseFirestore.getInstance()

    suspend fun getUpcomingEvents(): List<Event> {
        return try {
            db.collection("events")
                .orderBy("date")
                .get()
                .await()
                .toObjects(Event::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun createEvent(event: Event) {
        db.collection("events")
            .add(event)
            .await()
    }

    suspend fun deleteEvent(id: String) {
        db.collection("events")
            .document(id)
            .delete()
            .await()
    }
}
