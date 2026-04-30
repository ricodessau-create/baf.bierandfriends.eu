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
import baf.bierandfriends.eu.data.repository.UserRepository
import baf.bierandfriends.eu.databinding.MarketCreateFragmentBinding
import com.bumptech.glide.Glide
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.UUID

class MarketCreateFragment : Fragment() {

    private var _binding: MarketCreateFragmentBinding? = null
    private val binding get() = _binding!!

    private val repo = MarketRepository()
    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()
    private var selectedImageUri: Uri? = null
    private var selectedType = "verkauf"
    private var selectedCategory = "sonstiges"

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
            val uri = result.data?.data ?: return@registerForActivityResult
            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                )
            } catch (e: Exception) {}
            selectedImageUri = uri
            binding.createImagePlaceholder.visibility = View.GONE
            binding.createImagePreview.visibility = View.VISIBLE
            Glide.with(this).load(uri).into(binding.createImagePreview)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = MarketCreateFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTypeTabs()
        setupCategoryButtons()
        binding.createSelectImageButton.setOnClickListener { checkPermissionAndOpenPicker() }
        binding.createSaveButton.setOnClickListener { saveItem() }
    }

    private fun setupTypeTabs() {
        fun setActive(isVerkauf: Boolean) {
            val gold = android.content.res.ColorStateList.valueOf(
                resources.getColor(baf.bierandfriends.eu.R.color.baf_gold, null)
            )
            val card = android.content.res.ColorStateList.valueOf(
                resources.getColor(baf.bierandfriends.eu.R.color.baf_card, null)
            )
            val black = resources.getColor(baf.bierandfriends.eu.R.color.baf_black, null)
            val goldColor = resources.getColor(baf.bierandfriends.eu.R.color.baf_gold, null)

            binding.typeVerkauf.backgroundTintList = if (isVerkauf) gold else card
            binding.typeVerkauf.setTextColor(if (isVerkauf) black else goldColor)
            binding.typeKauf.backgroundTintList = if (!isVerkauf) gold else card
            binding.typeKauf.setTextColor(if (!isVerkauf) black else goldColor)
        }
        binding.typeVerkauf.setOnClickListener { selectedType = "verkauf"; setActive(true) }
        binding.typeKauf.setOnClickListener { selectedType = "kauf"; setActive(false) }
    }

    private fun setupCategoryButtons() {
        val gold = android.content.res.ColorStateList.valueOf(
            resources.getColor(baf.bierandfriends.eu.R.color.baf_gold, null)
        )
        val card = android.content.res.ColorStateList.valueOf(
            resources.getColor(baf.bierandfriends.eu.R.color.baf_card, null)
        )
        val black = resources.getColor(baf.bierandfriends.eu.R.color.baf_black, null)
        val goldColor = resources.getColor(baf.bierandfriends.eu.R.color.baf_gold, null)

        val buttons = listOf(
            binding.catBloecke to "bloecke",
            binding.catRuestung to "ruestung",
            binding.catWerkzeuge to "werkzeuge",
            binding.catGrundstuecke to "grundstuecke",
            binding.catSonstiges to "sonstiges"
        )

        buttons.forEach { (btn, cat) ->
            btn.setOnClickListener {
                selectedCategory = cat
                buttons.forEach { (b, _) ->
                    b.backgroundTintList = card
                    b.setTextColor(goldColor)
                }
                btn.backgroundTintList = gold
                btn.setTextColor(black)
            }
        }
    }

    private fun checkPermissionAndOpenPicker() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(requireContext(), permission)
            == PackageManager.PERMISSION_GRANTED
        ) openImagePicker()
        else permissionLauncher.launch(permission)
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
        }
        imagePicker.launch(intent)
    }

    private fun saveItem() {
        val title = binding.createTitle.text.toString().trim()
        val description = binding.createDescription.text.toString().trim()
        val price = binding.createPrice.text.toString().trim().toDoubleOrNull()

        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Bitte Titel eingeben.", Toast.LENGTH_SHORT).show()
            return
        }
        if (description.isEmpty()) {
            Toast.makeText(requireContext(), "Bitte Beschreibung eingeben.", Toast.LENGTH_SHORT).show()
            return
        }
        if (price == null || price <= 0) {
            Toast.makeText(requireContext(), "Bitte gültigen Preis eingeben.", Toast.LENGTH_SHORT).show()
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
                val profile = userRepository.getUserProfile()
                val ownerName = profile?.username ?: "Unbekannt"

                // Bild hochladen via MarketRepository → SupabaseHelper
                val imageUrl = selectedImageUri?.let { uri ->
                    val bytes = requireContext().contentResolver.openInputStream(uri)
                        ?.use { it.readBytes() }
                        ?: throw Exception("Bild nicht lesbar")
                    repo.uploadImage(bytes, id)
                }

                val item = MarketItem(
                    id = id,
                    createdAt = Timestamp.now(),
                    description = description,
                    ownerUuid = ownerUuid,
                    ownerName = ownerName,
                    price = price,
                    title = title,
                    imageUrl = imageUrl,
                    category = selectedCategory,
                    type = selectedType
                )

                repo.createMarketItem(item)
                Toast.makeText(requireContext(), "✅ Angebot erstellt!", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            } catch (e: Exception) {
                binding.createSaveButton.isEnabled = true
                binding.createSaveButton.text = "ANGEBOT ERSTELLEN"
                Toast.makeText(
                    requireContext(), "Fehler: ${e.message}", Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
