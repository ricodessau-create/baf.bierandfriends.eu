package com.deinprojekt.ui.market

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.deinprojekt.data.repository.MarketRepository
import com.deinprojekt.databinding.FragmentMarketBinding
import kotlinx.coroutines.launch

class MarketFragment : Fragment() {

    private var _binding: FragmentMarketBinding? = null
    private val binding get() = _binding!!

    private val marketRepository = MarketRepository()

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

        binding.marketRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        loadMarketItems()

        binding.marketFab.setOnClickListener {
            findNavController().navigate(
                R.id.action_marketFragment_to_marketCreateFragment
            )
        }
    }

    private fun loadMarketItems() {
        lifecycleScope.launch {
            val items = marketRepository.getMarketItems()

            binding.marketRecyclerView.adapter = MarketAdapter(items) { itemId ->
                val action = MarketFragmentDirections
                    .actionMarketFragmentToMarketDetailFragment(itemId)

                findNavController().navigate(action)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
