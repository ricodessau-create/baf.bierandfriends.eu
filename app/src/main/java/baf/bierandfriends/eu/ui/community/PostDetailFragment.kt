package baf.bierandfriends.eu.ui.community

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import baf.bierandfriends.eu.data.repository.ForumRepository
import baf.bierandfriends.eu.databinding.FragmentPostDetailBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostDetailFragment : Fragment() {

    private var _binding: FragmentPostDetailBinding? = null
    private val binding get() = _binding!!

    private val forumRepository = ForumRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val postId = arguments?.getString("postId") ?: return

        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        loadPost(postId)
    }

    private fun loadPost(postId: String) {
        lifecycleScope.launch {
            val posts = forumRepository.getLatestPosts()
            val post = posts.find { it.id == postId }

            if (post != null) {
                binding.detailTitle.text = post.title
                binding.detailContent.text = post.content
                binding.detailAuthor.text = "Von: ${post.author}"

                val dateText = post.createdAt?.let {
                    SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMAN)
                        .format(Date(it.seconds * 1000))
                } ?: ""
                binding.detailDate.text = dateText
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
