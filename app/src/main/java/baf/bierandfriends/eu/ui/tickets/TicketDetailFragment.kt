package baf.bierandfriends.eu.ui.tickets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import baf.bierandfriends.eu.data.repository.TicketRepository
import baf.bierandfriends.eu.data.repository.UserRepository
import baf.bierandfriends.eu.databinding.FragmentTicketDetailBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TicketDetailFragment : Fragment() {

    private var _binding: FragmentTicketDetailBinding? = null
    private val binding get() = _binding!!

    private val ticketRepository = TicketRepository()
    private val userRepository = UserRepository()
    private var ticketId = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTicketDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ticketId = arguments?.getString("ticketId") ?: ""

        binding.ticketDetailBack.setOnClickListener { findNavController().navigateUp() }

        binding.ticketSendButton.setOnClickListener { sendMessage() }

        binding.ticketMessageInput.setOnEditorActionListener { _, _, _ ->
            sendMessage()
            true
        }

        loadTicket()
        loadMessages()

        // Polling alle 5s
        lifecycleScope.launch {
            while (isActive) {
                delay(5000)
                loadMessages()
            }
        }
    }

    private fun loadTicket() {
        lifecycleScope.launch {
            try {
                val tickets = ticketRepository.getMyTickets() + ticketRepository.getAllTickets()
                val ticket = tickets.distinctBy { it.id }.find { it.id == ticketId } ?: return@launch

                binding.ticketDetailTitle.text = ticket.title
                binding.ticketDetailDescription.text = ticket.description
                binding.ticketDetailAuthor.text = "Von: ${ticket.authorName}"

                val statusDisplay = when (ticket.status.lowercase()) {
                    "offen" -> "🔴 Offen"
                    "in bearbeitung" -> "🟡 In Bearbeitung"
                    "geschlossen" -> "🟢 Geschlossen"
                    else -> ticket.status
                }
                binding.ticketDetailStatus.text = statusDisplay

                val dateText = if (ticket.createdAt > 0L)
                    SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMAN).format(Date(ticket.createdAt))
                else ""
                binding.ticketDetailDate.text = dateText
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadMessages() {
        if (ticketId.isEmpty()) return
        lifecycleScope.launch {
            try {
                val messages = ticketRepository.getTicketMessages(ticketId)
                val adapter = TicketMessageAdapter(messages)
                binding.ticketMessagesRecycler.adapter = adapter
                binding.ticketMessagesRecycler.layoutManager =
                    LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
                if (messages.isNotEmpty()) {
                    binding.ticketMessagesRecycler.scrollToPosition(messages.size - 1)
                }
            } catch (e: Exception) {
                // Ignorieren
            }
        }
    }

    private fun sendMessage() {
        val text = binding.ticketMessageInput.text.toString().trim()
        if (text.isEmpty() || ticketId.isEmpty()) return

        lifecycleScope.launch {
            try {
                val profile = userRepository.getUserProfile()
                val name = profile?.username ?: "Unbekannt"
                ticketRepository.addTicketMessage(ticketId, text, name)
                binding.ticketMessageInput.setText("")
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
