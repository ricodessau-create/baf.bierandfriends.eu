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
import baf.bierandfriends.eu.data.models.ChatMessage
import baf.bierandfriends.eu.data.models.PrivateMessage
import baf.bierandfriends.eu.data.repository.ChatRepository
import baf.bierandfriends.eu.data.repository.UserRepository
import baf.bierandfriends.eu.databinding.FragmentPrivateChatBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PrivateChatFragment : Fragment() {

    private var _binding: FragmentPrivateChatBinding? = null
    private val binding get() = _binding!!

    private val chatRepository = ChatRepository()
    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()
    private var receiverUid = ""
    private var receiverName = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPrivateChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        receiverUid = arguments?.getString("receiverUid") ?: ""
        receiverName = arguments?.getString("receiverName") ?: "Nutzer"

        binding.privateChatTitle.text = "💬 $receiverName"
        binding.privateChatBack.setOnClickListener { findNavController().navigateUp() }
        binding.privateChatSend.setOnClickListener { sendMessage() }

        loadMessages()

        lifecycleScope.launch {
            while (isActive) {
                delay(4000)
                loadMessages()
            }
        }
    }

    private fun loadMessages() {
        if (receiverUid.isEmpty()) return
        lifecycleScope.launch {
            val messages = chatRepository.getPrivateMessages(receiverUid)
            val currentUid = auth.currentUser?.uid ?: ""

            val chatMessages = messages.map { msg ->
                ChatMessage(
                    text = msg.text,
                    authorUid = msg.senderUid,
                    authorName = msg.senderName,
                    createdAt = msg.createdAt
                )
            }

            val adapter = ChatAdapter(chatMessages, currentUid)
            binding.privateChatRecycler.adapter = adapter
            binding.privateChatRecycler.layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            if (chatMessages.isNotEmpty()) {
                binding.privateChatRecycler.scrollToPosition(chatMessages.size - 1)
            }
        }
    }

    private fun sendMessage() {
        val text = binding.privateChatInput.text.toString().trim()
        if (text.isEmpty() || receiverUid.isEmpty()) return

        lifecycleScope.launch {
            val profile = userRepository.getUserProfile()
            val name = profile?.username ?: "Unbekannt"

            try {
                chatRepository.sendPrivateMessage(text, receiverUid, name)
                binding.privateChatInput.setText("")
                loadMessages()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
