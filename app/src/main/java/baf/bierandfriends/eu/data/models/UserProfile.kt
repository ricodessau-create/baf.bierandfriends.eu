package baf.bierandfriends.eu.data.models

data class UserProfile(
    val username: String = "",
    val email: String = "",
    val rank: String = "",
    val minecraftUuid: String = "",
    val minecraftName: String = "",
    val hopfenkaltschalen: Double = 0.0,
    val photoUrl: String = "",
    val birthday: String = "",
    val bio: String = "",
    val location: String = "",
    val discord: String = "",
    val isPrivate: Boolean = false
)
