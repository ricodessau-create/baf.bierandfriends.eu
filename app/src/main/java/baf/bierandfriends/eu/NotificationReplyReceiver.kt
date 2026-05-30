package baf.bierandfriends.eu

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import baf.bierandfriends.eu.util.UserPrefs
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Empfängt Direct-Reply-Antworten aus Benachrichtigungen und sendet
 * die Nachricht direkt an Firestore (public_chat oder private_chats).
 */
class NotificationReplyReceiver : BroadcastReceiver() {

    companion object {
        const val KEY_REPLY             = "key_reply_text"
        const val EXTRA_CHAT_TYPE       = "chat_type"
        const val EXTRA_SENDER_UID      = "sender_uid"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val ACTION_REPLY          = "baf.bierandfriends.eu.REPLY_ACTION"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        // Reply-Text aus dem RemoteInput auslesen
        val replyText = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(KEY_REPLY)?.toString()?.trim()

        if (replyText.isNullOrEmpty()) {
            pendingResult.finish()
            return
        }

        val chatType  = intent.getStringExtra(EXTRA_CHAT_TYPE)       ?: "public"
        val senderUid = intent.getStringExtra(EXTRA_SENDER_UID)       ?: ""
        val notifId   = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)

        // Eigene User-Daten aus SharedPreferences (wurden beim Chat-Öffnen gespeichert)
        val username = UserPrefs.getUsername(context)
        val rank     = UserPrefs.getRank(context)
        val myUid    = UserPrefs.getUid(context).ifEmpty {
            FirebaseAuth.getInstance().currentUser?.uid ?: run {
                pendingResult.finish()
                return
            }
        }

        val db = FirebaseFirestore.getInstance()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (chatType == "private" && senderUid.isNotEmpty()) {
                    // Privat-Nachricht zurück an den Absender
                    val chatId = if (myUid < senderUid) "${myUid}_${senderUid}"
                                 else "${senderUid}_${myUid}"
                    db.collection("private_chats")
                        .document(chatId)
                        .collection("messages")
                        .add(
                            mapOf(
                                "text"        to replyText,
                                "senderUid"   to myUid,
                                "senderName"  to username,
                                "receiverUid" to senderUid,
                                "createdAt"   to Timestamp.now(),
                                "id"          to ""
                            )
                        )
                } else {
                    // Öffentlichen Chat-Beitrag senden
                    db.collection("public_chat")
                        .add(
                            mapOf(
                                "text"       to replyText,
                                "authorUid"  to myUid,
                                "authorName" to username,
                                "authorRank" to rank,
                                "createdAt"  to Timestamp.now(),
                                "id"         to "",
                                "photoUrl"   to ""
                            )
                        )
                }

                // Notification auf "Gesendet" aktualisieren
                if (notifId != 0) {
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val sentNotif = NotificationCompat.Builder(context, BAFMessagingService.CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentText("✓ Antwort gesendet")
                        .setAutoCancel(true)
                        .build()
                    nm.notify(notifId, sentNotif)
                }

            } catch (_: Exception) {
                // Stille Fehlerbehandlung – Nutzer merkt nichts Schlimmes
            } finally {
                pendingResult.finish()
            }
        }
    }
}
