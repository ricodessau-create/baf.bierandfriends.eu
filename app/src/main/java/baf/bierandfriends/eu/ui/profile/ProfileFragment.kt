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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()

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
