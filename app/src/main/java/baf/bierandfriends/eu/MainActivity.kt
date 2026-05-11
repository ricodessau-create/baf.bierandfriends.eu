package baf.bierandfriends.eu

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
    ) { granted ->
        Log.d(TAG, "Notification Permission: $granted")
        if (granted) fetchAndSaveFcmToken()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        FirebaseApp.initializeApp(this)

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
        }
    }

    override fun onResume() {
        super.onResume()
        // Token bei jedem App-Start aktualisieren
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            fetchAndSaveFcmToken()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    fetchAndSaveFcmToken()
                }
                else -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            fetchAndSaveFcmToken()
        }
    }

    private fun fetchAndSaveFcmToken() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                Log.d(TAG, "FCM Token geholt: $token")
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .set(mapOf("fcmToken" to token), SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d(TAG, "✅ FCM Token in Firestore gespeichert")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "❌ Token speichern fehlgeschlagen: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ FCM Token holen fehlgeschlagen: ${e.message}")
            }
    }
}
