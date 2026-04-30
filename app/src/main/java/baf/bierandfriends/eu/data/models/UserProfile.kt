package baf.bierandfriends.eu.data.models

data class UserProfile(
    // Identität & Account
    val username: String = "",
    val email: String = "",
    val rank: String = "",
    val photoUrl: String = "",
    
    // Minecraft Integration
    val minecraftUuid: String = "",
    val minecraftName: String = "",
    
    // Statistiken & Währung
    val hopfenkaltschalen: Double = 0.0,
    
    // Profil-Informationen & Social
    val birthday: String = "", // Hier ist der Geburtstag enthalten
    val bio: String = "",
    val location: String = "",
    val discord: String = "",
    
    // Privatsphäre & Rechtliches (wichtig für die erste Version)
    val isPrivate: Boolean = false,
    val privacyAccepted: Boolean = false,
    val termsAccepted: Boolean = false
)
