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
import androidx.recyclerview.widget.LinearLayoutManager
import baf.bierandfriends.eu.R
import baf.bierandfriends.eu.data.models.MarketItem
import baf.bierandfriends.eu.data.repository.MarketRepository
import baf.bierandfriends.eu.databinding.FragmentMarketBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MarketFragment : Fragment() {

    private var _binding: FragmentMarketBinding? = null
    private val binding get() = _binding!!

    private val marketRepository = MarketRepository()
    private val auth = FirebaseAuth.getInstance()
    private var allItems = listOf<MarketItem>()
    private var showingMyItems = false

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

        setupTabs()
        loadItems()

        binding.marketFab.setOnClickListener {
            findNavController().navigate(R.id.action_marketFragment_to_marketCreateFragment)
        }

        binding.marketSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterItems(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupTabs() {
        binding.tabVerkaufen.setOnClickListener {
            showingMyItems = false
            binding.tabVerkaufen.setTextColor(resources.getColor(R.color.baf_gold, null))
            binding.tabKaufen.setTextColor(resources.getColor(R.color.baf_tab_unselected, null))
            binding.marketFab.visibility = View.VISIBLE
            loadItems()
        }

        binding.tabKaufen.setOnClickListener {
            showingMyItems = true
            binding.tabKaufen.setTextColor(resources.getColor(R.color.baf_gold, null))
            binding.tabVerkaufen.setTextColor(resources.getColor(R.color.baf_tab_unselected, null))
            binding.marketFab.visibility = View.GONE
            loadMyItems()
        }
    }

    private fun loadItems() {
        lifecycleScope.launch {
            allItems = marketRepository.getMarketItems()
            if (allItems.isNotEmpty()) {
                binding.emptyText.visibility = View.GONE
                updateAdapter(allItems)
            } else {
                binding.emptyText.visibility = View.VISIBLE
            }
        }
    }

    private fun loadMyItems() {
        val uid = auth.currentUser?.uid ?: return
        lifecycleScope.launch {
            allItems = marketRepository.getMarketItems()
            val myItems = allItems.filter { it.ownerUuid == uid }
            if (myItems.isNotEmpty()) {
                binding.emptyText.visibility = View.GONE
                updateAdapter(myItems)
            } else {
                binding.emptyText.text = "Du hast noch keine Angebote erstellt."
                binding.emptyText.visibility = View.VISIBLE
            }
        }
    }

    private fun filterItems(query: String) {
        val filtered = allItems.filter {
            it.title.lowercase().contains(query.lowercase()) ||
            it.description.lowercase().contains(query.lowercase())
        }
        updateAdapter(filtered)
    }

    private fun updateAdapter(items: List<MarketItem>) {
        val adapter = MarketAdapter(items) { itemId ->
            val action = MarketFragmentDirections
                .actionMarketFragmentToMarketDetailFragment(itemId)
            findNavController().navigate(action)
        }
        binding.marketRecyclerView.adapter = adapter
        binding.marketRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
