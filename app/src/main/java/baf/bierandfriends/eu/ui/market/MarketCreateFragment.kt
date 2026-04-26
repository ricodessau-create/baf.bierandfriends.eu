package baf.bierandfriends.eu.ui.market

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import baf.bierandfriends.eu.data.models.MarketItem
import baf.bierandfriends.eu.data.repository.MarketRepository
import baf.bierandfriends.eu.databinding.MarketCreateFragmentBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import java.util.UUID

class MarketCreateFragment : Fragment() {

    private var _binding: MarketCreateFragmentBinding? = null
    private val binding get() = _binding!!

    private val repo = MarketRepository()
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val storage = FirebaseStorage.getInstance()
    private var selectedImageUri: Uri? = null

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                selectedImageUri = result.data?.data
                binding.createImagePlaceholder.visibility = View.GONE
                binding.createImagePreview.visibility = View.VISIBLE
                binding.createImagePreview.setImageURI(selectedImageUri)
            }
        }

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

        binding.createSelectImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            imagePicker.launch(intent)
        }

        binding.createSaveButton.setOnClickListener {
            saveItem()
        }
    }

    private fun saveItem() {
        val title = binding.createTitle.text.toString().trim()
        val description = binding.createDescription.text.toString().trim()
        val price = binding.createPrice.text.toString().toDoubleOrNull() ?: 0.0

        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Bitte einen Titel eingeben.", Toast.LENGTH_SHORT).show()
            return
        }

        val ownerUuid = auth.currentUser?.uid ?: "unknown"
        val id = UUID.randomUUID().toString()

        lifecycleScope.launch {
            val imageUrl = if (selectedImageUri != null) {
                uploadImage(id, selectedImageUri!!)
            } else null

            val item = MarketItem(
                id = id,
                createdAt = Timestamp.now(),
                description = description,
                ownerUuid = ownerUuid,
                price = price,
                title = title,
                imageUrl = imageUrl
            )

            repo.createMarketItem(item)
            Toast.makeText(requireContext(), "Angebot erstellt!", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    private suspend fun uploadImage(id: String, uri: Uri): String {
        val ref = storage.getReference("market_images/$id.jpg")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
