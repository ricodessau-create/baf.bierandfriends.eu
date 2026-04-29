package baf.bierandfriends.eu.ui.profile

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import baf.bierandfriends.eu.data.repository.UserRepository
import baf.bierandfriends.eu.databinding.FragmentProfileBinding
import baf.bierandfriends.eu.util.RankHelper
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) openImagePicker() else
            Toast.makeText(requireContext(), "Berechtigung verweigert.", Toast.LENGTH_SHORT).show()
    }

    private val imagePicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {}
            uploadProfileImage(uri)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadProfile()

        binding.profileAvatar.setOnClickListener { checkAndPickImage() }

        binding.syncButton.setOnClickListener { generateToken() }

        binding.editProfileButton.setOnClickListener {
            showEditDialog()
        }

        binding.logoutButton.setOnClickListener {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            GoogleSignIn.getClient(requireActivity(), gso).signOut().addOnCompleteListener {
                auth.signOut()
                requireActivity().finish()
                startActivity(requireActivity().intent)
            }
        }
    }

    private fun checkAndPickImage() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED)
            openImagePicker() else permissionLauncher.launch(permission)
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        imagePicker.launch(intent)
    }

    private fun loadProfile() {
        lifecycleScope.launch {
            val profile = userRepository.getUserProfile() ?: return@launch
            binding.profileUsername.text = profile.username
            binding.profileEmail.text = profile.email
            binding.profileRank.text = RankHelper.getRankDisplayName(profile.rank)
            binding.profileRank.setTextColor(RankHelper.getRankColor(requireContext(), profile.rank))

            if (profile.bio.isNotEmpty()) {
                binding.profileBio.text = profile.bio
                binding.profileBio.visibility = View.VISIBLE
            }
            if (profile.location.isNotEmpty()) {
                binding.profileLocation.text = "📍 ${profile.location}"
                binding.profileLocation.visibility = View.VISIBLE
            }
            if (profile.birthday.isNotEmpty()) {
                binding.profileBirthday.text = "🎂 ${profile.birthday}"
                binding.profileBirthday.visibility = View.VISIBLE
            }
            if (profile.discord.isNotEmpty()) {
                binding.profileDiscord.text = "💬 Discord: ${profile.discord}"
                binding.profileDiscord.visibility = View.VISIBLE
            }
            if (profile.minecraftName.isNotEmpty()) {
                binding.profileMinecraft.text = "⚔ Minecraft: ${profile.minecraftName}"
                binding.profileMinecraft.visibility = View.VISIBLE
            }
            if (profile.hopfenkaltschalen > 0) {
                binding.profileHK.text = "🍺 ${profile.hopfenkaltschalen} HK"
                binding.profileHK.visibility = View.VISIBLE
            }
            if (profile.photoUrl.isNotEmpty()) {
                Glide.with(this@ProfileFragment).load(profile.photoUrl).circleCrop().into(binding.profileAvatar)
            }
        }
    }

    private fun showEditDialog() {
        lifecycleScope.launch {
            val profile = userRepository.getUserProfile() ?: return@launch
            val dialog = android.app.AlertDialog.Builder(requireContext())
            val dialogView = layoutInflater.inflate(baf.bierandfriends.eu.R.layout.dialog_edit_profile, null)

            val etUsername = dialogView.findViewById<android.widget.EditText>(baf.bierandfriends.eu.R.id.editUsername)
            val etBio = dialogView.findViewById<android.widget.EditText>(baf.bierandfriends.eu.R.id.editBio)
            val etLocation = dialogView.findViewById<android.widget.EditText>(baf.bierandfriends.eu.R.id.editLocation)
            val etBirthday = dialogView.findViewById<android.widget.EditText>(baf.bierandfriends.eu.R.id.editBirthday)
            val etDiscord = dialogView.findViewById<android.widget.EditText>(baf.bierandfriends.eu.R.id.editDiscord)

            etUsername.setText(profile.username)
            etBio.setText(profile.bio)
            etLocation.setText(profile.location)
            etBirthday.setText(profile.birthday)
            etDiscord.setText(profile.discord)

            dialog.setView(dialogView)
                .setTitle("Profil bearbeiten")
                .setPositiveButton("Speichern") { _, _ ->
                    lifecycleScope.launch {
                        val updated = profile.copy(
                            username = etUsername.text.toString().trim().ifEmpty { profile.username },
                            bio = etBio.text.toString().trim(),
                            location = etLocation.text.toString().trim(),
                            birthday = etBirthday.text.toString().trim(),
                            discord = etDiscord.text.toString().trim()
                        )
                        userRepository.updateUserProfile(updated)
                        Toast.makeText(requireContext(), "✅ Profil gespeichert!", Toast.LENGTH_SHORT).show()
                        loadProfile()
                    }
                }
                .setNegativeButton("Abbrechen", null)
                .show()
        }
    }

    private fun uploadProfileImage(uri: Uri) {
        val uid = auth.currentUser?.uid ?: return
        binding.profileAvatar.alpha = 0.5f
        Toast.makeText(requireContext(), "Wird hochgeladen...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                val storage = FirebaseStorage.getInstance()
                val ref = storage.reference.child("profile_images/$uid.jpg")

                val bytes = requireContext().contentResolver.openInputStream(uri)
                    ?.use { it.readBytes() } ?: throw Exception("Bild konnte nicht gelesen werden")

                ref.putBytes(bytes).await()
                val downloadUrl = ref.downloadUrl.await().toString()

                val profile = userRepository.getUserProfile()
                val updated = (profile ?: baf.bierandfriends.eu.data.models.UserProfile()).copy(photoUrl = downloadUrl)
                userRepository.updateUserProfile(updated)

                Glide.with(this@ProfileFragment).load(uri).circleCrop().into(binding.profileAvatar)
                binding.profileAvatar.alpha = 1f
                Toast.makeText(requireContext(), "✅ Profilbild gespeichert!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                binding.profileAvatar.alpha = 1f
                Toast.makeText(requireContext(), "Fehler: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun generateToken() {
        lifecycleScope.launch {
            val token = userRepository.generateSyncToken()
            binding.syncTokenText.text = token
            binding.syncTokenCard.visibility = View.VISIBLE
            Toast.makeText(requireContext(), "Tippe /biersync $token im Minecraft!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
