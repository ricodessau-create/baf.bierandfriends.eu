package baf.bierandfriends.eu.data.models

data class UserProfile(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val rank: String = "Malzbier",
    val hopfenkaltschalen: Int = 0,
    val profileImageUrl: String? = null // NEU: URL zum Profilbild in Firebase Storage
)
