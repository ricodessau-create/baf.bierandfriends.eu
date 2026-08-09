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

    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var chatAdapter: ChatAdapter

    private var isSending = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPrivateChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        receiverUid  = arguments?.getString("receiverUid")  ?: ""
        receiverName = arguments?.getString("receiverName") ?: "Nutzer"

        binding.privateChatTitle.text = "💬 $receiverName"
        binding.privateChatBack.setOnClickListener { findNavController().navigateUp() }
        binding.privateChatSend.setOnClickListener { sendMessage() }

        binding.privateChatInput.setOnEditorActionListener { _, _, _ ->
            sendMessage()
            true
        }

        layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        val currentUid = auth.currentUser?.uid ?: ""
        chatAdapter = ChatAdapter(emptyList(), currentUid) { authorName ->
            val mention = "@$authorName "
            val current = binding.privateChatInput.text?.toString() ?: ""
            val newText = if (current.startsWith(mention)) current
                          else mention + current.removePrefix(mention)
            binding.privateChatInput.setText(newText)
            binding.privateChatInput.setSelection(newText.length)
            binding.privateChatInput.requestFocus()
        }
        binding.privateChatRecycler.layoutManager = layoutManager
        binding.privateChatRecycler.adapter = chatAdapter

        loadMessages(scrollToBottom = true)

        lifecycleScope.launch {
            while (isActive) {
                delay(4000)
                loadMessages(scrollToBottom = false)
            }
        }
    }

    private fun loadMessages(scrollToBottom: Boolean) {
        if (receiverUid.isEmpty() || !isAdded || _binding == null) return
        lifecycleScope.launch {
            try {
                val messages = chatRepository.getPrivateMessages(receiverUid)
                if (!isAdded || _binding == null) return@launch
                val currentUid = auth.currentUser?.uid ?: ""

                val chatMessages = messages.map { msg ->
                    ChatMessage(
                        text       = msg.text,
                        authorUid  = msg.senderUid,
                        authorName = msg.senderName,
                        createdAt  = msg.createdAt
                    )
                }

                val oldCount = chatAdapter.itemCount
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                val wasAtBottom = oldCount == 0 || lastVisible >= oldCount - 1

                chatAdapter.updateMessages(chatMessages)

                if (chatMessages.isNotEmpty() && (scrollToBottom || wasAtBottom)) {
                    binding.privateChatRecycler.scrollToPosition(chatMessages.size - 1)
                }
            } catch (e: Exception) {
                if (isAdded && _binding != null) {
                    Toast.makeText(requireContext(), "Fehler beim Laden: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun sendMessage() {
        if (isSending) return
        val text = binding.privateChatInput.text?.toString()?.trim() ?: return
        if (text.isEmpty() || receiverUid.isEmpty()) return

        isSending = true
        binding.privateChatSend.isEnabled = false

        lifecycleScope.launch {
            val profile = userRepository.getUserProfile()
            val name = profile?.username ?: "Unbekannt"
            try {
                chatRepository.sendPrivateMessage(text, receiverUid, name)
                binding.privateChatInput.setText("")
                loadMessages(scrollToBottom = true)
            } catch (e: Exception) {
                if (isAdded && _binding != null) {
                    Toast.makeText(requireContext(), "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                isSending = false
                if (isAdded && _binding != null) {
                    binding.privateChatSend.isEnabled = true
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
