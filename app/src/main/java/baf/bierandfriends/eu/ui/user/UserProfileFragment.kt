package baf.bierandfriends.eu.ui.profile

import android.app.AlertDialog
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
import baf.bierandfriends.eu.data.models.UserProfile
import baf.bierandfriends.eu.data.repository.UserRepository
import baf.bierandfriends.eu.databinding.FragmentProfileBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val userRepository = UserRepository()

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data: Intent? = result.data
            val uri: Uri? = data?.data
            if (uri != null) lifecycleScope.launch { handleAvatarPicked(uri) }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showGuestState()
        lifecycleScope.launch { loadProfileAndTickets() }
        binding.editProfileButton.setOnClickListener { openEditProfileIfAllowed() }
        binding.profileAvatar.setOnClickListener { pickImageFromGallery() }
        binding.syncButton.setOnClickListener { lifecycleScope.launch { onGenerateTokenClicked() } }
        binding.btnResetToken.setOnClickListener { lifecycleScope.launch { onResetTokenClicked() } }
        binding.logoutButton.setOnClickListener { confirmAndLogout() }
    }

    private fun showGuestState() {
        binding.profileUsername.text = "Gast"
        binding.profileRank.text = "Gast"
        binding.profileEmail.text = ""
        binding.syncTokenCard.isVisible = false
        setActionAvailability(false)
    }

    private suspend fun loadProfileAndTickets() {
        val profile = withContext(Dispatchers.IO) { userRepository.getUserProfile() }
        if (profile == null) {
            withContext(Dispatchers.Main) { showGuestState() }
            return
        }
        withContext(Dispatchers.Main) { applyProfileToUi(profile) }
        val hasSync = !profile.syncToken.isNullOrBlank()
        lifecycleScope.launch { loadTicketsIfAllowed(hasSync) }
    }

    private fun applyProfileToUi(profile: UserProfile) {
        val username = profile.username.ifBlank { "" }
        val displayName = if (username.isBlank()) "Gast" else username
        val hasSync = !profile.syncToken.isNullOrBlank()
        val displayRank = mapRoleToDisplay(profile.rank, profile.syncToken)
        binding.profileUsername.text = displayName
        binding.profileEmail.text = profile.email
        binding.profileRank.text = displayRank
        binding.profileBio.text = profile.bio
        binding.profileLocation.text = profile.location
        binding.profileBirthday.text = profile.birthday
        binding.profileDiscord.text = profile.discord
        binding.profileMinecraft.text = profile.minecraftName
        binding.profileHK.text = profile.hopfenkaltschalen.toString()
        if (hasSync) {
            binding.syncTokenText.text = profile.syncToken
            binding.syncTokenCard.isVisible = true
        } else {
            binding.syncTokenCard.isVisible = false
        }
        setActionAvailability(hasSync)
    }

    private fun mapRoleToDisplay(roleKey: String, syncToken: String?): String {
        val hasSynced = !syncToken.isNullOrBlank()
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
        withContext(Dispatchers.Main) {
            if (!hasSync) {
                // keine Tickets anzeigen
            } else {
                // Tickets laden wenn implementiert
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
            try { MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri) } catch (e: Exception) { null }
        }
        if (bitmap != null) {
            // upload via userRepository.uploadAvatar wenn implementiert
        }
    }

    private suspend fun onGenerateTokenClicked() {
        binding.syncTokenCard.isVisible = false
        binding.syncTokenText.text = ""
        val token = withContext(Dispatchers.IO) { userRepository.generateSyncToken() }
        withContext(Dispatchers.Main) {
            if (!token.isNullOrBlank()) {
                binding.syncTokenText.text = token
                binding.syncTokenCard.isVisible = true
                lifecycleScope.launch { loadProfileAndTickets() }
            } else {
                binding.syncTokenText.text = "Fehler beim Erzeugen des Tokens"
                binding.syncTokenCard.isVisible = true
            }
        }
    }

    private suspend fun onResetTokenClicked() {
        val displayed = try { binding.syncTokenText.text?.toString() } catch (e: Exception) { null }
        val token = displayed?.takeIf { it.isNotBlank() } ?: run {
            withContext(Dispatchers.Main) { binding.syncTokenText.text = "Kein Token vorhanden" }
            return
        }
        val success = withContext(Dispatchers.IO) { userRepository.resetSyncToken(token) }
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
            .setPositiveButton("Abmelden") { _, _ -> lifecycleScope.launch { performLogout() } }
            .show()
    }

    private suspend fun performLogout() {
        withContext(Dispatchers.IO) {
            try { com.google.firebase.auth.FirebaseAuth.getInstance().signOut() } catch (_: Exception) { }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
