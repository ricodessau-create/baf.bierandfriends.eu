package baf.bierandfriends.eu.data.models

data class Ticket(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val status: String = "offen",
    val createdAt: Long = 0
)
