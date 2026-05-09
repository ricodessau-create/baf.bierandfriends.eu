package baf.bierandfriends.eu.ui.community

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import baf.bierandfriends.eu.data.repository.ForumRepository
import baf.bierandfriends.eu.data.repository.UserRepository
import baf.bierandfriends.eu.databinding.FragmentPostDetailBinding
import baf.bierandfriends.eu.util.RankHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostDetailFragment : Fragment() {

    private var _binding: FragmentPostDetailBinding? = null
    private val binding get() = _binding!!

    private val forumRepository = ForumRepository()
    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val postId = arguments?.getString("postId") ?: return

        binding.backButton.setOnClickListener { findNavController().navigateUp() }

        loadPost(postId)
    }

    private fun loadPost(postId: String) {
        lifecycleScope.launch {
            val profile = userRepository.getUserProfile()
            val myUid = auth.currentUser?.uid ?: ""
            val myRank = profile?.rank ?: ""

            val posts = forumRepository.getLatestPosts()
            val post = posts.find { it.id == postId } ?: return@launch

            binding.detailTitle.text = post.title
            binding.detailContent.text = post.content
            binding.detailAuthor.text = "Von: ${post.author}"

            val dateText = post.createdAt?.let {
                SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMAN)
                    .format(Date(it.seconds * 1000))
            } ?: ""
            binding.detailDate.text = dateText

            // Löschen-Button anzeigen wenn: eigener Post ODER Moderator+
            val canDelete = post.authorUid == myUid || RankHelper.canDeletePosts(myRank)
            if (canDelete) {
                binding.deletePostButton.visibility = View.VISIBLE
                binding.deletePostButton.setOnClickListener {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Beitrag löschen")
                        .setMessage("Möchtest du diesen Beitrag wirklich löschen?")
                        .setPositiveButton("Löschen") { _, _ ->
                            lifecycleScope.launch {
                                try {
                                    forumRepository.deletePost(postId)
                                    Toast.makeText(
                                        requireContext(),
                                        "✅ Beitrag gelöscht.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    findNavController().navigateUp()
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        requireContext(),
                                        "Fehler: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                        .setNegativeButton("Abbrechen", null)
                        .show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
