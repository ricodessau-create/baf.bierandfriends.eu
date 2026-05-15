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
    ) { granted -> if (granted) fetchAndSaveFcmToken() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        FirebaseApp.initializeApp(this)

        // Beide möglichen Keys prüfen
        pendingNotificationType = extractNotificationType(intent)
        Log.d(TAG, "Pending notification type: $pendingNotificationType")

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

            // Warten bis homeFragment geladen ist, dann navigieren
            if (destination.id == R.id.homeFragment) {
                pendingNotificationType?.let { type ->
                    pendingNotificationType = null
                    // 500ms warten damit NavController stabil ist
                    Handler(Looper.getMainLooper()).postDelayed({
                        navigateToNotification(navController, type)
                    }, 500)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val type = extractNotificationType(intent) ?: return
        Log.d(TAG, "onNewIntent type: $type")

        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val navController = findNavController(R.id.nav_host_fragment)
                val current = navController.currentDestination?.id
                if (current != null &&
                    current != R.id.loginFragment &&
                    current != R.id.registerFragment) {
                    navigateToNotification(navController, type)
                } else {
                    pendingNotificationType = type
                }
            } catch (e: Exception) {
                Log.e(TAG, "onNewIntent Fehler: ${e.message}")
            }
        }, 300)
    }

    private fun extractNotificationType(intent: Intent?): String? {
        if (intent == null) return null
        // FCM sendet data payload auf verschiedene Arten
        return intent.getStringExtra("notification_type")
            ?: intent.getStringExtra("type")
            ?: intent.extras?.getString("notification_type")
            ?: intent.extras?.getString("type")
    }

    private fun navigateToNotification(navController: NavController, type: String) {
        Log.d(TAG, "Navigiere zu Typ: $type")
        try {
            val destination = when (type) {
                "chat"   -> R.id.communityFragment
                "ticket" -> R.id.ticketsFragment
                "forum"  -> R.id.communityFragment
                "event"  -> R.id.eventsFragment
                "market" -> R.id.marketFragment
                "sync"   -> R.id.profileFragment
                else     -> null
            }
            destination?.let {
                // Erst zur Home navigieren falls nötig, dann zum Ziel
                val current = navController.currentDestination?.id
                if (current != R.id.homeFragment) {
                    navController.navigate(R.id.homeFragment)
                    Handler(Looper.getMainLooper()).postDelayed({
                        try { navController.navigate(it) } catch (e: Exception) { }
                    }, 300)
                } else {
                    navController.navigate(it)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Navigation Fehler: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        if (FirebaseAuth.getInstance().currentUser?.uid != null) fetchAndSaveFcmToken()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
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
