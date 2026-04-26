package baf.bierandfriends.eu.data.models

data class ForumPost(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val author: String = "",
    val authorUid: String = "",
    val createdAt: Long = 0
)
