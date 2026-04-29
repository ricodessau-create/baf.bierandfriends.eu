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
import baf.bierandfriends.eu.data.repository.UserRepository
import baf.bierandfriends.eu.databinding.FragmentCommunityBinding
import baf.bierandfriends.eu.ui.user.UserAdapter
import kotlinx.coroutines.launch

class CommunityFragment : Fragment() {

    private var _binding: FragmentCommunityBinding? = null
    private val binding get() = _binding!!

    private val forumRepository = ForumRepository()
    private val userRepository = UserRepository()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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
        fun resetTabs() {
            listOf(binding.tabFeed, binding.tabGruppen, binding.tabMitglieder, binding.tabChat).forEach {
                it.setTextColor(resources.getColor(R.color.baf_tab_unselected, null))
            }
        }

        binding.tabFeed.setOnClickListener {
            resetTabs()
            binding.tabFeed.setTextColor(resources.getColor(R.color.baf_gold, null))
            binding.forumRecyclerView.visibility = View.VISIBLE
            binding.newPostButton.visibility = View.VISIBLE
            loadPosts()
        }

        binding.tabChat.setOnClickListener {
            findNavController().navigate(R.id.action_communityFragment_to_chatFragment)
        }

        binding.tabGruppen.setOnClickListener {
            resetTabs()
            binding.tabGruppen.setTextColor(resources.getColor(R.color.baf_gold, null))
            binding.forumRecyclerView.visibility = View.GONE
            binding.newPostButton.visibility = View.GONE
            binding.emptyText.text = "Gruppen-Feature kommt bald!"
            binding.emptyText.visibility = View.VISIBLE
        }

        binding.tabMitglieder.setOnClickListener {
            resetTabs()
            binding.tabMitglieder.setTextColor(resources.getColor(R.color.baf_gold, null))
            binding.newPostButton.visibility = View.GONE
            loadMembers()
        }
    }

    private fun loadPosts() {
        binding.emptyText.visibility = View.GONE
        lifecycleScope.launch {
            val posts = forumRepository.getLatestPosts()
            if (posts.isNotEmpty()) {
                val adapter = ForumAdapter(posts) { post ->
                    val action = CommunityFragmentDirections
                        .actionCommunityFragmentToPostDetailFragment(post.id)
                    findNavController().navigate(action)
                }
                binding.forumRecyclerView.adapter = adapter
                binding.forumRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                binding.forumRecyclerView.visibility = View.VISIBLE
            } else {
                binding.emptyText.text = "Noch keine Beiträge."
                binding.emptyText.visibility = View.VISIBLE
            }
        }
    }

    private fun loadMembers() {
        lifecycleScope.launch {
            val users = userRepository.getAllUsers()
            if (users.isNotEmpty()) {
                binding.emptyText.visibility = View.GONE
                val adapter = UserAdapter(users) { user ->
                    val action = CommunityFragmentDirections
                        .actionCommunityFragmentToUserProfileFragment(user.username)
                    findNavController().navigate(action)
                }
                binding.forumRecyclerView.adapter = adapter
                binding.forumRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                binding.forumRecyclerView.visibility = View.VISIBLE
            } else {
                binding.emptyText.text = "Keine Mitglieder gefunden."
                binding.emptyText.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
