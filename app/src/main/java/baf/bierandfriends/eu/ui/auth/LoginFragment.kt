package baf.bierandfriends.eu.ui.auth

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import baf.bierandfriends.eu.R
import baf.bierandfriends.eu.data.models.UserProfile
import baf.bierandfriends.eu.data.repository.UserRepository
import baf.bierandfriends.eu.databinding.FragmentLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val userRepository = UserRepository()

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                binding.loginProgress.visibility = View.GONE
                Toast.makeText(requireContext(), "Google Login fehlgeschlagen: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            binding.loginProgress.visibility = View.GONE
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (auth.currentUser != null) {
            findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
            return
        }

        binding.loginButton.setOnClickListener { login() }
        binding.registerButton.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
        binding.googleLoginButton.setOnClickListener { googleLogin() }
    }

    private fun login() {
        val email = binding.loginEmail.text.toString().trim()
        val password = binding.loginPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Bitte alle Felder ausfüllen.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.loginProgress.visibility = View.VISIBLE
        binding.loginButton.isEnabled = false

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                binding.loginProgress.visibility = View.GONE
                findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
            }
            .addOnFailureListener {
                binding.loginProgress.visibility = View.GONE
                binding.loginButton.isEnabled = true
                Toast.makeText(requireContext(), "Login fehlgeschlagen: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun googleLogin() {
        binding.loginProgress.visibility = View.VISIBLE
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
        googleSignInClient.signOut().addOnCompleteListener {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val isNew = result.additionalUserInfo?.isNewUser ?: false
                if (isNew) {
                    val user = auth.currentUser
                    lifecycleScope.launch {
                        val profile = UserProfile(
                            username = user?.displayName ?: "Spieler",
                            email = user?.email ?: "",
                            rank = "malzbier",
                            photoUrl = user?.photoUrl?.toString() ?: ""
                        )
                        userRepository.updateUserProfile(profile)
                    }
                }
                binding.loginProgress.visibility = View.GONE
                findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
            }
            .addOnFailureListener {
                binding.loginProgress.visibility = View.GONE
                Toast.makeText(requireContext(), "Google Login fehlgeschlagen: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
