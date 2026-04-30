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
    private var currentTypeFilter = "verkauf"
    private var currentCategoryFilter = "alle"

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

        setupTypeTabs()
        setupCategoryFilters()
        loadItems("verkauf")

        binding.marketFab.setOnClickListener {
            findNavController().navigate(R.id.action_marketFragment_to_marketCreateFragment)
        }

        binding.marketSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { applyFilters() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupTypeTabs() {
        binding.tabVerkaufen.setOnClickListener {
            currentTypeFilter = "verkauf"
            currentCategoryFilter = "alle"
            binding.tabVerkaufen.setTextColor(resources.getColor(R.color.baf_gold, null))
            binding.tabKaufen.setTextColor(resources.getColor(R.color.baf_tab_unselected, null))
            binding.marketFab.visibility = View.VISIBLE
            resetCategoryButtons()
            loadItems("verkauf")
        }

        binding.tabKaufen.setOnClickListener {
            currentTypeFilter = "kauf"
            currentCategoryFilter = "alle"
            binding.tabKaufen.setTextColor(resources.getColor(R.color.baf_gold, null))
            binding.tabVerkaufen.setTextColor(resources.getColor(R.color.baf_tab_unselected, null))
            binding.marketFab.visibility = View.GONE
            resetCategoryButtons()
            loadItems("kauf")
        }
    }

    private fun setupCategoryFilters() {
        val gold = resources.getColor(R.color.baf_gold, null)
        val unselected = resources.getColor(R.color.baf_tab_unselected, null)

        val filterMap = mapOf(
            binding.filterAll to "alle",
            binding.filterBloecke to "bloecke",
            binding.filterRuestung to "ruestung",
            binding.filterWerkzeuge to "werkzeuge",
            binding.filterGrundstuecke to "grundstuecke",
            binding.filterSonstiges to "sonstiges"
        )

        filterMap.forEach { (button, category) ->
            button.setOnClickListener {
                currentCategoryFilter = category
                // Alle zurücksetzen
                filterMap.keys.forEach { btn ->
                    btn.setTextColor(unselected)
                    btn.backgroundTintList = android.content.res.ColorStateList.valueOf(
                        resources.getColor(R.color.baf_card, null)
                    )
                }
                // Ausgewählten hervorheben
                button.setTextColor(resources.getColor(R.color.baf_black, null))
                button.backgroundTintList = android.content.res.ColorStateList.valueOf(gold)
                applyFilters()
            }
        }
    }

    private fun resetCategoryButtons() {
        val gold = resources.getColor(R.color.baf_gold, null)
        val card = resources.getColor(R.color.baf_card, null)
        val black = resources.getColor(R.color.baf_black, null)

        listOf(
            binding.filterBloecke, binding.filterRuestung,
            binding.filterWerkzeuge, binding.filterGrundstuecke, binding.filterSonstiges
        ).forEach { btn ->
            btn.setTextColor(gold)
            btn.backgroundTintList = android.content.res.ColorStateList.valueOf(card)
        }
        // "Alle" aktiv
        binding.filterAll.setTextColor(black)
        binding.filterAll.backgroundTintList = android.content.res.ColorStateList.valueOf(gold)
    }

    private fun loadItems(type: String) {
        lifecycleScope.launch {
            allItems = marketRepository.getMarketItems().filter { it.type == type }
            applyFilters()
        }
    }

    private fun applyFilters() {
        val query = binding.marketSearch.text.toString().lowercase()

        var filtered = allItems

        if (currentCategoryFilter != "alle") {
            filtered = filtered.filter { it.category == currentCategoryFilter }
        }

        if (query.isNotEmpty()) {
            filtered = filtered.filter {
                it.title.lowercase().contains(query) ||
                it.description.lowercase().contains(query)
            }
        }

        if (filtered.isNotEmpty()) {
            binding.emptyText.visibility = View.GONE
            updateAdapter(filtered)
        } else {
            binding.emptyText.visibility = View.VISIBLE
            binding.emptyText.text = "Keine Angebote gefunden."
        }
    }

    private fun updateAdapter(items: List<MarketItem>) {
        val adapter = MarketAdapter(items) { itemId ->
            val bundle = Bundle().apply { putString("itemId", itemId) }
            findNavController().navigate(
                R.id.action_marketFragment_to_marketDetailFragment, bundle
            )
        }
        binding.marketRecyclerView.adapter = adapter
        binding.marketRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
