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
import baf.bierandfriends.eu.data.repository.UserRepository
import baf.bierandfriends.eu.databinding.FragmentTicketsBinding
import baf.bierandfriends.eu.util.RankHelper
import kotlinx.coroutines.launch

class TicketsFragment : Fragment() {

    private var _binding: FragmentTicketsBinding? = null
    private val binding get() = _binding!!

    private val ticketRepository = TicketRepository()
    private val userRepository = UserRepository()
    private var isStaff = false
    private var showingAll = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTicketsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.newTicketButton.setOnClickListener {
            findNavController().navigate(R.id.action_ticketsFragment_to_newTicketFragment)
        }

        lifecycleScope.launch {
            val profile = userRepository.getUserProfile()
            val rank = profile?.rank ?: ""
            isStaff = RankHelper.isStaff(rank)

            setupTabs()
            loadTickets(showAll = false)
        }
    }

    private fun setupTabs() {
        binding.tabMeineTickets.setOnClickListener {
            showingAll = false
            binding.tabMeineTickets.setTextColor(resources.getColor(R.color.baf_gold, null))
            binding.tabNeuesTicket.setTextColor(resources.getColor(R.color.baf_tab_unselected, null))
            loadTickets(showAll = false)
        }

        if (isStaff) {
            // Staff: zweiter Tab zeigt alle Tickets
            binding.tabNeuesTicket.text = "Alle Tickets"
            binding.tabNeuesTicket.setOnClickListener {
                showingAll = true
                binding.tabNeuesTicket.setTextColor(resources.getColor(R.color.baf_gold, null))
                binding.tabMeineTickets.setTextColor(resources.getColor(R.color.baf_tab_unselected, null))
                loadTickets(showAll = true)
            }
            // FAB: Staff kann auch neue Tickets erstellen
            binding.newTicketButton.visibility = View.VISIBLE
        } else {
            binding.tabNeuesTicket.text = "Neues Ticket"
            binding.tabNeuesTicket.setOnClickListener {
                findNavController().navigate(R.id.action_ticketsFragment_to_newTicketFragment)
            }
        }
    }

    private fun loadTickets(showAll: Boolean) {
        lifecycleScope.launch {
            val tickets = if (showAll && isStaff) {
                ticketRepository.getAllTickets()
            } else {
                ticketRepository.getMyTickets()
            }

            if (tickets.isNotEmpty()) {
                binding.emptyText.visibility = View.GONE
                val adapter = TicketsAdapter(tickets) { ticket ->
                    val bundle = Bundle().apply { putString("ticketId", ticket.id) }
                    findNavController().navigate(
                        R.id.action_ticketsFragment_to_ticketDetailFragment, bundle
                    )
                }
                binding.ticketsRecyclerView.adapter = adapter
                binding.ticketsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            } else {
                binding.emptyText.visibility = View.VISIBLE
                binding.emptyText.text = if (showAll)
                    "Keine offenen Tickets vorhanden."
                else
                    "Du hast noch keine Tickets erstellt.\nTippe auf + um ein Ticket zu erstellen."
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
