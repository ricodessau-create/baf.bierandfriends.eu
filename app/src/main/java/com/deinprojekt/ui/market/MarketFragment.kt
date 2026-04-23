package com.deinprojekt.ui.market

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
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

        loadMarketItems()
    }

    private fun loadMarketItems() {
        lifecycleScope.launch {
            val items = marketRepository.getMarketItems()

            if (items.isNotEmpty()) {
                val first = items.first()

                binding.marketTitle.text = first.title
                binding.marketDescription.text = first.description
                binding.marketPrice.text = "${first.price} €"
                binding.marketOwner.text = "Anbieter: ${first.ownerUuid}"
            } else {
                binding.marketTitle.text = "Keine Angebote"
                binding.marketDescription.text = "Es wurden keine Einträge gefunden."
                binding.marketPrice.text = ""
                binding.marketOwner.text = ""
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
