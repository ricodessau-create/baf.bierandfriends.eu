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

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

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
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showGuestState()
        lifecycleScope.launch { loadProfileAndTickets() }
        binding.editProfileButton.setOnClickListener { openEditProfileIfAllowed() }
        binding.profileAvatar.setOnClickListener { pickImageFromGallery() }
        binding.syncButton.setOnClickListener { lifecycleScope.launch { generateAndShowSyncToken() } }
        binding.btnResetToken.setOnClickListener { lifecycleScope.launch { resetSyncToken() } }
        binding.logoutButton.setOnClickListener { confirmAndLogout() }
    }

    private fun showGuestState() {
        binding.profileUsername.text = "Gast"
        binding.profileRank.text = "Gast"
        binding.profileEmail.text = ""
        binding.syncTokenCard.isVisible = false
    }

    private suspend fun loadProfileAndTickets() {
        val userData: Map<String, Any?>? = withContext(Dispatchers.IO) {
            try {
                mapOf(
                    "username" to null,
                    "email" to null,
                    "rank" to "malzbier",
                    "bio" to "",
                    "location" to "",
                    "birthday" to "",
                    "discord" to "",
                    "minecraft" to "",
                    "hk" to "",
                    "syncToken" to null
                )
            } catch (e: Exception) {
                null
            }
        }

        if (userData == null) {
            withContext(Dispatchers.Main) {
                showGuestState()
            }
            return
        }

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

        val displayRank = mapRoleToDisplay(rankKey, syncTokenAny)
        val hasSync = syncTokenAny is String && syncTokenAny.isNotBlank()

        withContext(Dispatchers.Main) {
            binding.profileUsername.text = if (username.isBlank()) "Gast" else username
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

        lifecycleScope.launch { loadTicketsIfAllowed(hasSync) }
    }

    private fun mapRoleToDisplay(roleKey: String, syncTokenAny: Any?): String {
        val hasSynced = syncTokenAny is String && syncTokenAny.isNotBlank()
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
            else -> if (roleKey.isBlank()) "Gast" else roleKey.replaceFirstChar { it.uppercase() }
        }
        return if (hasSynced) base else "$base (eingeschränkt)"
    }

    private fun setActionAvailability(hasSync: Boolean) {
        binding.editProfileButton.isEnabled = hasSync
        binding.syncButton.isEnabled = true
        binding.btnResetToken.isEnabled = hasSync
    }

    private suspend fun loadTicketsIfAllowed(hasSync: Boolean) {
        val allowed = withContext(Dispatchers.IO) {
            try {
                hasSync
            } catch (e: Exception) {
                false
            }
        }

        withContext(Dispatchers.Main) {
            if (!allowed) {
                // no tickets shown
            } else {
                // load and show tickets if implemented
            }
        }
    }

    private fun openEditProfileIfAllowed() {
        if (!binding.editProfileButton.isEnabled) return
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
                    // upload if implemented
                } catch (_: Exception) { }
            }
        }
    }

    private suspend fun generateAndShowSyncToken() {
        binding.syncTokenCard.isVisible = false
        binding.syncTokenText.text = ""
        val tokenResult: String? = withContext(Dispatchers.IO) {
            try {
                "demo-sync-token-1234"
            } catch (e: Exception) {
                null
            }
        }
        withContext(Dispatchers.Main) {
            if (!tokenResult.isNullOrBlank()) {
                binding.syncTokenText.text = tokenResult
                binding.syncTokenCard.isVisible = true
                lifecycleScope.launch { loadProfileAndTickets() }
            } else {
                binding.syncTokenText.text = "Fehler beim Erzeugen des Tokens"
                binding.syncTokenCard.isVisible = true
            }
        }
    }

    private suspend fun resetSyncToken() {
        val displayed = try { binding.syncTokenText.text?.toString() } catch (e: Exception) { null }
        val tokenString = displayed?.takeIf { it.isNotBlank() }
        if (tokenString.isNullOrBlank()) {
            withContext(Dispatchers.Main) { binding.syncTokenText.text = "Kein Token vorhanden" }
            return
        }
        val success = withContext(Dispatchers.IO) {
            try {
                true
            } catch (e: Exception) {
                false
            }
        }
        withContext(Dispatchers.Main) {
            if (success) {
                binding.syncTokenText.text = "Token zurückgesetzt"
                binding.syncTokenCard.isVisible = false
                lifecycleScope.launch { loadProfileAndTickets() }
            } else {
                binding.syncTokenText.text = "Fehler beim Zurücksetzen"
                binding.syncTokenCard.isVisible = true
            }
        }
    }

    private fun confirmAndLogout() {
        AlertDialog.Builder(requireContext())
            .setTitle("Abmelden")
            .setMessage("Möchtest du dich wirklich abmelden?")
            .setNegativeButton("Abbrechen", null)
            .setPositiveButton("Abmelden") { _, _ -> lifecycleScope.launch { performLogoutSafely() } }
            .show()
    }

    private suspend fun performLogoutSafely() {
        val ok = withContext(Dispatchers.IO) {
            try {
                true
            } catch (e: Exception) {
                false
            }
        }
        withContext(Dispatchers.Main) {
            if (ok) {
                // navigate to login if implemented
            } else {
                // show failure if implemented
            }
        }
    }

    private fun getStringSafe(value: String): String = value

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
