package baf.bierandfriends.eu.ui.profile

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import baf.bierandfriends.eu.R
import baf.bierandfriends.eu.data.repository.UserRepository
import baf.bierandfriends.eu.databinding.FragmentProfileBinding
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch

class ProfileFragment : Fragment(R.layout.fragment_profile) {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val userRepository = UserRepository()

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { uploadImage(it) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        binding.profileImage.setOnClickListener { imagePicker.launch("image/*") }
        loadProfile()
    }

    private fun loadProfile() {
        lifecycleScope.launch {
            val user = userRepository.getUserProfile()
            binding.profileUsername.text = user?.username
            user?.profileImageUrl?.let {
                Glide.with(this@ProfileFragment).load(it).circleCrop().into(binding.profileImage)
            }
        }
    }

    private fun uploadImage(uri: Uri) {
        lifecycleScope.launch {
            val url = userRepository.uploadProfileImage(uri)
            if (url != null) {
                Glide.with(this@ProfileFragment).load(url).circleCrop().into(binding.profileImage)
            }
        }
    }
}
