package baf.bierandfriends.eu.data.models

import com.google.firebase.Timestamp

data class Event(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val date: Timestamp? = null
)
