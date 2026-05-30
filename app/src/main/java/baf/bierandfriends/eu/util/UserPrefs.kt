package baf.bierandfriends.eu.util

import android.content.Context

/**
 * Speichert und liest grundlegende User-Daten aus SharedPreferences.
 * Wird benötigt, damit der NotificationReplyReceiver (BroadcastReceiver)
 * die eigene UID, den Nutzernamen und den Rang kennt – ohne Firestore-Abfrage.
 */
object UserPrefs {

    private const val PREFS_NAME   = "baf_user_prefs"
    private const val KEY_UID      = "uid"
    private const val KEY_USERNAME = "username"
    private const val KEY_RANK     = "rank"

    fun save(context: Context, uid: String, username: String, rank: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_UID,      uid)
            .putString(KEY_USERNAME, username)
            .putString(KEY_RANK,     rank)
            .apply()
    }

    fun getUid(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_UID, "") ?: ""

    fun getUsername(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USERNAME, "Unbekannt") ?: "Unbekannt"

    fun getRank(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_RANK, "malzbier") ?: "malzbier"
}
