package baf.bierandfriends.eu.ui.chat

import android.os.Bundle
import android.util.Log
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
    private val TAG = "ChatFragment"

    private lateinit var chatAdapter: ChatAdapter
    private lateinit var layoutManager: LinearLayoutManager

    private var isSending = false

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

        layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        chatAdapter = ChatAdapter(emptyList(), auth.currentUser?.uid ?: "")
        binding.chatRecyclerView.layoutManager = layoutManager
        binding.chatRecyclerView.adapter = chatAdapter

        binding.chatBackButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.chatSendButton.setOnClickListener {
            sendMessage()
        }

        loadMessages(scrollToBottom = true)

        lifecycleScope.launch {
            while (isActive) {
                delay(5000)
                loadMessages(scrollToBottom = false)
            }
        }
    }

    private fun loadMessages(scrollToBottom: Boolean) {
        if (!isAdded || _binding == null) return

        lifecycleScope.launch {
            try {
                val messages = chatRepository.getPublicMessages()
                if (!isAdded || _binding == null) return@launch

                val oldCount = chatAdapter.itemCount
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                val isAtBottom = oldCount == 0 || lastVisible >= oldCount - 2

                chatAdapter.updateMessages(messages)

                if (messages.isNotEmpty() && (scrollToBottom || isAtBottom)) {
                    binding.chatRecyclerView.scrollToPosition(messages.size - 1)
                }

            } catch (e: Exception) {
                Log.e(TAG, "loadMessages Fehler: ${e.message}", e)
                if (isAdded && _binding != null) {
                    Toast.makeText(
                        requireContext(),
                        "Chat konnte nicht geladen werden.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun sendMessage() {
        if (isSending) return

        val text = binding.chatInput.text.toString().trim()
        if (text.isEmpty()) return

        isSending = true
        binding.chatSendButton.isEnabled = false

        lifecycleScope.launch {
            try {
                val profile = userRepository.getUserProfile()
                val name = profile?.username ?: "Unbekannt"
                val rank = profile?.rank ?: "malzbier"
                chatRepository.sendPublicMessage(text, name, rank)
                binding.chatInput.setText("")
                loadMessages(scrollToBottom = true)
            } catch (e: Exception) {
                Log.e(TAG, "sendMessage Fehler: ${e.message}", e)
                if (isAdded && _binding != null) {
                    Toast.makeText(requireContext(), "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                isSending = false
                if (isAdded && _binding != null) {
                    binding.chatSendButton.isEnabled = true
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
