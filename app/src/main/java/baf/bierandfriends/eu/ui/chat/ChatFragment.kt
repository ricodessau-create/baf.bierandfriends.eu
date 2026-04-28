package baf.bierandfriends.eu.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import baf.bierandfriends.eu.data.repository.ChatRepository
import baf.bierandfriends.eu.data.repository.UserRepository
import baf.bierandfriends.eu.databinding.FragmentChatBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val chatRepository = ChatRepository()
    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.chatBackButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.chatSendButton.setOnClickListener {
            sendMessage()
        }

        loadMessages()

        lifecycleScope.launch {
            while (isActive) {
                delay(5000)
                loadMessages()
            }
        }
    }

    private fun loadMessages() {
        lifecycleScope.launch {
            val messages = chatRepository.getPublicMessages()
            val adapter = ChatAdapter(messages, auth.currentUser?.uid ?: "")
            binding.chatRecyclerView.adapter = adapter
            binding.chatRecyclerView.layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            if (messages.isNotEmpty()) {
                binding.chatRecyclerView.scrollToPosition(messages.size - 1)
            }
        }
    }

    private fun sendMessage() {
        val text = binding.chatInput.text.toString().trim()
        if (text.isEmpty()) return

        lifecycleScope.launch {
            val profile = userRepository.getUserProfile()
            val name = profile?.username ?: "Unbekannt"
            val rank = profile?.rank ?: "malzbier"

            chatRepository.sendPublicMessage(text, name, rank)
            binding.chatInput.setText("")
            loadMessages()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
