package baf.bierandfriends.eu

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import baf.bierandfriends.eu.databinding.ActivityMainBinding
import baf.bierandfriends.eu.util.UserPrefs
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val TAG = "MainActivity"

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) fetchAndSaveFcmToken() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        FirebaseApp.initializeApp(this)

        requestNotificationPermission()

        val navController = findNavController(R.id.nav_host_fragment)
        binding.bottomNavigation.setupWithNavController(navController)

        val noBottomNav = setOf(
            R.id.loginFragment, R.id.registerFragment, R.id.profileFragment,
            R.id.newPostFragment, R.id.newTicketFragment, R.id.marketCreateFragment,
            R.id.marketDetailFragment, R.id.chatFragment, R.id.privateChatFragment,
            R.id.postDetailFragment, R.id.userProfileFragment, R.id.ticketDetailFragment
        )

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNavigation.visibility =
                if (destination.id in noBottomNav) View.GONE else View.VISIBLE
        }

        if (intent?.data?.toString() == "baf://app/delete-account") {
            Handler(Looper.getMainLooper()).postDelayed({
                try { findNavController(R.id.nav_host_fragment).navigate(R.id.profileFragment) }
                catch (e: Exception) { Log.e(TAG, "DeepLink Navigation: ${e.message}") }
            }, 1000)
            return
        }

        val payload = getNotificationPayload(intent)
        if (payload != null) {
            Handler(Looper.getMainLooper()).postDelayed({
                handleNotificationNavigation(payload)
            }, 1000)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (intent.data?.toString() == "baf://app/delete-account") {
            Handler(Looper.getMainLooper()).postDelayed({
                try { findNavController(R.id.nav_host_fragment).navigate(R.id.profileFragment) }
                catch (e: Exception) { Log.e(TAG, "DeepLink Navigation: ${e.message}") }
            }, 300)
            return
        }

        val payload = getNotificationPayload(intent) ?: return
        Handler(Looper.getMainLooper()).postDelayed({
            handleNotificationNavigation(payload)
        }, 300)
    }

    override fun onResume() {
        super.onResume()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        fetchAndSaveFcmToken()
        saveUserPrefsFromFirestore(uid)
    }

    private fun saveUserPrefsFromFirestore(uid: String) {
        lifecycleScope.launch {
            try {
                val doc      = FirebaseFirestore.getInstance()
                    .collection("users").document(uid).get().await()
                val username = doc.getString("username") ?: return@launch
                val rank     = doc.getString("rank")     ?: "malzbier"
                UserPrefs.save(this@MainActivity, uid, username, rank)
                Log.d(TAG, "UserPrefs gespeichert: $username / $rank")
            } catch (_: Exception) {}
        }
    }

    private data class NotificationPayload(
        val type: String,
        val chatType: String,
        val senderUid: String,
        val senderName: String
    )

    private fun getNotificationPayload(intent: Intent?): NotificationPayload? {
        if (intent == null) return null
        val type = intent.getStringExtra("notification_type")
            ?: intent.getStringExtra("type")
            ?: return null
        return NotificationPayload(
            type       = type,
            chatType   = intent.getStringExtra("chatType") ?: "public",
            senderUid  = intent.getStringExtra("senderUid") ?: "",
            senderName = intent.getStringExtra("senderName") ?: "Nutzer"
        )
    }

    private fun handleNotificationNavigation(payload: NotificationPayload) {
        try {
            val navController = findNavController(R.id.nav_host_fragment)
            if (FirebaseAuth.getInstance().currentUser?.uid == null) return

            when (payload.type) {
                "chat" -> {
                    if (payload.chatType == "private" && payload.senderUid.isNotEmpty()) {
                        try {
                            navController.navigate(
                                R.id.privateChatFragment,
                                bundleOf(
                                    "receiverUid"  to payload.senderUid,
                                    "receiverName" to payload.senderName
                                )
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Private Chat Navigation: ${e.message}")
                        }
                    } else {
                        binding.bottomNavigation.selectedItemId = R.id.communityFragment
                        Handler(Looper.getMainLooper()).postDelayed({
                            try {
                                if (navController.currentDestination?.id == R.id.communityFragment) {
                                    navController.navigate(R.id.action_communityFragment_to_chatFragment)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Chat Navigation: ${e.message}")
                            }
                        }, 700)
                    }
                }
                "ticket" -> binding.bottomNavigation.selectedItemId = R.id.ticketsFragment
                "forum"  -> binding.bottomNavigation.selectedItemId = R.id.communityFragment
                "event"  -> binding.bottomNavigation.selectedItemId = R.id.eventsFragment
                "market" -> binding.bottomNavigation.selectedItemId = R.id.marketFragment
                "sync"   -> {
                    try { findNavController(R.id.nav_host_fragment).navigate(R.id.profileFragment) }
                    catch (e: Exception) { Log.e(TAG, "Sync Navigation: ${e.message}") }
                }
                else -> binding.bottomNavigation.selectedItemId = R.id.homeFragment
            }
        } catch (e: Exception) {
            Log.e(TAG, "Navigation Fehler: ${e.message}")
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                fetchAndSaveFcmToken()
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            fetchAndSaveFcmToken()
        }
    }

    private fun fetchAndSaveFcmToken() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .set(mapOf("fcmToken" to token), SetOptions.merge())
                .addOnSuccessListener { Log.d(TAG, "✅ FCM Token gespeichert") }
        }
    }
}
