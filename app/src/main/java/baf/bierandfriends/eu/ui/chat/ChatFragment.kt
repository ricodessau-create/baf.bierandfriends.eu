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

    private var adapter: ChatAdapter? = null
    private val currentMessages = mutableListOf<baf.bierandfriends.eu.data.models.ChatMessage>()

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

        val layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.chatRecyclerView.layoutManager = layoutManager

        adapter = ChatAdapter(currentMessages, auth.currentUser?.uid ?: "")
        binding.chatRecyclerView.adapter = adapter

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

                val layoutManager = binding.chatRecyclerView.layoutManager
                    as? LinearLayoutManager ?: return@launch

                val lastVisible = layoutManager.findLastVisibleItemPosition()
                val oldCount = currentMessages.size
                // User gilt als "unten" wenn er die letzten 3 Nachrichten sieht
                val wasAtBottom = oldCount == 0 || lastVisible >= oldCount - 3

                // Nur updaten wenn neue Nachrichten da sind
                if (messages.size != currentMessages.size ||
                    messages.lastOrNull()?.text != currentMessages.lastOrNull()?.text) {

                    currentMessages.clear()
                    currentMessages.addAll(messages)
                    adapter?.notifyDataSetChanged()

                    if (scrollToBottom || wasAtBottom) {
                        binding.chatRecyclerView.scrollToPosition(currentMessages.size - 1)
                    }
                    // Andernfalls: Position bleibt wo sie ist ✅
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
        val text = binding.chatInput.text.toString().trim()
        if (text.isEmpty()) return

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
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
