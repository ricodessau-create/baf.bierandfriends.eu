package baf.bierandfriends.eu.ui.tickets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import baf.bierandfriends.eu.data.repository.TicketRepository
import baf.bierandfriends.eu.databinding.FragmentTicketsBinding
import kotlinx.coroutines.launch

class TicketsFragment : Fragment() {

    private var _binding: FragmentTicketsBinding? = null
    private val binding get() = _binding!!

    private val ticketRepository = TicketRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTicketsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadTickets()

        binding.newTicketButton.setOnClickListener {
            findNavController().navigate(R.id.action_ticketsFragment_to_newTicketFragment)
        }
    }

    private fun loadTickets() {
        lifecycleScope.launch {
            val tickets = ticketRepository.getMyTickets()

            if (tickets.isNotEmpty()) {
                val adapter = TicketsAdapter(tickets)
                binding.ticketsRecyclerView.adapter = adapter
                binding.ticketsRecyclerView.layoutManager =
                    androidx.recyclerview.widget.LinearLayoutManager(requireContext())
            } else {
                binding.emptyText.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
