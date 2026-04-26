package baf.bierandfriends.eu.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import baf.bierandfriends.eu.data.repository.NewsRepository
import baf.bierandfriends.eu.databinding.FragmentHomeBinding
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val newsRepository = NewsRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadNews()
    }

    private fun loadNews() {
        lifecycleScope.launch {
            val newsList = newsRepository.getLatestNews()

            if (newsList.isNotEmpty()) {
                val adapter = baf.bierandfriends.eu.ui.home.NewsAdapter(newsList)
                binding.newsRecyclerView.adapter = adapter
                binding.newsRecyclerView.layoutManager =
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
