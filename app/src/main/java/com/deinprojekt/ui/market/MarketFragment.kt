package com.deinprojekt.ui.market

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.deinprojekt.data.models.MarketItem
import com.deinprojekt.data.repository.MarketRepository
import com.deinprojekt.databinding.FragmentMarketBinding
import kotlinx.coroutines.launch

class MarketFragment : Fragment() {

    private var _binding: FragmentMarketBinding? = null
    private val binding get() = _binding!!

    private val marketRepository = MarketRepository()

    private var allItems: List<MarketItem> = emptyList()

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
        setupSearch()

        binding.marketFab.setOnClickListener {
            findNavController().navigate(
                R.id.action_marketFragment_to_marketCreateFragment
            )
        }
    }

    private fun loadMarketItems() {
        lifecycleScope.launch {
            allItems = marketRepository.getMarketItems()
            updateList(allItems)
        }
    }

    private fun setupSearch() {
        binding.marketSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterList(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun filterList(query: String) {
        val filtered = allItems.filter { item ->
            item.title.contains(query, ignoreCase = true) ||
            item.description.contains(query, ignoreCase = true)
        }
        updateList(filtered)
    }

    private fun updateList(list: List<MarketItem>) {
        binding.marketRecyclerView.adapter = MarketAdapter(list) { itemId ->
            val action =
                MarketFragmentDirections.actionMarketFragmentToMarketDetailFragment(itemId)
            findNavController().navigate(action)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
