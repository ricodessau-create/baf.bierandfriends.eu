package baf.bierandfriends.eu.ui.events

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import baf.bierandfriends.eu.data.repository.EventsRepository
import baf.bierandfriends.eu.databinding.FragmentEventsBinding
import kotlinx.coroutines.launch

class EventsFragment : Fragment() {

    private var _binding: FragmentEventsBinding? = null
    private val binding get() = _binding!!

    private val eventsRepository = EventsRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadEvents()
    }

    private fun loadEvents() {
        lifecycleScope.launch {
            val events = eventsRepository.getUpcomingEvents()

            if (events.isNotEmpty()) {
                val adapter = EventsAdapter(events)
                binding.eventsRecyclerView.adapter = adapter
                binding.eventsRecyclerView.layoutManager =
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
