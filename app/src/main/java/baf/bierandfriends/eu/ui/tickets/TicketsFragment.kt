package baf.bierandfriends.eu.ui.tickets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import baf.bierandfriends.eu.R
import baf.bierandfriends.eu.data.repository.TicketRepository
import baf.bierandfriends.eu.databinding.FragmentTicketsBinding
import kotlinx.coroutines.launch

class TicketsFragment : Fragment() {

    private var _binding: FragmentTicketsBinding? = null
    private val binding get() = _binding!!

    private val ticketRepository = TicketRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTicketsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTabs()
        loadTickets()

        binding.newTicketButton.setOnClickListener {
            findNavController().navigate(R.id.action_ticketsFragment_to_newTicketFragment)
        }
    }

    private fun setupTabs() {
        binding.tabMeineTickets.setOnClickListener {
            binding.tabMeineTickets.setTextColor(
                resources.getColor(R.color.baf_gold, null)
            )
            binding.tabNeuesTicket.setTextColor(
                resources.getColor(R.color.baf_tab_unselected, null)
            )
            loadTickets()
        }

        binding.tabNeuesTicket.setOnClickListener {
            findNavController().navigate(R.id.action_ticketsFragment_to_newTicketFragment)
        }
    }

    private fun loadTickets() {
        lifecycleScope.launch {
            val tickets = ticketRepository.getMyTickets()
            if (tickets.isNotEmpty()) {
                binding.emptyText.visibility = View.GONE
                val adapter = TicketsAdapter(tickets) { ticket ->
                    // Ticket öffnen
                    val bundle = Bundle().apply {
                        putString("ticketId", ticket.id)
                    }
                    findNavController().navigate(
                        R.id.action_ticketsFragment_to_ticketDetailFragment, bundle
                    )
                }
                binding.ticketsRecyclerView.adapter = adapter
                binding.ticketsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            } else {
                binding.emptyText.visibility = View.VISIBLE
                binding.emptyText.text = "Noch keine Tickets erstellt.\nTippe auf + um ein Ticket zu erstellen."
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
