package baf.bierandfriends.eu.ui.profile

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import baf.bierandfriends.eu.R
import baf.bierandfriends.eu.data.repository.UserRepository
import baf.bierandfriends.eu.databinding.FragmentProfileBinding
import com.bumptech.glide.Glide // Benötigt Glide Bibliothek
import kotlinx.coroutines.launch

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val userRepository = UserRepository()

    // NEU: Image Picker Launcher
    private val getImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { uploadImage(it) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        loadProfile()

        // NEU: Klick auf Bild oder FAB öffnet den Picker
        binding.profileImage.setOnClickListener { getImage.launch("image/*") }
        binding.editImageFab.setOnClickListener { getImage.launch("image/*") }
    }

    private fun loadProfile() {
        lifecycleScope.launch {
            val profile = userRepository.getUserProfile()
            if (profile != null) {
                binding.profileUsername.text = profile.username
                binding.profileEmail.text = profile.email
                // ... andere Felder ...

                // Profilbild laden (mit Glide)
                if (!profile.profileImageUrl.isNullOrEmpty()) {
                    Glide.with(this@ProfileFragment)
                        .load(profile.profileImageUrl)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .circleCrop() // Rundes Bild
                        .into(binding.profileImage)
                }
            }
        }
    }

    private fun uploadImage(uri: Uri) {
        binding.profileProgress.visibility = View.VISIBLE // Progressbar hinzufügen
        lifecycleScope.launch {
            val newUrl = userRepository.uploadProfileImage(uri)
            binding.profileProgress.visibility = View.GONE
            
            if (newUrl != null) {
                // Bild sofort lokal aktualisieren
                Glide.with(this@ProfileFragment).load(newUrl).circleCrop().into(binding.profileImage)
                Toast.makeText(context, "Bild aktualisiert!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Upload fehlgeschlagen.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
