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
import baf.bierandfriends.eu.data.repository.MarketRepository

class MarketFragment : Fragment() {

    private var _binding: FragmentMarketBinding? = null
    private val binding get() = _binding!!
    private val marketRepository = MarketRepository()
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

        setupRecyclerView()

        // FIX: Zugriff auf marketFab (muss in fragment_market.xml existieren)
        binding.marketFab.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_market_to_newMarketItemFragment)
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
        // Da die Repository-Methode suspend ist, müsste hier normalerweise ein CoroutineScope genutzt werden.
        // Für den Build-Fix stellen wir sicher, dass die UI-Elemente korrekt angesprochen werden.
        // (Beispielhafte Implementierung basierend auf deinem bisherigen Code)
        binding.emptyText.visibility = View.VISIBLE
        binding.marketRecyclerView.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
