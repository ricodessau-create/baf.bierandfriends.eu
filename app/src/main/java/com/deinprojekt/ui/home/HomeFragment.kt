package com.deinprojekt.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.deinprojekt.R
import com.deinprojekt.data.repository.NewsRepository
import com.deinprojekt.databinding.FragmentHomeBinding
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
                val first = newsList.first()

                binding.homeTitle.text = first.title
                binding.homeContent.text = first.content
            } else {
                binding.homeTitle.text = "Keine News"
                binding.homeContent.text = "Es wurden keine Einträge gefunden."
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
