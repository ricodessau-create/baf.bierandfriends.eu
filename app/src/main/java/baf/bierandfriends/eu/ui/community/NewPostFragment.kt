package baf.bierandfriends.eu.ui.community

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import baf.bierandfriends.eu.data.models.ForumPost
import baf.bierandfriends.eu.data.repository.ForumRepository
import baf.bierandfriends.eu.data.repository.UserRepository
import baf.bierandfriends.eu.databinding.FragmentNewPostBinding
import kotlinx.coroutines.launch

class NewPostFragment : Fragment() {

    private var _binding: FragmentNewPostBinding? = null
    private val binding get() = _binding!!

    private val forumRepository = ForumRepository()
    private val userRepository = UserRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.submitPostButton.setOnClickListener {
            submitPost()
        }
    }

    private fun submitPost() {
        val title = binding.postTitleInput.text.toString().trim()
        val content = binding.postContentInput.text.toString().trim()

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(requireContext(), "Bitte alle Felder ausfüllen.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val profile = userRepository.getUserProfile()
            val authorName = profile?.username ?: "Unbekannt"

            val post = ForumPost(
                title = title,
                content = content,
                author = authorName,
                createdAt = System.currentTimeMillis()
            )

            forumRepository.createPost(post)
            Toast.makeText(requireContext(), "Beitrag erstellt!", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
