package baf.bierandfriends.eu.ui.profile

import android.app.AlertDialog
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
 * Robuste ProfileFragment-Version
 *
 * - Zeigt lokal gespeicherte Rolle (z. B. Malzbier) an, markiert aber fehlende Sync-Rechte.
 * - Deaktiviert kritische Aktionen, wenn keine Berechtigung vorhanden.
 * - Zeigt Tickets aus lokalem Cache, falls vorhanden.
 * - Logout mit Bestätigung, verhindert inkonsistente Zustände.
 *
 * TODO: Ersetze die TODO-Abschnitte mit deinen echten Repositories/Managern.
 */

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    // TODO: Ersetze durch deine Implementierungen
    // private val userRepository = UserRepository.instance
    // private val authManager = AuthManager.instance
    // private val ticketRepository = TicketRepository.instance
    // private val localCache = LocalCache.instance

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val uri: Uri? = data?.data
            if (uri != null) lifecycleScope.launch { handleAvatarPicked(uri) }
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

        // Zeige sofort lokale/platzhalter Daten, damit UI nicht "leer" ist
        showLocalCachedProfile()

        // Lade remote Daten asynchron, aber überschreibe nicht sofort kritische UI ohne Prüfung
        lifecycleScope.launch { loadProfileSafely() }

        binding.editProfileButton.setOnClickListener { openEditProfileIfAllowed() }
        binding.profileAvatar.setOnClickListener { pickImageFromGallery() }
        binding.syncButton.setOnClickListener { lifecycleScope.launch { generateAndShowSyncToken() } }
        binding.btnResetToken.setOnClickListener { lifecycleScope.launch { resetSyncToken() } }
        binding.logoutButton.setOnClickListener { confirmAndLogout() }
    }

    private fun showLocalCachedProfile() {
        // Versuche lokale Werte aus Cache zu lesen (Platzhalter)
        val cached = mapOf<String, Any?>(
            "username" to "Ricardo",
            "email" to "ricardo@example.com",
            "rank" to "malzbier",
            "syncToken" to null
        )

        val username = cached["username"]?.toString().orEmpty()
        val email = cached["email"]?.toString().orEmpty()
        val rankKey = cached["rank"]?.toString().orEmpty()
        val syncTokenAny = cached["syncToken"]

        binding.profileUsername.text = username.ifEmpty { "Unbekannt" }
        binding.profileEmail.text = email
        binding.profileRank.text = mapRoleToDisplay(rankKey, syncTokenAny)

        // Buttons: wenn kein Sync, Aktionen einschränken (nicht entfernen)
        val hasSync = syncTokenAny is String && syncTokenAny.isNotBlank()
        setActionAvailability(hasSync)
    }

    private suspend fun loadProfileSafely() {
        val remote: Map<String, Any?>? = withContext(Dispatchers.IO) {
            try {
                // TODO: return@withContext userRepository.getUserProfileRemoteOrCached()
                // Beispiel: remote data (simulate)
                mapOf(
                    "username" to "Ricardo",
                    "email" to "ricardo@example.com",
                    "rank" to "malzbier",
                    "bio" to "Prost!",
                    "location" to "Köln",
                    "birthday" to "1990-01-01",
                    "discord" to "ricardo#1234",
                    "minecraft" to "ricardo_mc",
                    "hk" to "42",
                    "syncToken" to null // oder "token-xyz" wenn synchronisiert
                )
            } catch (e: Exception) {
                null
            }
        }

        if (remote == null) {
            // Keine Remote-Daten: belasse lokale Ansicht, zeige Hinweis
            withContext(Dispatchers.Main) {
                // Optional: Snackbar/Toast anzeigen
            }
            return
        }

        val username = remote["username"]?.toString().orEmpty()
        val email = remote["email"]?.toString().orEmpty()
        val rankKey = remote["rank"]?.toString().orEmpty()
        val bio = remote["bio"]?.toString().orEmpty()
        val location = remote["location"]?.toString().orEmpty()
        val birthday = remote["birthday"]?.toString().orEmpty()
        val discord = remote["discord"]?.toString().orEmpty()
        val minecraft = remote["minecraft"]?.toString().orEmpty()
        val hk = remote["hk"]?.toString().orEmpty()
        val syncTokenAny = remote["syncToken"]

        val displayRank = mapRoleToDisplay(rankKey, syncTokenAny)
        val hasSync = syncTokenAny is String && syncTokenAny.isNotBlank()

        withContext(Dispatchers.Main) {
            binding.profileUsername.text = username.ifEmpty { "Unbekannt" }
            binding.profileEmail.text = email
            binding.profileRank.text = displayRank
            binding.profileBio.text = bio
            binding.profileLocation.text = location
            binding.profileBirthday.text = birthday
            binding.profileDiscord.text = discord
            binding.profileMinecraft.text = minecraft
            binding.profileHK.text = hk

            if (hasSync) {
                binding.syncTokenText.text = syncTokenAny as String
                binding.syncTokenCard.isVisible = true
            } else {
                binding.syncTokenCard.isVisible = false
            }

            setActionAvailability(hasSync)
        }

        // Tickets laden (nur wenn Berechtigung)
        lifecycleScope.launch { loadTicketsIfAllowed(hasSync) }
    }

    private fun mapRoleToDisplay(roleKey: String, syncTokenAny: Any?): String {
        val hasSynced = syncTokenAny is String && syncTokenAny.isNotBlank()
        // Wenn lokal Rolle vorhanden, zeige sie, aber wenn nicht synchronisiert -> markiere eingeschränkt
        val base = when (roleKey.lowercase()) {
            "malzbier" -> "Malzbier"
            "feierabendbier" -> "Feierabendbier"
            "vollwieneimer" -> "Vollwieneimer"
            "absturzlegende" -> "Absturzlegende"
            "builder" -> "Builder"
            "moderator" -> "Moderator"
            "supporter" -> "Supporter"
            "trainee" -> "Trainee"
            "admin" -> "Admin"
            "cheffe" -> "Cheffe"
            else -> roleKey.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }.ifEmpty { "Gast" }
        }
        return if (hasSynced) base else "$base (eingeschränkt)"
    }

    private fun setActionAvailability(hasSync: Boolean) {
        // Wenn nicht synchronisiert, Aktionen deaktivieren, aber nicht entfernen
        binding.editProfileButton.isEnabled = hasSync
        binding.syncButton.isEnabled = true // Sync immer erlaubt
        binding.btnResetToken.isEnabled = hasSync
        // Tickets / Markt / Angebote Buttons sollten ebenfalls geprüft werden (TODO)
    }

    private suspend fun loadTicketsIfAllowed(hasSync: Boolean) {
        val allowed = withContext(Dispatchers.IO) {
            try {
                // TODO: Prüfe echte Berechtigung: authManager.isLoggedIn() && authManager.hasTicketAccess()
                hasSync // Platzhalter: nur wenn synchronisiert
            } catch (e: Exception) {
                false
            }
        }

        withContext(Dispatchers.Main) {
            if (!allowed) {
                // Zeige leere Ticketsicht mit Hinweis
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

    private fun openEditProfileIfAllowed() {
        if (!binding.editProfileButton.isEnabled) {
            // Optional: Snackbar "Bitte zuerst synchronisieren"
            return
        }
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
                    // TODO: userRepository.uploadAvatar(...)
                } catch (_: Exception) { }
            }
        }
    }

    private suspend fun generateAndShowSyncToken() {
        binding.syncTokenCard.isVisible = false
        binding.syncTokenText.text = ""

        val tokenResult: String? = withContext(Dispatchers.IO) {
            try {
                // TODO: return@withContext userRepository.generateSyncToken()
                "demo-sync-token-1234"
            } catch (e: Exception) {
                null
            }
        }

        withContext(Dispatchers.Main) {
            if (!tokenResult.isNullOrBlank()) {
                binding.syncTokenText.text = tokenResult
                binding.syncTokenCard.isVisible = true
                // Nach erfolgreichem Token ggf. Profil neu laden
                lifecycleScope.launch { loadProfileSafely() }
            } else {
                binding.syncTokenText.text = getStringSafe("Fehler beim Erzeugen des Tokens")
                binding.syncTokenCard.isVisible = true
            }
        }
    }

    private suspend fun resetSyncToken() {
        val displayed = try { binding.syncTokenText.text?.toString() } catch (e: Exception) { null }
        val tokenString = displayed?.takeIf { it.isNotBlank() }
        if (tokenString.isNullOrBlank()) {
            withContext(Dispatchers.Main) { binding.syncTokenText.text = getStringSafe("Kein Token vorhanden") }
            return
        }

        val success = withContext(Dispatchers.IO) {
            try {
                // TODO: return@withContext SupabaseHelper.resetToken(tokenString)
                true
            } catch (e: Exception) {
                false
            }
        }

        withContext(Dispatchers.Main) {
            if (success) {
                binding.syncTokenText.text = getStringSafe("Token zurückgesetzt")
                binding.syncTokenCard.isVisible = false
                lifecycleScope.launch { loadProfileSafely() }
            } else {
                binding.syncTokenText.text = getStringSafe("Fehler beim Zurücksetzen")
                binding.syncTokenCard.isVisible = true
            }
        }
    }

    private fun confirmAndLogout() {
        AlertDialog.Builder(requireContext())
            .setTitle("Abmelden")
            .setMessage("Möchtest du dich wirklich abmelden?")
            .setNegativeButton("Abbrechen", null)
            .setPositiveButton("Abmelden") { _, _ ->
                lifecycleScope.launch { performLogoutSafely() }
            }
            .show()
    }

    private suspend fun performLogoutSafely() {
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
                // Navigation zur Login-Seite
                // Navigation.findNavController(requireView()).navigate(R.id.action_profile_to_login)
            } else {
                // Optional: Snackbar "Abmelden fehlgeschlagen"
            }
        }
    }

    private fun getStringSafe(value: String): String = value

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
