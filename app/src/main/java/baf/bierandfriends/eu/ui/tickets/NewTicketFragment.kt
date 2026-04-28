package baf.bierandfriends.eu.ui.tickets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import baf.bierandfriends.eu.data.models.Ticket
import baf.bierandfriends.eu.data.repository.TicketRepository
import baf.bierandfriends.eu.data.repository.UserRepository
import baf.bierandfriends.eu.databinding.FragmentNewTicketBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class NewTicketFragment : Fragment() {

    private var _binding: FragmentNewTicketBinding? = null
    private val binding get() = _binding!!

    private val ticketRepository = TicketRepository()
    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewTicketBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.submitTicketButton.setOnClickListener { submitTicket() }
    }

    private fun submitTicket() {
        val title = binding.ticketTitleInput.text.toString().trim()
        val description = binding.ticketDescriptionInput.text.toString().trim()

        if (title.isEmpty() || description.isEmpty()) {
            Toast.makeText(requireContext(), "Bitte alle Felder ausfüllen.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val profile = userRepository.getUserProfile()
            val authorName = profile?.username ?: "Unbekannt"
            val authorUid = auth.currentUser?.uid ?: ""

            val ticket = Ticket(
                title = title,
                description = description,
                authorUid = authorUid,
                authorName = authorName,
                status = "offen",
                createdAt = System.currentTimeMillis()
            )

            ticketRepository.createTicket(ticket)
            Toast.makeText(requireContext(), "Ticket erstellt!", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
