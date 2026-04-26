package baf.bierandfriends.eu.ui.auth

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
import baf.bierandfriends.eu.databinding.FragmentRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val userRepository = UserRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.registerButton.setOnClickListener { register() }
        binding.backToLoginButton.setOnClickListener { findNavController().navigateUp() }
    }

    private fun register() {
        val username = binding.registerUsername.text.toString().trim()
        val email = binding.registerEmail.text.toString().trim()
        val password = binding.registerPassword.text.toString().trim()
        val passwordConfirm = binding.registerPasswordConfirm.text.toString().trim()

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Bitte alle Felder ausfüllen.", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != passwordConfirm) {
            Toast.makeText(requireContext(), "Passwörter stimmen nicht überein.", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(requireContext(), "Passwort muss mindestens 6 Zeichen haben.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.registerProgress.visibility = View.VISIBLE
        binding.registerButton.isEnabled = false

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                lifecycleScope.launch {
                    val profile = UserProfile(
                        username = username,
                        email = email,
                        rank = "malzbier"
                    )
                    userRepository.updateUserProfile(profile)
                    binding.registerProgress.visibility = View.GONE
                    Toast.makeText(requireContext(), "Willkommen bei BAF, $username!", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_registerFragment_to_homeFragment)
                }
            }
            .addOnFailureListener {
                binding.registerProgress.visibility = View.GONE
                binding.registerButton.isEnabled = true
                Toast.makeText(requireContext(), "Fehler: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
