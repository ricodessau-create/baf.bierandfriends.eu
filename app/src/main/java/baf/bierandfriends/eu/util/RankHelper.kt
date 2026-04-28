package baf.bierandfriends.eu.util

import android.content.Context
import androidx.core.content.ContextCompat
import baf.bierandfriends.eu.R

object RankHelper {

    fun getRankColor(context: Context, rank: String): Int {
        return when (rank.lowercase()) {
            "malzbier" -> ContextCompat.getColor(context, R.color.rank_malzbier)
            "feierabendbier" -> ContextCompat.getColor(context, R.color.rank_feierabendbier)
            "vollwieneimer" -> ContextCompat.getColor(context, R.color.rank_vollwieneimer)
            "absturzlegende" -> ContextCompat.getColor(context, R.color.rank_absturzlegende)
            "builder" -> ContextCompat.getColor(context, R.color.rank_builder)
            "moderator" -> ContextCompat.getColor(context, R.color.rank_moderator)
            "supporter" -> ContextCompat.getColor(context, R.color.rank_supporter)
            "trainee" -> ContextCompat.getColor(context, R.color.rank_trainee)
            "admin" -> ContextCompat.getColor(context, R.color.rank_admin)
            "cheffe" -> ContextCompat.getColor(context, R.color.rank_cheffe)
            else -> ContextCompat.getColor(context, R.color.baf_text_secondary)
        }
    }

    fun getRankDisplayName(rank: String): String {
        return when (rank.lowercase()) {
            "malzbier" -> "🍺 Malzbier"
            "feierabendbier" -> "🍻 Feierabendbier"
            "vollwieneimer" -> "🪣 Vollwieneimer"
            "absturzlegende" -> "💀 Absturzlegende"
            "builder" -> "🔨 Builder"
            "moderator" -> "🛡️ Moderator"
            "supporter" -> "💬 Supporter"
            "trainee" -> "🌱 Trainee"
            "admin" -> "⚡ Admin"
            "cheffe" -> "👑 Cheffe"
            else -> rank.replaceFirstChar { it.uppercase() }
        }
    }

    fun isStaff(rank: String): Boolean {
        return rank.lowercase() in listOf("moderator", "supporter", "trainee", "admin", "cheffe")
    }

    fun isAdmin(rank: String): Boolean {
        return rank.lowercase() in listOf("admin", "cheffe")
    }
}
