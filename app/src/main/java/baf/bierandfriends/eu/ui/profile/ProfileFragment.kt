package baf.bierandfriends.eu.ui.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import baf.bierandfriends.eu.data.repository.UserRepository
import baf.bierandfriends.eu.databinding.FragmentProfileBinding
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
    private val storage = FirebaseStorage.getInstance()

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data ?: return@registerForActivityResult
                uploadProfileImage(uri)
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
        loadProfile()

        binding.profileAvatar.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            imagePicker.launch(intent)
        }

        binding.syncButton.setOnClickListener { generateToken() }

        binding.logoutButton.setOnClickListener {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            val googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
            googleSignInClient.signOut().addOnCompleteListener {
                auth.signOut()
                requireActivity().runOnUiThread {
                    requireActivity().finish()
                    startActivity(requireActivity().intent)
                }
            }
        }
    }

    private fun loadProfile() {
        lifecycleScope.launch {
            val profile = userRepository.getUserProfile()
            if (profile != null) {
                binding.profileUsername.text = profile.username
                binding.profileEmail.text = profile.email
                binding.profileRank.text = profile.rank.replaceFirstChar { it.uppercase() }

                if (profile.photoUrl.isNotEmpty()) {
                    Glide.with(this@ProfileFragment)
                        .load(profile.photoUrl)
                        .circleCrop()
                        .into(binding.profileAvatar)
                }

                if (profile.minecraftName.isNotEmpty()) {
                    binding.profileMinecraft.text = "Minecraft: ${profile.minecraftName}"
                    binding.profileMinecraft.visibility = View.VISIBLE
                }

                if (profile.hopfenkaltschalen > 0) {
                    binding.profileHK.text = "${profile.hopfenkaltschalen} HK"
                    binding.profileHK.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun uploadProfileImage(uri: Uri) {
        val uid = auth.currentUser?.uid ?: return
        binding.profileAvatar.alpha = 0.5f

        lifecycleScope.launch {
            try {
                val ref = storage.getReference("profile_images/$uid.jpg")
                ref.putFile(uri).await()
                val downloadUrl = ref.downloadUrl.await().toString()

                val profile = userRepository.getUserProfile()
                if (profile != null) {
                    userRepository.updateUserProfile(profile.copy(photoUrl = downloadUrl))
                }

                Glide.with(this@ProfileFragment)
                    .load(uri)
                    .circleCrop()
                    .into(binding.profileAvatar)

                binding.profileAvatar.alpha = 1f
                Toast.makeText(requireContext(), "Profilbild gespeichert!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                binding.profileAvatar.alpha = 1f
                Toast.makeText(requireContext(), "Fehler beim Hochladen.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun generateToken() {
        lifecycleScope.launch {
            val token = userRepository.generateSyncToken()
            binding.syncTokenText.text = token
            binding.syncTokenCard.visibility = View.VISIBLE
            Toast.makeText(
                requireContext(),
                "Tippe /biersync $token im Minecraft!",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
