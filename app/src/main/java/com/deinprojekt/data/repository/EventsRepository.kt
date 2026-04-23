package com.deinprojekt.data.repository

import com.deinprojekt.data.models.Event
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
}
