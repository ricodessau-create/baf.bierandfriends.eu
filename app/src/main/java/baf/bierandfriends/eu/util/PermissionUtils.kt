package baf.bierandfriends.eu.util

import baf.bierandfriends.eu.data.models.UserProfile

object PermissionUtils {

    private val supportRoles = setOf("supporter", "trainee", "moderator", "admin")

    fun isSupportRole(profile: UserProfile?): Boolean {
        return profile?.rank?.lowercase() in supportRoles
    }

    fun canWritePublicChat(): Boolean {
        return true
    }

    fun isParticipantInPrivateChat(chatId: String, uid: String?): Boolean {
        if (uid == null) return false
        return chatId.startsWith("${uid}_") || chatId.endsWith("_${uid}")
    }

    fun canWriteTicket(profile: UserProfile?, ticketAuthorUid: String?, currentUid: String?): Boolean {
        if (currentUid == null) return false
        if (ticketAuthorUid == currentUid) return true
        return isSupportRole(profile)
    }
}
