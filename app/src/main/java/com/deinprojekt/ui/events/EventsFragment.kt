package com.deinprojekt.ui.events

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.deinprojekt.data.repository.EventsRepository
import com.deinprojekt.databinding.FragmentEventsBinding
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
                val first = events.first()

                binding.eventTitle.text = first.name
                binding.eventDescription.text = first.description
            } else {
                binding.eventTitle.text = "Keine Events"
                binding.eventDescription.text = "Es wurden keine Einträge gefunden."
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
