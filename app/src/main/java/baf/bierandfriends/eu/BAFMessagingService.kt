package baf.bierandfriends.eu

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class BAFMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID   = "baf_notifications"
        const val CHANNEL_NAME = "BierAndFriends"
        private const val TAG  = "BAFMessaging"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .set(mapOf("fcmToken" to token), SetOptions.merge())
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title     = message.notification?.title ?: message.data["title"] ?: "BierAndFriends"
        val body      = message.notification?.body  ?: message.data["body"]  ?: ""
        val type      = message.data["type"]     ?: ""
        val chatType  = message.data["chatType"] ?: "public"
        val senderUid = message.data["senderUid"] ?: ""

        Log.d(TAG, "Empfangen: type=$type chatType=$chatType")
        createNotificationChannel()
        showNotification(title, body, type, chatType, senderUid)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                mgr.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "BierAndFriends Benachrichtigungen"
                        enableVibration(true)
                        enableLights(true)
                    }
                )
            }
        }
    }

    private fun showNotification(
        title: String,
        body: String,
        type: String,
        chatType: String,
        senderUid: String
    ) {
        val notifMgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifId  = System.currentTimeMillis().toInt()

        // Tap-Intent: App öffnen und zum richtigen Screen navigieren
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notification_type", type)
            putExtra("type", type)
        }
        val openPI = PendingIntent.getActivity(
            this, notifId, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPI)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        // Direct-Reply-Action nur für Chat-Nachrichten
        if (type == "chat") {
            val remoteInput = RemoteInput.Builder(NotificationReplyReceiver.KEY_REPLY)
                .setLabel("Antworten…")
                .build()

            val replyIntent = Intent(this, NotificationReplyReceiver::class.java).apply {
                action = NotificationReplyReceiver.ACTION_REPLY
                putExtra(NotificationReplyReceiver.EXTRA_CHAT_TYPE,       chatType)
                putExtra(NotificationReplyReceiver.EXTRA_SENDER_UID,      senderUid)
                putExtra(NotificationReplyReceiver.EXTRA_NOTIFICATION_ID, notifId)
            }

            // FLAG_MUTABLE ist für RemoteInput auf API 31+ Pflicht
            val replyPiFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            else
                PendingIntent.FLAG_UPDATE_CURRENT

            val replyPI = PendingIntent.getBroadcast(this, notifId + 1, replyIntent, replyPiFlags)

            val replyAction = NotificationCompat.Action.Builder(
                R.mipmap.ic_launcher, "↩ Antworten", replyPI
            ).addRemoteInput(remoteInput).build()

            builder.addAction(replyAction)
        }

        notifMgr.notify(notifId, builder.build())
    }
}
