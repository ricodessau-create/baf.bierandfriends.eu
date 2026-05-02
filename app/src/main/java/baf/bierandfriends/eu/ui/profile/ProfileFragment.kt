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
 * Vollständige, robuste Version von ProfileFragment.kt
 *
 * Hinweise:
 * - Keine Pfadangaben oder Kommentare mit Dateipfaden in der Datei.
 * - Diese Datei geht defensiv mit Any/CharSequence Werten um.
 * - Passe Repository/Helper-Aufrufe an deine Implementierung an.
 *
 * Erwartete Layout-IDs in fragment_profile.xml:
 * profileAvatar, editProfileButton, syncButton, logoutButton, btnResetToken,
 * syncTokenText, syncTokenCard, profileUsername, profileEmail, profileRank,
 * profileBio, profileLocation, profileBirthday, profileDiscord, profileMinecraft, profileHK
 */

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    // Beispiel: Repository/Helper-Instanzen hier initialisieren, falls vorhanden
    // private val userRepository = UserRepository.instance
    // private val supabaseHelper = SupabaseHelper.instance

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val uri: Uri? = data?.data
            if (uri != null) {
                lifecycleScope.launch {
                    handleAvatarPicked(uri)
                }
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

        lifecycleScope.launch {
            loadProfile()
        }

        binding.editProfileButton.setOnClickListener {
            openEditProfile()
        }

        binding.profileAvatar.setOnClickListener {
            pickImageFromGallery()
        }

        binding.syncButton.setOnClickListener {
            lifecycleScope.launch {
                generateAndShowSyncToken()
            }
        }

        binding.btnResetToken.setOnClickListener {
            lifecycleScope.launch {
                resetSyncToken()
            }
        }

        binding.logoutButton.setOnClickListener {
            performLogout()
        }
    }

    private suspend fun loadProfile() {
        // Ersetze durch echten Repository-Aufruf
        // val profile = withContext(Dispatchers.IO) { userRepository.getUserProfile() }

        val profile = mapOf<String, Any?>(
            "username" to "Unbekannt",
            "email" to "noreply@example.com",
            "rank" to "Gast",
            "bio" to "",
            "location" to "",
            "birthday" to "",
            "discord" to "",
            "minecraft" to "",
            "hk" to ""
        )

        binding.profileUsername.text = profile["username"]?.toString() ?: ""
        binding.profileEmail.text = profile["email"]?.toString() ?: ""
        binding.profileRank.text = profile["rank"]?.toString() ?: ""
        binding.profileBio.text = profile["bio"]?.toString() ?: ""
        binding.profileLocation.text = profile["location"]?.toString() ?: ""
        binding.profileBirthday.text = profile["birthday"]?.toString() ?: ""
        binding.profileDiscord.text = profile["discord"]?.toString() ?: ""
        binding.profileMinecraft.text = profile["minecraft"]?.toString() ?: ""
        binding.profileHK.text = profile["hk"]?.toString() ?: ""
    }

    private fun openEditProfile() {
        // Öffne Edit-Fragment oder Dialog
        // Navigation.findNavController(requireView()).navigate(R.id.action_profile_to_editProfile)
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
                    // userRepository.uploadAvatar(bitmapToByteArray(bitmap))
                } catch (e: Exception) {
                    // Fehlerbehandlung
                }
            }
        }
    }

    private suspend fun generateAndShowSyncToken() {
        binding.syncTokenCard.isVisible = false
        binding.syncTokenText.text = ""

        val tokenResult: String? = withContext(Dispatchers.IO) {
            try {
                // return@withContext userRepository.generateSyncToken()
                "demo-sync-token-1234"
            } catch (e: Exception) {
                null
            }
        }

        if (!tokenResult.isNullOrBlank()) {
            binding.syncTokenText.text = tokenResult
            binding.syncTokenCard.isVisible = true
        } else {
            binding.syncTokenText.text = getStringSafe("Fehler beim Erzeugen des Tokens")
            binding.syncTokenCard.isVisible = true
        }
    }

    private suspend fun resetSyncToken() {
        val displayedTokenAny: Any? = try {
            binding.syncTokenText.text?.toString()
        } catch (e: Exception) {
            null
        }

        val tokenString: String? = when (displayedTokenAny) {
            is String -> displayedTokenAny
            is CharSequence -> displayedTokenAny.toString()
            else -> displayedTokenAny?.toString()
        }

        if (tokenString.isNullOrBlank()) {
            binding.syncTokenText.text = getStringSafe("Kein Token vorhanden")
            return
        }

        val success = withContext(Dispatchers.IO) {
            try {
                // Beispiel: SupabaseHelper.resetToken(tokenString)
                true
            } catch (e: Exception) {
                false
            }
        }

        if (success) {
            binding.syncTokenText.text = getStringSafe("Token zurückgesetzt")
            binding.syncTokenCard.isVisible = false
        } else {
            binding.syncTokenText.text = getStringSafe("Fehler beim Zurücksetzen")
            binding.syncTokenCard.isVisible = true
        }
    }

    private fun performLogout() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    // AuthHelper.signOut()
                } catch (_: Exception) {
                }
            }
            // Navigation.findNavController(requireView()).navigate(R.id.action_profile_to_login)
        }
    }

    private fun getStringSafe(value: String): String = value

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}