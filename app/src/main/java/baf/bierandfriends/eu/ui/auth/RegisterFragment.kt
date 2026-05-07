package baf.bierandfriends.eu.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import baf.bierandfriends.eu.R
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
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnRegister.setOnClickListener { register() }

        binding.txtTermsLink.setOnClickListener {
            showTermsDialog()
        }

        // Fix: Button-Farbe korrekt setzen
        binding.btnRegister.setBackgroundColor(
            ContextCompat.getColor(requireContext(), R.color.baf_gold)
        )
    }

    private fun register() {
        val email = binding.editEmail.text.toString().trim()
        val password = binding.editPassword.text.toString().trim()
        val username = binding.editUsername.text.toString().trim()

        if (email.isEmpty() || password.isEmpty() || username.isEmpty()) {
            Toast.makeText(requireContext(), "Bitte alle Felder ausfüllen", Toast.LENGTH_SHORT).show()
            return
        }

        if (!binding.checkTerms.isChecked) {
            Toast.makeText(requireContext(), "Bitte akzeptiere die Nutzungsbedingungen", Toast.LENGTH_SHORT).show()
            return
        }

        binding.registerProgress.visibility = View.VISIBLE

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
                    findNavController().navigate(R.id.action_registerFragment_to_homeFragment)
                }
            }
            .addOnFailureListener {
                binding.registerProgress.visibility = View.GONE
                Toast.makeText(requireContext(), "Fehler: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showTermsDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Nutzungsbedingungen")
            .setMessage(getString(R.string.terms_of_service_text))
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
