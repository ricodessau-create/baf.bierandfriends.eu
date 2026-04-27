package baf.bierandfriends.eu.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import baf.bierandfriends.eu.R
import baf.bierandfriends.eu.data.repository.EventsRepository
import baf.bierandfriends.eu.data.repository.ForumRepository
import baf.bierandfriends.eu.data.repository.MarketRepository
import baf.bierandfriends.eu.data.repository.NewsRepository
import baf.bierandfriends.eu.data.repository.TicketRepository
import baf.bierandfriends.eu.databinding.FragmentHomeBinding
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val newsRepository = NewsRepository()
    private val eventsRepository = EventsRepository()
    private val forumRepository = ForumRepository()
    private val ticketRepository = TicketRepository()
    private val marketRepository = MarketRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.profileIcon.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_profileFragment)
        }

        loadNews()
        loadStats()
    }

    private fun loadNews() {
        lifecycleScope.launch {
            val newsList = newsRepository.getLatestNews()
            if (newsList.isNotEmpty()) {
                binding.emptyText.visibility = View.GONE
                val adapter = NewsAdapter(newsList)
                binding.newsRecyclerView.adapter = adapter
                binding.newsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            } else {
                binding.emptyText.visibility = View.VISIBLE
            }
        }
    }

    private fun loadStats() {
        lifecycleScope.launch {
            val events = eventsRepository.getUpcomingEvents()
            binding.statEventsCount.text = events.size.toString()
        }
        lifecycleScope.launch {
            val posts = forumRepository.getLatestPosts()
            binding.statPostsCount.text = posts.size.toString()
        }
        lifecycleScope.launch {
            val tickets = ticketRepository.getMyTickets()
            val open = tickets.count { it.status == "offen" }
            binding.statTicketsCount.text = open.toString()
        }
        lifecycleScope.launch {
            val items = marketRepository.getMarketItems()
            binding.statMarketCount.text = items.size.toString()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
