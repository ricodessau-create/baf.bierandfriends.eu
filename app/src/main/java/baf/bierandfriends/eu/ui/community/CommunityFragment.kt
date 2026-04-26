package baf.bierandfriends.eu.ui.community

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import baf.bierandfriends.eu.data.repository.ForumRepository
import baf.bierandfriends.eu.databinding.FragmentCommunityBinding
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
        loadPosts()

        binding.newPostButton.setOnClickListener {
            findNavController().navigate(R.id.action_communityFragment_to_newPostFragment)
        }
    }

    private fun loadPosts() {
        lifecycleScope.launch {
            val posts = forumRepository.getLatestPosts()

            if (posts.isNotEmpty()) {
                val adapter = ForumAdapter(posts)
                binding.forumRecyclerView.adapter = adapter
                binding.forumRecyclerView.layoutManager =
                    androidx.recyclerview.widget.LinearLayoutManager(requireContext())
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
