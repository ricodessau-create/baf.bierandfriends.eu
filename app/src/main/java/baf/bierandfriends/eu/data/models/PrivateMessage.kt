package baf.bierandfriends.eu.data.models

import com.google.firebase.Timestamp

data class PrivateMessage(
    val id: String = "",
    val text: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val receiverUid: String = "",
    val createdAt: Timestamp? = null
)
