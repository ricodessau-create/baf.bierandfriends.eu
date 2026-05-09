package baf.bierandfriends.eu.ui.events

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import baf.bierandfriends.eu.data.models.Event
import baf.bierandfriends.eu.data.repository.EventsRepository
import baf.bierandfriends.eu.data.repository.UserRepository
import baf.bierandfriends.eu.databinding.FragmentEventsBinding
import baf.bierandfriends.eu.util.RankHelper
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.util.Calendar

class EventsFragment : Fragment() {

    private var _binding: FragmentEventsBinding? = null
    private val binding get() = _binding!!

    private val eventsRepository = EventsRepository()
    private val userRepository = UserRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            val profile = userRepository.getUserProfile()
            val rank = profile?.rank ?: ""

            if (RankHelper.canCreateEvents(rank)) {
                binding.createEventButton.visibility = View.VISIBLE
                binding.createEventButton.setOnClickListener { showCreateEventDialog() }
            }

            loadEvents()
        }
    }

    private fun loadEvents() {
        lifecycleScope.launch {
            val events = eventsRepository.getUpcomingEvents()
            if (events.isNotEmpty()) {
                binding.emptyText.visibility = View.GONE
                val adapter = EventsAdapter(events)
                binding.eventsRecyclerView.adapter = adapter
                binding.eventsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            } else {
                binding.emptyText.visibility = View.VISIBLE
                binding.emptyText.text = "Keine Events geplant."
            }
        }
    }

    private fun showCreateEventDialog() {
        val dialogView = layoutInflater.inflate(
            baf.bierandfriends.eu.R.layout.dialog_create_event, null
        )
        val etName = dialogView.findViewById<EditText>(baf.bierandfriends.eu.R.id.eventName)
        val etDesc = dialogView.findViewById<EditText>(baf.bierandfriends.eu.R.id.eventDescription)
        val etDate = dialogView.findViewById<EditText>(baf.bierandfriends.eu.R.id.eventDate)

        var selectedCalendar = Calendar.getInstance()

        etDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, y, m, d ->
                selectedCalendar.set(y, m, d)
                TimePickerDialog(requireContext(), { _, h, min ->
                    selectedCalendar.set(Calendar.HOUR_OF_DAY, h)
                    selectedCalendar.set(Calendar.MINUTE, min)
                    etDate.setText(
                        String.format("%02d.%02d.%d %02d:%02d", d, m + 1, y, h, min)
                    )
                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Neues Event erstellen")
            .setView(dialogView)
            .setPositiveButton("Erstellen") { _, _ ->
                val name = etName.text.toString().trim()
                val desc = etDesc.text.toString().trim()

                if (name.isEmpty()) {
                    Toast.makeText(requireContext(), "Bitte einen Namen eingeben.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                lifecycleScope.launch {
                    try {
                        val event = Event(
                            name = name,
                            description = desc,
                            date = Timestamp(selectedCalendar.time)
                        )
                        eventsRepository.createEvent(event)
                        Toast.makeText(requireContext(), "✅ Event erstellt!", Toast.LENGTH_SHORT).show()
                        loadEvents()
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
