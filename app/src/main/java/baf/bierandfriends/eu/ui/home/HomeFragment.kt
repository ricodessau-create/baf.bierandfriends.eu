package baf.bierandfriends.eu.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import baf.bierandfriends.eu.data.models.News
import baf.bierandfriends.eu.databinding.ItemNewsBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NewsAdapter(private val items: List<News>) :
    RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    inner class NewsViewHolder(val binding: ItemNewsBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val binding = ItemNewsBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return NewsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val news = items[position]
        holder.binding.newsTitle.text = news.title
        holder.binding.newsContent.text = news.content
        val date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMAN)
            .format(Date(news.timestamp))
        holder.binding.newsDate.text = date
    }

    override fun getItemCount(): Int = items.size
    }
