package com.deinprojekt.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
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
    }

    private fun loadUserProfile() {
        lifecycleScope.launch {
            val profile = userRepository.getUserProfile()

            if (profile != null) {
                binding.profileUsername.text = profile.username
                binding.profileEmail.text = profile.email
                binding.profileRank.text = profile.rank
            } else {
                binding.profileUsername.text = "Unbekannt"
                binding.profileEmail.text = "Keine Daten gefunden"
                binding.profileRank.text = ""
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
