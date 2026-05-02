package baf.bierandfriends.eu.data.models

data class UserProfile(
    val username: String = "",
    val email: String = "",
    val rank: String = "",
    val photoUrl: String = "",
    val minecraftUuid: String = "",
    val minecraftName: String = "",
    val hopfenkaltschalen: Double = 0.0,
    val birthday: String = "",
    val bio: String = "",
    val location: String = "",
    val discord: String = "",
    val isPrivate: Boolean = false,
    val privacyAccepted: Boolean = false,
    val termsAccepted: Boolean = false,
    val syncToken: String? = null
)
