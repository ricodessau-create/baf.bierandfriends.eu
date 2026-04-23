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
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MarketFragment : Fragment() {

    private var _binding: FragmentMarketBinding? = null
    private val binding get() = _binding!!

    private val repo = MarketRepository()
    private val auth = FirebaseAuth.getInstance()

    private var allItems: List<MarketItem> = emptyList()
    private var filteredItems: List<MarketItem> = emptyList()

    private var filterOwn = false
    private var filterWithImage = false
    private var sortPriceAsc = false

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

        loadItems()
        setupSearch()
        setupFilters()

        binding.marketFab.setOnClickListener {
            findNavController().navigate(
                R.id.action_marketFragment_to_marketCreateFragment
            )
        }
    }

    private fun loadItems() {
        lifecycleScope.launch {
            allItems = repo.getMarketItems()
            applyFilters()
        }
    }

    private fun setupSearch() {
        binding.marketSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                applyFilters()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupFilters() {

        binding.filterOwn.setOnClickListener {
            filterOwn = !filterOwn
            applyFilters()
        }

        binding.filterWithImage.setOnClickListener {
            filterWithImage = !filterWithImage
            applyFilters()
        }

        binding.sortPrice.setOnClickListener {
            sortPriceAsc = !sortPriceAsc
            applyFilters()
        }

        binding.sortNewest.setOnClickListener {
            filteredItems = filteredItems.sortedByDescending { it.createdAt?.seconds ?: 0 }
            updateList(filteredItems)
        }
    }

    private fun applyFilters() {
        val query = binding.marketSearch.text.toString().trim()

        filteredItems = allItems.filter { item ->

            val matchesSearch =
                item.title.contains(query, ignoreCase = true) ||
                item.description.contains(query, ignoreCase = true)

            val matchesOwn =
                !filterOwn || item.ownerUuid == auth.currentUser?.uid

            val matchesImage =
                !filterWithImage || item.imageUrl != null

            matchesSearch && matchesOwn && matchesImage
        }

        if (sortPriceAsc) {
            filteredItems = filteredItems.sortedBy { it.price }
        }

        updateList(filteredItems)
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
