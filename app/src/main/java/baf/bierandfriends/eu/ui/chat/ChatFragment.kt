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

        loadMessages(scrollToBottom = true)

        lifecycleScope.launch {
            while (isActive) {
                delay(5000)
                loadMessages(scrollToBottom = false)
            }
        }
    }

    /**
     * Nachrichten laden.
     * @param scrollToBottom true = immer ans Ende scrollen (beim ersten Laden),
     *                       false = Scroll-Position behalten, damit der User
     *                       ältere Nachrichten lesen kann.
     */
    private fun loadMessages(scrollToBottom: Boolean) {
        if (!isAdded || _binding == null) return

        lifecycleScope.launch {
            try {
                val messages = chatRepository.getPublicMessages()
                if (!isAdded || _binding == null) return@launch

                val layoutManager = binding.chatRecyclerView.layoutManager
                    as? LinearLayoutManager
                    ?: LinearLayoutManager(requireContext()).apply {
                        stackFromEnd = true
                    }.also { binding.chatRecyclerView.layoutManager = it }

                // Scroll-Position vor dem Update merken
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                val oldCount = binding.chatRecyclerView.adapter?.itemCount ?: 0
                val wasAtBottom = oldCount == 0 || lastVisible >= oldCount - 1

                val adapter = ChatAdapter(messages, auth.currentUser?.uid ?: "")
                binding.chatRecyclerView.adapter = adapter

                if (messages.isNotEmpty()) {
                    if (scrollToBottom || wasAtBottom) {
                        // Ganz nach unten scrollen
                        binding.chatRecyclerView.scrollToPosition(messages.size - 1)
                    }
                    // Andernfalls: User hat hochgescrollt → Position beibehalten
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadMessages Fehler: ${e.javaClass.simpleName}: ${e.message}", e)
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
                // Nach eigenem Senden immer nach unten scrollen
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
