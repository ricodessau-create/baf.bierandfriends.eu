package baf.bierandfriends.eu.ui.market

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import baf.bierandfriends.eu.R
import baf.bierandfriends.eu.data.repository.MarketRepository
import baf.bierandfriends.eu.databinding.FragmentMarketBinding
import kotlinx.coroutines.launch

class MarketFragment : Fragment() {

    private var _binding: FragmentMarketBinding? = null
    private val binding get() = _binding!!

    private val marketRepository = MarketRepository()
    private var allItems = listOf<baf.bierandfriends.eu.data.models.MarketItem>()

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
        loadItems()

        binding.marketFab.setOnClickListener {
            findNavController().navigate(R.id.action_marketFragment_to_marketCreateFragment)
        }

        binding.marketSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().lowercase()
                val filtered = allItems.filter {
                    it.title.lowercase().contains(query) ||
                    it.description.lowercase().contains(query)
                }
                updateAdapter(filtered)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun loadItems() {
        lifecycleScope.launch {
            allItems = marketRepository.getMarketItems()
            if (allItems.isNotEmpty()) {
                updateAdapter(allItems)
            } else {
                binding.emptyText.visibility = View.VISIBLE
            }
        }
    }

    private fun updateAdapter(items: List<baf.bierandfriends.eu.data.models.MarketItem>) {
        val adapter = MarketAdapter(items) { itemId ->
            val action = MarketFragmentDirections
                .actionMarketFragmentToMarketDetailFragment(itemId)
            findNavController().navigate(action)
        }
        binding.marketRecyclerView.adapter = adapter
        binding.marketRecyclerView.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(requireContext())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
