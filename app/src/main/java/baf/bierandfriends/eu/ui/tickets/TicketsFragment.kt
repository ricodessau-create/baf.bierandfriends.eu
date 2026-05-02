package baf.bierandfriends.eu.ui.tickets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import baf.bierandfriends.eu.data.models.Ticket
import baf.bierandfriends.eu.data.models.UserProfile
import baf.bierandfriends.eu.data.repository.TicketRepository
import baf.bierandfriends.eu.data.repository.UserRepository
import baf.bierandfriends.eu.databinding.FragmentTicketBinding
import baf.bierandfriends.eu.util.PermissionUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class TicketsFragment : Fragment() {

    private var _binding: FragmentTicketBinding? = null
    private val binding get() = _binding!!

    private val ticketRepository = TicketRepository()
    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()

    private var ticket: Ticket? = null
    private var ticketId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ticketId = arguments?.getString("ticketId")
        ticket = arguments?.getParcelable("ticket")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTicketBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.messagesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        loadTicketAndMessages()
        binding.sendMessageButton.setOnClickListener { sendMessage() }
    }

    private fun loadTicketAndMessages() {
        lifecycleScope.launch {
            if (ticket == null && ticketId != null) {
                ticket = ticketRepository.getAllTickets().firstOrNull { it.id == ticketId }
            }
            ticketId?.let { id ->
                val messages = ticketRepository.getTicketMessages(id)
                // update adapter if present
            }
        }
    }

    private fun sendMessage() {
        val text = binding.messageInput.text.toString().trim()
        if (text.isEmpty()) {
            Toast.makeText(requireContext(), "Bitte eine Nachricht eingeben.", Toast.LENGTH_SHORT).show()
            return
        }
        val currentUid = auth.currentUser?.uid
        if (currentUid == null) {
            Toast.makeText(requireContext(), "Bitte einloggen, um Nachrichten zu senden.", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            val profile: UserProfile? = userRepository.getUserProfile()
            val authorUid = ticket?.authorUid ?: ticketId?.let { id ->
                ticketRepository.getAllTickets().firstOrNull { it.id == id }?.authorUid
            }
            val allowed = PermissionUtils.canWriteTicket(profile, authorUid, currentUid)
            if (!allowed) {
                Toast.makeText(requireContext(), "Du darfst in diesem Ticket nicht schreiben.", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val displayName = profile?.username ?: auth.currentUser?.displayName ?: "Unbekannt"
            ticketId?.let { id ->
                ticketRepository.addTicketMessage(id, text, displayName)
                binding.messageInput.setText("")
                Toast.makeText(requireContext(), "Nachricht gesendet.", Toast.LENGTH_SHORT).show()
                loadTicketAndMessages()
            } ?: run {
                Toast.makeText(requireContext(), "Ticket nicht gefunden.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
