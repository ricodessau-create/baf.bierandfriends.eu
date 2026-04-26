package baf.bierandfriends.eu.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import baf.bierandfriends.eu.data.repository.UserRepository
import baf.bierandfriends.eu.databinding.FragmentProfileBinding
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val userRepository = UserRepository()

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

        binding.syncButton.setOnClickListener {
            generateToken()
        }
    }

    private fun loadProfile() {
        lifecycleScope.launch {
            val profile = userRepository.getUserProfile()
            if (profile != null) {
                binding.profileUsername.text = profile.username
                binding.profileEmail.text = profile.email
                binding.profileRank.text = profile.rank.replaceFirstChar { it.uppercase() }
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
