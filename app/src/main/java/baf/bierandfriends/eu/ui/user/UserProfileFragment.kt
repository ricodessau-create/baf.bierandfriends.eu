package baf.bierandfriends.eu.ui.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import baf.bierandfriends.eu.data.repository.UserRepository
import baf.bierandfriends.eu.databinding.FragmentUserProfileBinding
import baf.bierandfriends.eu.util.RankHelper
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class UserProfileFragment : Fragment() {

    private var _binding: FragmentUserProfileBinding? = null
    private val binding get() = _binding!!

    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()
    private var targetUsername = ""
    private var targetUid = ""
    private var isIgnored = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentUserProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        targetUsername = arguments?.getString("username") ?: ""

        binding.backButton.setOnClickListener { findNavController().navigateUp() }

        binding.messageButton.setOnClickListener {
            if (targetUid.isNotEmpty()) {
                val action = UserProfileFragmentDirections
                    .actionUserProfileFragmentToPrivateChatFragment(targetUid, targetUsername)
                findNavController().navigate(action)
            }
        }

        binding.ignoreButton.setOnClickListener {
            toggleIgnore()
        }

        loadUserProfile()
    }

    private fun loadUserProfile() {
        lifecycleScope.launch {
            val users = userRepository.getAllUsers()
            val user = users.find { it.username == targetUsername } ?: return@launch

            val currentUid = auth.currentUser?.uid ?: ""
            val docs = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").whereEqualTo("username", targetUsername).get().await()
            targetUid = docs.documents.firstOrNull()?.id ?: ""

            binding.userUsername.text = user.username
            binding.userEmail.text = user.email
            binding.userRank.text = RankHelper.getRankDisplayName(user.rank)
            binding.userRank.setTextColor(RankHelper.getRankColor(requireContext(), user.rank))

            if (user.bio.isNotEmpty()) {
                binding.userBio.text = user.bio
                binding.userBio.visibility = View.VISIBLE
            }
            if (user.location.isNotEmpty()) {
                binding.userLocation.text = "📍 ${user.location}"
                binding.userLocation.visibility = View.VISIBLE
            }
            if (user.minecraftName.isNotEmpty()) {
                binding.userMinecraft.text = "⚔ ${user.minecraftName}"
                binding.userMinecraft.visibility = View.VISIBLE
            }
            if (user.photoUrl.isNotEmpty()) {
                Glide.with(this@UserProfileFragment).load(user.photoUrl).circleCrop().into(binding.userAvatar)
            }

            if (targetUid == currentUid) {
                binding.messageButton.visibility = View.GONE
                binding.ignoreButton.visibility = View.GONE
            } else {
                isIgnored = userRepository.isUserIgnored(targetUid)
                binding.ignoreButton.text = if (isIgnored) "🔕 Ignoriert" else "🔇 Ignorieren"
            }
        }
    }

    private fun toggleIgnore() {
        if (targetUid.isEmpty()) return
        lifecycleScope.launch {
            if (isIgnored) {
                userRepository.unignoreUser(targetUid)
                isIgnored = false
                binding.ignoreButton.text = "🔇 Ignorieren"
                Toast.makeText(requireContext(), "Nutzer nicht mehr ignoriert.", Toast.LENGTH_SHORT).show()
            } else {
                userRepository.ignoreUser(targetUid)
                isIgnored = true
                binding.ignoreButton.text = "🔕 Ignoriert"
                Toast.makeText(requireContext(), "Nutzer wird ignoriert.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T {
        return kotlinx.coroutines.tasks.await(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
