package baf.bierandfriends.eu.util

import baf.bierandfriends.eu.data.models.UserProfile

object PermissionUtils {

    private val supportRoles = setOf("supporter", "trainee", "moderator", "admin")

    /**
     * Prüft, ob das übergebene Profil eine Support-Rolle hat.
     */
    fun isSupportRole(profile: UserProfile?): Boolean {
        return profile?.rank?.lowercase() in supportRoles
    }

    /**
     * Clientseitige UX-Prüfung: alle angemeldeten Nutzer dürfen im öffentlichen Chat schreiben.
     * Die echte Autorisierung erfolgt über Firestore Rules.
     */
    fun canWritePublicChat(): Boolean {
        return true
    }

    /**
     * Prüft, ob die angegebene UID Teilnehmer des privaten Chats ist.
     * chatId-Format: "uid1_uid2" (lexicographisch sortiert).
     */
    fun isParticipantInPrivateChat(chatId: String, uid: String?): Boolean {
        if (uid == null) return false
        return chatId.startsWith("${uid}_") || chatId.endsWith("_${uid}")
    }

    /**
     * Prüft, ob der aktuelle Nutzer in einem Ticket schreiben darf:
     * - Ticket-Ersteller darf schreiben
     * - Support-Rollen dürfen schreiben
     */
    fun canWriteTicket(profile: UserProfile?, ticketAuthorUid: String?, currentUid: String?): Boolean {
        if (currentUid == null) return false
        if (ticketAuthorUid == currentUid) return true
        return isSupportRole(profile)
    }
}
