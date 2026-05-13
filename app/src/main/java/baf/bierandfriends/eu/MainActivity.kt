package baf.bierandfriends.eu

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
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
    private var pendingNotificationType: String? = null

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) fetchAndSaveFcmToken()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        FirebaseApp.initializeApp(this)

        // Notification-Typ aus Intent merken
        pendingNotificationType = intent?.getStringExtra("notification_type")

        requestNotificationPermission()

        val navController = findNavController(R.id.nav_host_fragment)
        binding.bottomNavigation.setupWithNavController(navController)

        val noBottomNav = setOf(
            R.id.loginFragment,
            R.id.registerFragment,
            R.id.profileFragment,
            R.id.newPostFragment,
            R.id.newTicketFragment,
            R.id.marketCreateFragment,
            R.id.marketDetailFragment,
            R.id.chatFragment,
            R.id.privateChatFragment,
            R.id.postDetailFragment,
            R.id.userProfileFragment,
            R.id.ticketDetailFragment
        )

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNavigation.visibility =
                if (destination.id in noBottomNav) View.GONE else View.VISIBLE

            // Erst navigieren wenn wir auf einem Haupt-Tab sind (nicht Login)
            if (destination.id == R.id.homeFragment) {
                pendingNotificationType?.let { type ->
                    pendingNotificationType = null
                    navigateToNotification(navController, type)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val type = intent.getStringExtra("notification_type") ?: return
        try {
            val navController = findNavController(R.id.nav_host_fragment)
            val current = navController.currentDestination?.id
            // Nur navigieren wenn User schon eingeloggt ist (nicht auf Login-Screen)
            if (current != null &&
                current != R.id.loginFragment &&
                current != R.id.registerFragment) {
                navigateToNotification(navController, type)
            } else {
                pendingNotificationType = type
            }
        } catch (e: Exception) {
            Log.e(TAG, "onNewIntent Navigation Fehler: ${e.message}")
        }
    }

    private fun navigateToNotification(navController: NavController, type: String) {
        Log.d(TAG, "Navigiere zu: $type")
        try {
            when (type) {
                "chat"   -> navController.navigate(R.id.communityFragment)
                "ticket" -> navController.navigate(R.id.ticketsFragment)
                "forum"  -> navController.navigate(R.id.communityFragment)
                "event"  -> navController.navigate(R.id.eventsFragment)
                "market" -> navController.navigate(R.id.marketFragment)
                "sync"   -> navController.navigate(R.id.profileFragment)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Navigation Fehler: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) fetchAndSaveFcmToken()
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
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .set(mapOf("fcmToken" to token), SetOptions.merge())
                    .addOnSuccessListener { Log.d(TAG, "✅ FCM Token gespeichert") }
                    .addOnFailureListener { e -> Log.e(TAG, "❌ Token Fehler: ${e.message}") }
            }
    }
}
