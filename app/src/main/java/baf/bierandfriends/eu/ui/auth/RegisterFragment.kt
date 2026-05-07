package baf.bierandfriends.eu.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.registerButton.setOnClickListener { register() }

        binding.backToLoginButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.linkPrivacy.setOnClickListener {
            showLegalDialog(
                getString(R.string.privacy_policy_title),
                getString(R.string.privacy_policy_text)
            )
        }

        binding.linkTerms.setOnClickListener {
            showLegalDialog(
                getString(R.string.terms_title),
                getString(R.string.terms_of_service_text)
            )
        }
    }

    private fun showLegalDialog(title: String, content: String) {
        val context = requireContext()
        val dp16 = (16 * context.resources.displayMetrics.density).toInt()

        val scrollView = ScrollView(context)
        val textView = TextView(context).apply {
            text = content.trim()
            setPadding(dp16, dp16, dp16, dp16)
            textSize = 13f
            setTextColor(resources.getColor(R.color.baf_text_primary, null))
            setBackgroundColor(resources.getColor(R.color.baf_background, null))
            lineSpacingMultiplier = 1.4f
        }
        scrollView.addView(textView)

        AlertDialog.Builder(context)
            .setTitle(title)
            .setView(scrollView)
            .setPositiveButton("Schließen") { dialog, _ -> dialog.dismiss() }
            .show()
            .apply {
                getButton(AlertDialog.BUTTON_POSITIVE)
                    ?.setTextColor(resources.getColor(R.color.baf_gold, null))
                window?.setBackgroundDrawableResource(R.color.baf_card)
            }
    }

    private fun register() {
        val username = binding.registerUsername.text.toString().trim()
        val email = binding.registerEmail.text.toString().trim()
        val password = binding.registerPassword.text.toString().trim()
        val passwordConfirm = binding.registerPasswordConfirm.text.toString().trim()

        if (username.isEmpty()) {
            Toast.makeText(requireContext(), "Bitte einen Benutzernamen eingeben.", Toast.LENGTH_SHORT).show()
            return
        }
        if (username.length < 3) {
            Toast.makeText(requireContext(), "Benutzername muss mind. 3 Zeichen haben.", Toast.LENGTH_SHORT).show()
            return
        }
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), "Bitte eine gültige E-Mail-Adresse eingeben.", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.length < 6) {
            Toast.makeText(requireContext(), "Passwort muss mindestens 6 Zeichen haben.", Toast.LENGTH_SHORT).show()
            return
        }
        if (password != passwordConfirm) {
            Toast.makeText(requireContext(), "Passwörter stimmen nicht überein.", Toast.LENGTH_SHORT).show()
            return
        }
        if (!binding.checkboxAge.isChecked) {
            Toast.makeText(requireContext(), "Bitte bestätige, dass du mindestens 16 Jahre alt bist.", Toast.LENGTH_LONG).show()
            return
        }
        if (!binding.checkboxPrivacy.isChecked) {
            Toast.makeText(requireContext(), "Bitte akzeptiere die Datenschutzerklärung.", Toast.LENGTH_LONG).show()
            return
        }
        if (!binding.checkboxTerms.isChecked) {
            Toast.makeText(requireContext(), "Bitte akzeptiere die Nutzungsbedingungen.", Toast.LENGTH_LONG).show()
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
                        rank = "malzbier",
                        privacyAccepted = true,
                        termsAccepted = true
                    )
                    userRepository.updateUserProfile(profile)
                    binding.registerProgress.visibility = View.GONE
                    Toast.makeText(
                        requireContext(),
                        "Willkommen bei BierAndFriends, $username! 🍺",
                        Toast.LENGTH_SHORT
                    ).show()
                    findNavController().navigate(R.id.action_registerFragment_to_homeFragment)
                }
            }
            .addOnFailureListener {
                binding.registerProgress.visibility = View.GONE
                binding.registerButton.isEnabled = true
                val msg = when {
                    it.message?.contains("email address is already") == true ->
                        "Diese E-Mail-Adresse ist bereits registriert."
                    it.message?.contains("badly formatted") == true ->
                        "Ungültige E-Mail-Adresse."
                    else -> "Registrierung fehlgeschlagen: ${it.message}"
                }
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
