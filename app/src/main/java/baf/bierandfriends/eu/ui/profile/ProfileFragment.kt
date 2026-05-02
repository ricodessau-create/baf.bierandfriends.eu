package baf.bierandfriends.eu.ui.profile

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import baf.bierandfriends.eu.databinding.FragmentProfileBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Vollständiges ProfileFragment
 *
 * Hinweise zur Integration:
 * - Ersetze die TODOs mit deinen echten Repository/Supabase/Firebase-Aufrufen.
 * - Erwartetes Layout: fragment_profile.xml mit IDs wie in den Kommentaren.
 * - Dieses Fragment zeigt zunächst "Gast" bis echte Userdaten geladen sind.
 * - Rollen werden nur nach erfolgreichem Laden gesetzt; solange kein Sync-Token vorhanden ist,
 *   bleibt der Status unverändert (z. B. Malzbier erst nach erfolgreicher Synchronisation).
 *
 * Erwartete Layout-IDs:
 * profileAvatar, editProfileButton, syncButton, logoutButton, btnResetToken,
 * syncTokenText, syncTokenCard, profileUsername, profileEmail, profileRank,
 * profileBio, profileLocation, profileBirthday, profileDiscord, profileMinecraft, profileHK,
 * ticketsRecyclerView (falls Tickets angezeigt werden sollen)
 */

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    // TODO: Ersetze diese Platzhalter durch deine echten Implementierungen
    // private val userRepository = UserRepository.instance
    // private val authManager = AuthManager.instance
    // private val ticketRepository = TicketRepository.instance

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val uri: Uri? = data?.data
            if (uri != null) {
                lifecycleScope.launch { handleAvatarPicked(uri) }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Zeige vorläufige Gast-Ansicht, bis echte Daten geladen sind
        showGuestState()

        // Lade Userdaten asynchron
        lifecycleScope.launch { loadProfileAndTickets() }

        binding.editProfileButton.setOnClickListener { openEditProfile() }
        binding.profileAvatar.setOnClickListener { pickImageFromGallery() }
        binding.syncButton.setOnClickListener { lifecycleScope.launch { generateAndShowSyncToken() } }
        binding.btnResetToken.setOnClickListener { lifecycleScope.launch { resetSyncToken() } }
        binding.logoutButton.setOnClickListener { lifecycleScope.launch { performLogoutSafely() } }
    }

    private fun showGuestState() {
        binding.profileUsername.text = getStringSafe("Unbekannt")
        binding.profileRank.text = getStringSafe("Gast")
        binding.profileEmail.text = ""
        binding.syncTokenCard.isVisible = false
        // Tickets ausblenden oder Hinweis anzeigen
        // binding.ticketsRecyclerView.isVisible = false
    }

    private suspend fun loadProfileAndTickets() {
        // Lade Userdaten aus Repository / Auth
        val userData: Map<String, Any?>? = withContext(Dispatchers.IO) {
            try {
                // TODO: return@withContext userRepository.getCachedOrRemoteUser()
                // Beispiel-Rückgabe (Platzhalter)
                mapOf(
                    "username" to "Ricardo",
                    "email" to "ricardo@example.com",
                    "rank" to "malzbier", // interne Rolle
                    "bio" to "Prost!",
                    "location" to "Köln",
                    "birthday" to "1990-01-01",
                    "discord" to "ricardo#1234",
                    "minecraft" to "ricardo_mc",
                    "hk" to "42",
                    "syncToken" to null // falls bereits synchronisiert, hier String
                )
            } catch (e: Exception) {
                null
            }
        }

        if (userData == null) {
            // Bleibe im Gast-Modus, zeige Fehlerhinweis
            withContext(Dispatchers.Main) {
                binding.profileUsername.text = getStringSafe("Unbekannt")
                binding.profileRank.text = getStringSafe("Gast")
            }
            return
        }

        // Sichere Konvertierung aller Felder zu Strings
        val username = userData["username"]?.toString().orEmpty()
        val email = userData["email"]?.toString().orEmpty()
        val rankKey = userData["rank"]?.toString().orEmpty()
        val bio = userData["bio"]?.toString().orEmpty()
        val location = userData["location"]?.toString().orEmpty()
        val birthday = userData["birthday"]?.toString().orEmpty()
        val discord = userData["discord"]?.toString().orEmpty()
        val minecraft = userData["minecraft"]?.toString().orEmpty()
        val hk = userData["hk"]?.toString().orEmpty()
        val syncTokenAny = userData["syncToken"]

        // Mappe interne Rollen auf Anzeige-Strings
        val displayRank = mapRoleToDisplay(rankKey, syncTokenAny)

        withContext(Dispatchers.Main) {
            binding.profileUsername.text = username.ifEmpty { getStringSafe("Unbekannt") }
            binding.profileEmail.text = email
            binding.profileRank.text = displayRank
            binding.profileBio.text = bio
            binding.profileLocation.text = location
            binding.profileBirthday.text = birthday
            binding.profileDiscord.text = discord
            binding.profileMinecraft.text = minecraft
            binding.profileHK.text = hk

            // Wenn ein Sync-Token vorhanden ist, zeige ihn
            if (syncTokenAny is String && syncTokenAny.isNotBlank()) {
                binding.syncTokenText.text = syncTokenAny
                binding.syncTokenCard.isVisible = true
            } else {
                binding.syncTokenCard.isVisible = false
            }
        }

        // Lade Tickets nur wenn berechtigt / angemeldet
        lifecycleScope.launch {
            loadTicketsIfAllowed()
        }
    }

    private fun mapRoleToDisplay(roleKey: String, syncTokenAny: Any?): String {
        // Wenn noch kein Sync erfolgt ist, zeige Gast oder die lokale Rolle, aber ohne Rechte
        val hasSynced = syncTokenAny is String && syncTokenAny.isNotBlank()
        return when (roleKey.lowercase()) {
            "malzbier" -> if (hasSynced) "Malzbier" else "Gast"
            "feierabendbier" -> if (hasSynced) "Feierabendbier" else "Gast"
            "vollwieneimer" -> if (hasSynced) "Vollwieneimer" else "Gast"
            "absturzlegende" -> if (hasSynced) "Absturzlegende" else "Gast"
            "builder" -> if (hasSynced) "Builder" else "Gast"
            "moderator" -> if (hasSynced) "Moderator" else "Gast"
            "supporter" -> if (hasSynced) "Supporter" else "Gast"
            "trainee" -> if (hasSynced) "Trainee" else "Gast"
            "admin" -> if (hasSynced) "Admin" else "Gast"
            "cheffe" -> if (hasSynced) "Cheffe" else "Gast"
            else -> if (hasSynced && roleKey.isNotBlank()) roleKey.replaceFirstChar { it.uppercase() } else "Gast"
        }
    }

    private suspend fun loadTicketsIfAllowed() {
        // Prüfe Berechtigung anhand Rolle oder Auth-Status
        val allowed = withContext(Dispatchers.IO) {
            try {
                // TODO: return@withContext authManager.isLoggedIn() && authManager.hasTicketAccess()
                true // Platzhalter: falls du echte Prüfung hast, ersetze hier
            } catch (e: Exception) {
                false
            }
        }

        withContext(Dispatchers.Main) {
            if (!allowed) {
                // Tickets nicht anzeigen, Hinweis setzen
                // binding.ticketsRecyclerView.isVisible = false
                // binding.ticketsEmptyText.isVisible = true
            } else {
                // Lade Tickets aus Repository und zeige sie
                // val tickets = withContext(Dispatchers.IO) { ticketRepository.getUserTickets() }
                // adapter.submitList(tickets)
                // binding.ticketsRecyclerView.isVisible = true
            }
        }
    }

    private fun openEditProfile() {
        // TODO: Navigation zur Edit-Seite
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private suspend fun handleAvatarPicked(uri: Uri) {
        val bitmap: Bitmap? = withContext(Dispatchers.IO) {
            try {
                MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
            } catch (e: Exception) {
                null
            }
        }

        if (bitmap != null) {
            withContext(Dispatchers.IO) {
                try {
                    // TODO: userRepository.uploadAvatar(bitmapToByteArray(bitmap))
                } catch (_: Exception) {
                }
            }
        }
    }

    private suspend fun generateAndShowSyncToken() {
        binding.syncTokenCard.isVisible = false
        binding.syncTokenText.text = ""

        val tokenResult: String? = withContext(Dispatchers.IO) {
            try {
                // TODO: return@withContext userRepository.generateSyncToken()
                "demo-sync-token-1234" // Platzhalter
            } catch (e: Exception) {
                null
            }
        }

        withContext(Dispatchers.Main) {
            if (!tokenResult.isNullOrBlank()) {
                binding.syncTokenText.text = tokenResult
                binding.syncTokenCard.isVisible = true
                // Optional: nach Token-Generierung Rolle aktualisieren
                // lifecycleScope.launch { loadProfileAndTickets() }
            } else {
                binding.syncTokenText.text = getStringSafe("Fehler beim Erzeugen des Tokens")
                binding.syncTokenCard.isVisible = true
            }
        }
    }

    private suspend fun resetSyncToken() {
        val displayedToken = try { binding.syncTokenText.text?.toString() } catch (e: Exception) { null }
        val tokenString = displayedToken?.takeIf { it.isNotBlank() }

        if (tokenString.isNullOrBlank()) {
            withContext(Dispatchers.Main) {
                binding.syncTokenText.text = getStringSafe("Kein Token vorhanden")
            }
            return
        }

        val success = withContext(Dispatchers.IO) {
            try {
                // TODO: return@withContext SupabaseHelper.resetToken(tokenString)
                true // Platzhalter
            } catch (e: Exception) {
                false
            }
        }

        withContext(Dispatchers.Main) {
            if (success) {
                binding.syncTokenText.text = getStringSafe("Token zurückgesetzt")
                binding.syncTokenCard.isVisible = false
                // Rolle neu laden, Tickets neu laden
                lifecycleScope.launch { loadProfileAndTickets() }
            } else {
                binding.syncTokenText.text = getStringSafe("Fehler beim Zurücksetzen")
                binding.syncTokenCard.isVisible = true
            }
        }
    }

    private suspend fun performLogoutSafely() {
        // Führe Logout im Hintergrund aus, aber navigiere erst, wenn sauber abgeschlossen
        val ok = withContext(Dispatchers.IO) {
            try {
                // TODO: authManager.signOut()
                true
            } catch (e: Exception) {
                false
            }
        }

        withContext(Dispatchers.Main) {
            if (ok) {
                // Navigation zur Login-Seite oder Start-Activity
                // Navigation.findNavController(requireView()).navigate(R.id.action_profile_to_login)
            } else {
                // Zeige Fehler, verhindere inkonsistenten Zustand
                // z.B. Snackbar: "Abmelden fehlgeschlagen"
            }
        }
    }

    private fun getStringSafe(value: String): String = value

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
