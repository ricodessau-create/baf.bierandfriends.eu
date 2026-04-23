package com.deinprojekt.ui.market

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.deinprojekt.data.models.MarketItem
import com.deinprojekt.data.repository.MarketRepository
import com.deinprojekt.databinding.MarketCreateFragmentBinding
import kotlinx.coroutines.launch
import java.util.UUID

class MarketCreateFragment : Fragment() {

    private var _binding: MarketCreateFragmentBinding? = null
    private val binding get() = _binding!!

    private val repo = MarketRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MarketCreateFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.createSaveButton.setOnClickListener {
            saveItem()
        }
    }

    private fun saveItem() {
        val title = binding.createTitle.text.toString()
        val description = binding.createDescription.text.toString()
        val price = binding.createPrice.text.toString().toDoubleOrNull() ?: 0.0

        val item = MarketItem(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            price = price,
            ownerUuid = "TODO_USER_ID"
        )

        lifecycleScope.launch {
            repo.createMarketItem(item)
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
