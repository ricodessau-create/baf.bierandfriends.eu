package baf.bierandfriends.eu.ui.market

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import baf.bierandfriends.eu.R
import baf.bierandfriends.eu.databinding.FragmentMarketBinding
import baf.bierandfriends.eu.repository.MarketRepository

class MarketFragment : Fragment() {

    private var _binding: FragmentMarketBinding? = null
    private val binding get() = _binding!!
    private lateinit var marketRepository: MarketRepository
    private lateinit var marketAdapter: MarketAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMarketBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        marketRepository = MarketRepository()
        setupRecyclerView()

        // Der Click-Listener für das mittige FAB
        binding.marketFab.setOnClickListener {
            // Stelle sicher, dass diese Action in deiner navigation_graph.xml existiert!
            try {
                findNavController().navigate(R.id.action_navigation_market_to_newMarketItemFragment)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        loadMarketItems()
    }

    private fun setupRecyclerView() {
        marketAdapter = MarketAdapter(emptyList())
        binding.marketRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = marketAdapter
        }
    }

    private fun loadMarketItems() {
        marketRepository.getMarketItems { items ->
            if (isAdded) {
                if (items.isEmpty()) {
                    binding.emptyText.visibility = View.VISIBLE
                    binding.marketRecyclerView.visibility = View.GONE
                } else {
                    binding.emptyText.visibility = View.GONE
                    binding.marketRecyclerView.visibility = View.VISIBLE
                    marketAdapter.updateItems(items)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
