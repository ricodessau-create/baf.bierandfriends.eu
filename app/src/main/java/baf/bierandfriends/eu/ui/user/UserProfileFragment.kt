package baf.bierandfriends.eu.ui.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import baf.bierandfriends.eu.data.models.UserProfile
import baf.bierandfriends.eu.data.repository.UserRepository
import baf.bierandfriends.eu.databinding.FragmentUserProfileBinding
import baf.bierandfriends.eu.util.RankHelper
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserProfileFragment : Fragment() {

    private var _binding: FragmentUserProfileBinding? = null
    private val binding get() = _binding!!

    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var targetUsername = ""
    private var targetUid = ""
    private var isIgnored = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        targetUsername = arguments?.getString("username") ?: ""

        binding.backButton.setOnClickListener { findNavController().navigateUp() }

        binding.messageButton.setOnClickListener {
            if (targetUid.isNotEmpty()) {
                val bundle = Bundle().apply {
                    putString("receiverUid", targetUid)
                    putString("receiverName", targetUsername)
                }
                findNavController().navigate(
                    baf.bierandfriends.eu.R.id.action_userProfileFragment_to_privateChatFragment,
                    bundle
                )
            }
        }

        binding.ignoreButton.setOnClickListener { toggleIgnore() }

        loadUserProfile()
    }

    private fun loadUserProfile() {
        lifecycleScope.launch {
            try {
                val docs = db.collection("users")
                    .whereEqualTo("username", targetUsername)
                    .get().await()

                val doc = docs.documents.firstOrNull() ?: return@launch
                targetUid = doc.id
                val user = doc.toObject(UserProfile::class.java) ?: return@launch

                binding.userUsername.text = user.username
                binding.userEmail.visibility = View.GONE // DSGVO

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
                    Glide.with(this@UserProfileFragment)
                        .load(user.photoUrl)
                        .circleCrop()
                        .into(binding.userAvatar)
                }

                val currentUid = auth.currentUser?.uid ?: ""
                if (targetUid == currentUid) {
                    binding.messageButton.visibility = View.GONE
                    binding.ignoreButton.visibility = View.GONE
                } else {
                    isIgnored = userRepository.isUserIgnored(targetUid)
                    binding.ignoreButton.text =
                        if (isIgnored) "🔕 Nicht mehr ignorieren" else "🔇 Ignorieren"
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(requireContext(), "Nicht mehr ignoriert.", Toast.LENGTH_SHORT).show()
            } else {
                userRepository.ignoreUser(targetUid)
                isIgnored = true
                binding.ignoreButton.text = "🔕 Nicht mehr ignorieren"
                Toast.makeText(requireContext(), "Nutzer ignoriert.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
