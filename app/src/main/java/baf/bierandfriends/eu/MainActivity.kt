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
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import baf.bierandfriends.eu.databinding.ActivityMainBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging

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

        // Notification-Navigation erst nach 1 Sekunde
        val type = getNotificationType(intent)
        if (type != null) {
            Handler(Looper.getMainLooper()).postDelayed({
                handleNotificationNavigation(type)
            }, 1000)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val type = getNotificationType(intent) ?: return
        Handler(Looper.getMainLooper()).postDelayed({
            handleNotificationNavigation(type)
        }, 300)
    }

    private fun getNotificationType(intent: Intent?): String? {
        if (intent == null) return null
        return intent.getStringExtra("notification_type")
            ?: intent.getStringExtra("type")
    }

    private fun handleNotificationNavigation(type: String) {
        try {
            val navController = findNavController(R.id.nav_host_fragment)
            val uid = FirebaseAuth.getInstance().currentUser?.uid

            // Nicht navigieren wenn nicht eingeloggt
            if (uid == null) {
                Log.d(TAG, "Nicht eingeloggt – keine Navigation")
                return
            }

            // Direkt Bottom Nav Item aktivieren – sicherer als navigate()
            when (type) {
                "chat", "forum" -> {
                    binding.bottomNavigation.selectedItemId = R.id.communityFragment
                }
                "ticket" -> {
                    binding.bottomNavigation.selectedItemId = R.id.ticketsFragment
                }
                "event" -> {
                    binding.bottomNavigation.selectedItemId = R.id.eventsFragment
                }
                "market" -> {
                    binding.bottomNavigation.selectedItemId = R.id.marketFragment
                }
                else -> {
                    binding.bottomNavigation.selectedItemId = R.id.homeFragment
                }
            }
            Log.d(TAG, "Navigation zu: $type")
        } catch (e: Exception) {
            Log.e(TAG, "Navigation Fehler: ${e.message}")
            // Kein Crash – einfach auf Home bleiben
        }
    }

    override fun onResume() {
        super.onResume()
        if (FirebaseAuth.getInstance().currentUser?.uid != null) fetchAndSaveFcmToken()
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
