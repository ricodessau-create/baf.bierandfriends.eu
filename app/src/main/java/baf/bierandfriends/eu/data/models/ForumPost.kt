package baf.bierandfriends.eu.data.models

import com.google.firebase.Timestamp

data class ForumPost(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val author: String = "",
    val authorUid: String = "",
    val createdAt: Timestamp? = null
)
