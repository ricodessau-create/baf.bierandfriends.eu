package baf.bierandfriends.eu.util

import android.content.Context
import androidx.core.content.ContextCompat
import baf.bierandfriends.eu.R

object RankHelper {

    fun getRankColor(context: Context, rank: String): Int {
        return when (rank.lowercase()) {
            "malzbier"       -> ContextCompat.getColor(context, R.color.rank_malzbier)
            "feierabendbier" -> ContextCompat.getColor(context, R.color.rank_feierabendbier)
            "vollwieneimer"  -> ContextCompat.getColor(context, R.color.rank_vollwieneimer)
            "absturzlegende" -> ContextCompat.getColor(context, R.color.rank_absturzlegende)
            "builder"        -> ContextCompat.getColor(context, R.color.rank_builder)
            "trainee"        -> ContextCompat.getColor(context, R.color.rank_trainee)
            "supporter"      -> ContextCompat.getColor(context, R.color.rank_supporter)
            "moderator"      -> ContextCompat.getColor(context, R.color.rank_moderator)
            "admin"          -> ContextCompat.getColor(context, R.color.rank_admin)
            "cheffe"         -> ContextCompat.getColor(context, R.color.rank_cheffe)
            else             -> ContextCompat.getColor(context, R.color.baf_text_secondary)
        }
    }

    fun getRankDisplayName(rank: String): String {
        return when (rank.lowercase()) {
            "malzbier"       -> "🍺 Malzbier"
            "feierabendbier" -> "🍻 Feierabendbier"
            "vollwieneimer"  -> "🪣 Vollwieneimer"
            "absturzlegende" -> "💀 Absturzlegende"
            "builder"        -> "🔨 Builder"
            "trainee"        -> "🌱 Trainee"
            "supporter"      -> "💬 Supporter"
            "moderator"      -> "🛡️ Moderator"
            "admin"          -> "⚡ Admin"
            "cheffe"         -> "👑 Cheffe"
            else             -> rank.replaceFirstChar { it.uppercase() }
        }
    }

    // Supporter, Moderator, Admin, Cheffe = Staff
    fun isStaff(rank: String): Boolean {
        return rank.lowercase() in listOf("trainee", "supporter", "moderator", "admin", "cheffe")
    }

    // Supporter bis Cheffe können Tickets schließen
    fun canCloseTickets(rank: String): Boolean {
        return rank.lowercase() in listOf("supporter", "moderator", "admin", "cheffe")
    }

    // Moderator bis Cheffe können Forumsbeiträge löschen
    fun canDeletePosts(rank: String): Boolean {
        return rank.lowercase() in listOf("moderator", "admin", "cheffe")
    }

    // Admin und Cheffe können Events erstellen
    fun canCreateEvents(rank: String): Boolean {
        return rank.lowercase() in listOf("admin", "cheffe")
    }

    // Admin und Cheffe
    fun isAdmin(rank: String): Boolean {
        return rank.lowercase() in listOf("admin", "cheffe")
    }
}
