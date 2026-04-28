package baf.bierandfriends.eu.ui.community

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import baf.bierandfriends.eu.R
import baf.bierandfriends.eu.data.repository.ForumRepository
import baf.bierandfriends.eu.databinding.FragmentCommunityBinding
import kotlinx.coroutines.launch

class CommunityFragment : Fragment() {

    private var _binding: FragmentCommunityBinding? = null
    private val binding get() = _binding!!

    private val forumRepository = ForumRepository()
    private var currentTab = "feed"

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

        setupTabs()
        loadPosts()

        binding.newPostButton.setOnClickListener {
            findNavController().navigate(R.id.action_communityFragment_to_newPostFragment)
        }
    }

    private fun setupTabs() {
        binding.tabFeed.setOnClickListener {
            currentTab = "feed"
            binding.tabFeed.setTextColor(resources.getColor(R.color.baf_gold, null))
            binding.tabGruppen.setTextColor(resources.getColor(R.color.baf_tab_unselected, null))
            binding.tabMitglieder.setTextColor(resources.getColor(R.color.baf_tab_unselected, null))
            binding.tabChat.setTextColor(resources.getColor(R.color.baf_tab_unselected, null))
            binding.forumRecyclerView.visibility = View.VISIBLE
            loadPosts()
        }

        binding.tabChat.setOnClickListener {
            findNavController().navigate(R.id.action_communityFragment_to_chatFragment)
        }

        binding.tabGruppen.setOnClickListener {
            binding.tabGruppen.setTextColor(resources.getColor(R.color.baf_gold, null))
            binding.tabFeed.setTextColor(resources.getColor(R.color.baf_tab_unselected, null))
        }

        binding.tabMitglieder.setOnClickListener {
            binding.tabMitglieder.setTextColor(resources.getColor(R.color.baf_gold, null))
            binding.tabFeed.setTextColor(resources.getColor(R.color.baf_tab_unselected, null))
        }
    }

    private fun loadPosts() {
        lifecycleScope.launch {
            val posts = forumRepository.getLatestPosts()
            if (posts.isNotEmpty()) {
                binding.emptyText.visibility = View.GONE
                val adapter = ForumAdapter(posts) { post ->
                    val action = CommunityFragmentDirections
                        .actionCommunityFragmentToPostDetailFragment(post.id)
                    findNavController().navigate(action)
                }
                binding.forumRecyclerView.adapter = adapter
                binding.forumRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            } else {
                binding.emptyText.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
