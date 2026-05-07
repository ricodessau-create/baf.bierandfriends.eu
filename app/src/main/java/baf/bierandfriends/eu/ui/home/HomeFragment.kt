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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val newsRepository = NewsRepository()
    private val eventsRepository = EventsRepository()
    private val forumRepository = ForumRepository()
    private val ticketRepository = TicketRepository()
    private val marketRepository = MarketRepository()

    private val serverIp = "baf.bierandfriends.eu"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.profileIcon.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_profileFragment)
        }

        binding.statEvents.setOnClickListener {
            findNavController().navigate(R.id.eventsFragment)
        }
        binding.statPosts.setOnClickListener {
            findNavController().navigate(R.id.communityFragment)
        }
        binding.statTickets.setOnClickListener {
            findNavController().navigate(R.id.ticketsFragment)
        }
        binding.statMarket.setOnClickListener {
            findNavController().navigate(R.id.marketFragment)
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
            try {
                binding.statEventsCount.text = eventsRepository.getUpcomingEvents().size.toString()
            } catch (e: Exception) { binding.statEventsCount.text = "0" }
        }
        lifecycleScope.launch {
            try {
                binding.statPostsCount.text = forumRepository.getLatestPosts().size.toString()
            } catch (e: Exception) { binding.statPostsCount.text = "0" }
        }
        lifecycleScope.launch {
            try {
                val tickets = ticketRepository.getMyTickets()
                binding.statTicketsCount.text = tickets.count { it.status == "offen" }.toString()
            } catch (e: Exception) { binding.statTicketsCount.text = "0" }
        }
        lifecycleScope.launch {
            try {
                binding.statMarketCount.text = marketRepository.getMarketItems().size.toString()
            } catch (e: Exception) { binding.statMarketCount.text = "0" }
        }
    }

    private fun loadServerStatus() {
        lifecycleScope.launch {
            // 3 Versuche mit verschiedenen APIs
            val online = tryGetServerStatus()
            if (!isAdded || _binding == null) return@launch

            if (online != null) {
                if (online.first) {
                    binding.serverStatusText.text = "Online"
                    binding.serverStatusText.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.baf_green)
                    )
                    binding.serverStatusDot.setBackgroundColor(
                        ContextCompat.getColor(requireContext(), R.color.baf_green)
                    )
                    binding.serverPlayersText.text = "${online.second} Spieler online"
                } else {
                    binding.serverStatusText.text = "Offline"
                    binding.serverStatusText.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.baf_red)
                    )
                    binding.serverStatusDot.setBackgroundColor(
                        ContextCompat.getColor(requireContext(), R.color.baf_red)
                    )
                    binding.serverPlayersText.text = ""
                }
            } else {
                binding.serverStatusText.text = "Nicht erreichbar"
                binding.serverStatusText.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.baf_text_secondary)
                )
                binding.serverPlayersText.text = ""
            }
        }
    }

    /**
     * Versucht den Serverstatus mit 3 Versuchen abzufragen.
     * Gibt Pair(online, playerCount) zurück, oder null bei Fehler.
     */
    private suspend fun tryGetServerStatus(): Pair<Boolean, Int>? {
        // API v3 ist aktueller und zuverlässiger
        val apis = listOf(
            "https://api.mcsrvstat.us/3/$serverIp",
            "https://api.mcsrvstat.us/2/$serverIp",
            "https://mcapi.us/server/status?ip=$serverIp"
        )

        repeat(3) { attempt ->
            try {
                return withContext(Dispatchers.IO) {
                    val url = URL(apis[attempt % apis.size])
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 6000
                    conn.readTimeout = 6000
                    conn.requestMethod = "GET"
                    conn.setRequestProperty("User-Agent", "BAFApp/1.0")

                    val responseCode = conn.responseCode
                    if (responseCode != 200) return@withContext null

                    val text = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(text)

                    // API v3 Format
                    if (json.has("online")) {
                        val isOnline = json.optBoolean("online", false)
                        val players = json.optJSONObject("players")
                        val count = players?.optInt("online", 0) ?: 0
                        return@withContext Pair(isOnline, count)
                    }

                    // mcapi.us Format
                    if (json.has("status")) {
                        val status = json.optString("status", "")
                        val isOnline = status == "online"
                        val players = json.optJSONObject("players")
                        val count = players?.optInt("now", 0) ?: 0
                        return@withContext Pair(isOnline, count)
                    }

                    null
                }
            } catch (e: Exception) {
                if (attempt < 2) delay(1500) // 1.5s warten vor nächstem Versuch
            }
        }
        return null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
