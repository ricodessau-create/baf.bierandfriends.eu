package com.deinprojekt.ui.market

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.deinprojekt.data.repository.MarketRepository
import com.deinprojekt.databinding.MarketDetailFragmentBinding
import kotlinx.coroutines.launch

class MarketDetailFragment : Fragment() {

    private var _binding: MarketDetailFragmentBinding? = null
    private val binding get() = _binding!!

    private val marketRepository = MarketRepository()
    private var currentItemId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MarketDetailFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val itemId = arguments?.getString("itemId")
        currentItemId = itemId

        if (itemId != null) {
            loadItem(itemId)
        }

        binding.detailDeleteButton.setOnClickListener {
            showDeleteDialog()
        }
    }

    private fun loadItem(id: String) {
        lifecycleScope.launch {
            val item = marketRepository.getMarketItemById(id)

            if (item != null) {
                binding.detailTitle.text = item.title
                binding.detailDescription.text = item.description
                binding.detailPrice.text = "${item.price} €"
                binding.detailOwner.text = "Anbieter: ${item.ownerUuid}"

                if (item.imageUrl != null) {
                    binding.detailImage.visibility = View.VISIBLE
                    binding.detailImage.setImageURI(android.net.Uri.parse(item.imageUrl))
                }
            } else {
                binding.detailTitle.text = "Nicht gefunden"
                binding.detailDescription.text = "Dieses Angebot existiert nicht mehr."
                binding.detailPrice.text = ""
                binding.detailOwner.text = ""
            }
        }
    }

    private fun showDeleteDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Löschen?")
            .setMessage("Willst du dieses Angebot wirklich löschen?")
            .setPositiveButton("Ja") { _, _ ->
                deleteItem()
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    private fun deleteItem() {
        val id = currentItemId ?: return

        lifecycleScope.launch {
            marketRepository.deleteMarketItem(id)
            marketRepository.deleteImage(id)
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
