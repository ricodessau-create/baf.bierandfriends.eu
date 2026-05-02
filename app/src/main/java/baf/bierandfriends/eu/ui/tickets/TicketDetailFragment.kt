package baf.bierandfriends.eu.ui.tickets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import baf.bierandfriends.eu.data.repository.TicketRepository
import baf.bierandfriends.eu.databinding.FragmentTicketDetailBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TicketDetailFragment : Fragment() {

    private var _binding: FragmentTicketDetailBinding? = null
    private val binding get() = _binding!!

    private val ticketRepository = TicketRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTicketDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ticketId = arguments?.getString("ticketId") ?: return

        binding.ticketDetailBack.setOnClickListener { findNavController().navigateUp() }

        loadTicket(ticketId)
    }

    private fun loadTicket(ticketId: String) {
        lifecycleScope.launch {
            val tickets = ticketRepository.getMyTickets() + ticketRepository.getAllTickets()
            val ticket = tickets.find { it.id == ticketId } ?: return@launch

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

            val dateText = if (ticket.createdAt > 0L) {
                SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMAN)
                    .format(Date(ticket.createdAt))
            } else ""
            binding.ticketDetailDate.text = dateText
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
