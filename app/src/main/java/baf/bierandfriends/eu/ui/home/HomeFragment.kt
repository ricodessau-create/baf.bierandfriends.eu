package baf.bierandfriends.eu.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val newsRepository = NewsRepository()
    private val eventsRepository = EventsRepository()
    private val forumRepository = ForumRepository()
    private val ticketRepository = TicketRepository()
    private val marketRepository = MarketRepository()

    // Deine Minecraft Server IP hier eintragen
    private val serverIp = "baf.bierandfriends.eu"

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
        loadServerStatus()
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

    private fun loadServerStatus() {
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val url = URL("https://api.mcsrvstat.us/2/$serverIp")
                    val connection = url.openConnection()
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    connection.getInputStream().bufferedReader().readText()
                }

                val json = JSONObject(result)
                val online = json.optBoolean("online", false)

                if (online) {
                    val players = json.optJSONObject("players")
                    val online_count = players?.optInt("online", 0) ?: 0
                    val max = players?.optInt("max", 0) ?: 0

                    binding.serverStatusText.text = "Online"
                    binding.serverStatusText.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.baf_green)
                    )
                    binding.serverStatusDot.setBackgroundColor(
                        ContextCompat.getColor(requireContext(), R.color.baf_green)
                    )
                    binding.serverPlayersText.text = "$online_count/$max"
                } else {
                    binding.serverStatusText.text = "Offline"
                    binding.serverStatusText.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.baf_red)
                    )
                    binding.serverStatusDot.setBackgroundColor(
                        ContextCompat.getColor(requireContext(), R.color.baf_red)
                    )
                    binding.serverPlayersText.text = "0/0"
                }
            } catch (e: Exception) {
                binding.serverStatusText.text = "Nicht erreichbar"
                binding.serverStatusText.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.baf_text_secondary)
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
