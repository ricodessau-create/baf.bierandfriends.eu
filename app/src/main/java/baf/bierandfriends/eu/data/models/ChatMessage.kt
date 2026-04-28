package baf.bierandfriends.eu.data.models

import com.google.firebase.Timestamp

data class ChatMessage(
    val id: String = "",
    val text: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val authorRank: String = "",
    val createdAt: Timestamp? = null,
    val photoUrl: String = ""
)
