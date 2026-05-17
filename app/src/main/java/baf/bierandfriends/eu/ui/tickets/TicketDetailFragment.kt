package baf.bierandfriends.eu.ui.tickets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import baf.bierandfriends.eu.data.repository.TicketRepository
import baf.bierandfriends.eu.data.repository.UserRepository
import baf.bierandfriends.eu.databinding.FragmentTicketDetailBinding
import baf.bierandfriends.eu.util.RankHelper
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
    private var canClose = false
    private var canDelete = false
    private var currentStatus = ""

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
            sendMessage(); true
        }

        lifecycleScope.launch {
            val profile = userRepository.getUserProfile()
            val rank = profile?.rank ?: ""
            canClose = RankHelper.canCloseTickets(rank)
            canDelete = RankHelper.isAdmin(rank)

            loadTicket()
            loadMessages()
        }

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
                val tickets = (ticketRepository.getMyTickets() +
                    ticketRepository.getAllTickets()).distinctBy { it.id }
                val ticket = tickets.find { it.id == ticketId } ?: return@launch

                currentStatus = ticket.status.lowercase()
                binding.ticketDetailTitle.text = ticket.title
                binding.ticketDetailDescription.text = ticket.description
                binding.ticketDetailAuthor.text = "Von: ${ticket.authorName}"

                binding.ticketDetailStatus.text = when (currentStatus) {
                    "offen"          -> "🔴 Offen"
                    "in bearbeitung" -> "🟡 In Bearbeitung"
                    "geschlossen"    -> "🟢 Geschlossen"
                    else             -> ticket.status
                }

                val dateText = if (ticket.createdAt > 0L)
                    SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMAN)
                        .format(Date(ticket.createdAt))
                else ""
                binding.ticketDetailDate.text = dateText

                // Schließen-Button
                if (canClose) {
                    binding.closeTicketButton.visibility = View.VISIBLE
                    if (currentStatus == "geschlossen") {
                        binding.closeTicketButton.text = "✅ Bereits geschlossen"
                        binding.closeTicketButton.isEnabled = false
                    } else {
                        binding.closeTicketButton.text = "🔒 TICKET SCHLIESSEN"
                        binding.closeTicketButton.isEnabled = true
                        binding.closeTicketButton.setOnClickListener { confirmCloseTicket() }
                    }
                }

                // Löschen-Button nur für Admin/Cheffe und nur wenn geschlossen
                if (canDelete && currentStatus == "geschlossen") {
                    binding.deleteTicketButton.visibility = View.VISIBLE
                    binding.deleteTicketButton.setOnClickListener { confirmDeleteTicket() }
                } else {
                    binding.deleteTicketButton.visibility = View.GONE
                }

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
                if (messages.isNotEmpty())
                    binding.ticketMessagesRecycler.scrollToPosition(messages.size - 1)
            } catch (e: Exception) { }
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

    private fun confirmCloseTicket() {
        AlertDialog.Builder(requireContext())
            .setTitle("Ticket schließen")
            .setMessage("Möchtest du dieses Ticket als geschlossen markieren?")
            .setPositiveButton("Schließen") { _, _ ->
                lifecycleScope.launch {
                    try {
                        ticketRepository.updateTicketStatus(ticketId, "geschlossen")
                        Toast.makeText(requireContext(), "✅ Ticket geschlossen.", Toast.LENGTH_SHORT).show()
                        loadTicket()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    private fun confirmDeleteTicket() {
        AlertDialog.Builder(requireContext())
            .setTitle("Ticket löschen")
            .setMessage("Möchtest du dieses geschlossene Ticket endgültig löschen?\nDiese Aktion kann nicht rückgängig gemacht werden.")
            .setPositiveButton("Löschen") { _, _ ->
                lifecycleScope.launch {
                    try {
                        ticketRepository.deleteTicket(ticketId)
                        Toast.makeText(requireContext(), "✅ Ticket gelöscht.", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
