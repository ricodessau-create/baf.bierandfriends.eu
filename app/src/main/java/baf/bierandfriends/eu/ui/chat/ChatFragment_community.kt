package baf.bierandfriends.eu.ui.community

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
import baf.bierandfriends.eu.ui.chat.ChatAdapter
import baf.bierandfriends.eu.util.UserPrefs
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private val TAG = "CommunityChat"

    private val chatRepository = ChatRepository()
    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var chatAdapter: ChatAdapter

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
        chatAdapter = ChatAdapter(emptyList(), auth.currentUser?.uid ?: "") { authorName ->
            val mention = "@$authorName "
            val current = binding.chatInput.text?.toString() ?: ""
            val newText = if (current.startsWith(mention)) current
                          else mention + current.removePrefix(mention)
            binding.chatInput.setText(newText)
            binding.chatInput.setSelection(newText.length)
            binding.chatInput.requestFocus()
        }
        binding.chatRecyclerView.layoutManager = layoutManager
        binding.chatRecyclerView.adapter = chatAdapter

        binding.chatBackButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.chatSendButton.setOnClickListener {
            sendMessage()
        }

        binding.chatInput.setOnEditorActionListener { _, _, _ ->
            sendMessage()
            true
        }

        lifecycleScope.launch {
            val profile = userRepository.getUserProfile()
            val uid = auth.currentUser?.uid ?: ""
            if (profile != null && uid.isNotEmpty()) {
                UserPrefs.save(requireContext(), uid, profile.username, profile.rank)
            }
        }

        loadMessages(scrollToBottom = true)

        lifecycleScope.launch {
            while (isActive) {
                delay(4000)
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
                val wasAtBottom = oldCount == 0 || lastVisible >= oldCount - 2

                chatAdapter.updateMessages(messages)

                if (messages.isNotEmpty() && (scrollToBottom || wasAtBottom)) {
                    binding.chatRecyclerView.scrollToPosition(messages.size - 1)
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadMessages: ${e.javaClass.simpleName}: ${e.message}", e)
                if (isAdded && _binding != null) {
                    Toast.makeText(requireContext(), "Chat konnte nicht geladen werden.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun sendMessage() {
        val text = binding.chatInput.text?.toString()?.trim() ?: return
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
                Log.e(TAG, "sendMessage: ${e.message}", e)
                if (isAdded && _binding != null) {
                    Toast.makeText(
                        requireContext(),
                        "Fehler: ${e.message ?: "Unbekannter Fehler"}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
