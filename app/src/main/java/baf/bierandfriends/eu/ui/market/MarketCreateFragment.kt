package baf.bierandfriends.eu.ui.market

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import baf.bierandfriends.eu.data.models.MarketItem
import baf.bierandfriends.eu.data.repository.MarketRepository
import baf.bierandfriends.eu.databinding.MarketCreateFragmentBinding
import com.bumptech.glide.Glide
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class MarketCreateFragment : Fragment() {

    private var _binding: MarketCreateFragmentBinding? = null
    private val binding get() = _binding!!

    private val repo = MarketRepository()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private var selectedImageUri: Uri? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) openImagePicker()
        else Toast.makeText(requireContext(), "Berechtigung verweigert.", Toast.LENGTH_SHORT).show()
    }

    private val imagePicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            if (selectedImageUri != null) {
                binding.createImagePlaceholder.visibility = View.GONE
                binding.createImagePreview.visibility = View.VISIBLE
                Glide.with(this)
                    .load(selectedImageUri)
                    .into(binding.createImagePreview)
            }
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
            checkPermissionAndOpenPicker()
        }

        binding.createSaveButton.setOnClickListener {
            saveItem()
        }
    }

    private fun checkPermissionAndOpenPicker() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED -> {
                openImagePicker()
            }
            else -> {
                permissionLauncher.launch(permission)
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        imagePicker.launch(intent)
    }

    private fun saveItem() {
        val title = binding.createTitle.text.toString().trim()
        val description = binding.createDescription.text.toString().trim()
        val priceText = binding.createPrice.text.toString().trim()

        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Bitte einen Titel eingeben.", Toast.LENGTH_SHORT).show()
            return
        }

        if (description.isEmpty()) {
            Toast.makeText(requireContext(), "Bitte eine Beschreibung eingeben.", Toast.LENGTH_SHORT).show()
            return
        }

        val price = priceText.toDoubleOrNull()
        if (price == null || price <= 0) {
            Toast.makeText(requireContext(), "Bitte einen gültigen Preis eingeben.", Toast.LENGTH_SHORT).show()
            return
        }

        val ownerUuid = auth.currentUser?.uid ?: run {
            Toast.makeText(requireContext(), "Nicht eingeloggt.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.createSaveButton.isEnabled = false
        binding.createSaveButton.text = "Wird gespeichert..."

        val id = UUID.randomUUID().toString()

        lifecycleScope.launch {
            try {
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
            } catch (e: Exception) {
                binding.createSaveButton.isEnabled = true
                binding.createSaveButton.text = "Angebot erstellen"
                Toast.makeText(requireContext(), "Fehler: ${e.message}", Toast.LENGTH_LONG).show()
            }
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
