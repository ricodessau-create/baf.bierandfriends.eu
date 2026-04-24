package com.deinprojekt.ui.profile

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.deinprojekt.data.models.UserProfile
import com.deinprojekt.data.repository.UserRepository
import com.deinprojekt.databinding.FragmentProfileBinding
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

        loadUserProfile()

        binding.profileSyncButton.setOnClickListener {
            showSyncTokenDialog()
        }
    }

    private fun loadUserProfile() {
        lifecycleScope.launch {
            val profile = userRepository.getUserProfile()

            if (profile != null) {
                binding.profileUsername.text = profile.username
                binding.profileEmail.text = profile.email
                binding.profileRankApp.text = "App-Rang: ${profile.rank_app}"
                binding.profileRankIngame.text =
                    "Ingame-Rang: ${if (profile.synced) profile.rank_ingame else "Nicht synchronisiert"}"
                binding.profileUUID.text =
                    "UUID: ${if (profile.synced) profile.uuid else "-"}"
            }
        }
    }

    private fun showSyncTokenDialog() {
        val token = (100000..999999).random().toString()

        AlertDialog.Builder(requireContext())
            .setTitle("Sync-Code")
            .setMessage("Gib im Spiel ein:\n\n/biersync $token")
            .setPositiveButton("OK", null)
            .show()

        lifecycleScope.launch {
            userRepository.saveSyncToken(token)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
