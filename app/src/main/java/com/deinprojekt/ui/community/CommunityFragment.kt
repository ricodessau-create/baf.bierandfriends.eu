package com.deinprojekt.ui.community

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.deinprojekt.data.repository.ForumRepository
import com.deinprojekt.databinding.FragmentCommunityBinding
import kotlinx.coroutines.launch

class CommunityFragment : Fragment() {

    private var _binding: FragmentCommunityBinding? = null
    private val binding get() = _binding!!

    private val forumRepository = ForumRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCommunityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadForumPosts()
    }

    private fun loadForumPosts() {
        lifecycleScope.launch {
            val posts = forumRepository.getLatestPosts()

            if (posts.isNotEmpty()) {
                val first = posts.first()

                binding.communityTitle.text = first.title
                binding.communityContent.text = first.content
                binding.communityAuthor.text = "von ${first.author}"
            } else {
                binding.communityTitle.text = "Keine Beiträge"
                binding.communityContent.text = "Es wurden keine Einträge gefunden."
                binding.communityAuthor.text = ""
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
