package baf.bierandfriends.eu.ui.community

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import baf.bierandfriends.eu.data.models.ForumPost
import baf.bierandfriends.eu.databinding.ItemForumPostBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ForumAdapter(
    private val items: List<ForumPost>,
    private val onClick: (ForumPost) -> Unit
) : RecyclerView.Adapter<ForumAdapter.ForumViewHolder>() {

    inner class ForumViewHolder(val binding: ItemForumPostBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForumViewHolder {
        val binding = ItemForumPostBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ForumViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ForumViewHolder, position: Int) {
        val post = items[position]
        holder.binding.postTitle.text = post.title
        holder.binding.postContent.text = post.content
        holder.binding.postAuthor.text = post.author

        val dateText = post.createdAt?.let {
            SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMAN)
                .format(Date(it.seconds * 1000))
        } ?: ""
        holder.binding.postDate.text = dateText

        holder.binding.root.setOnClickListener { onClick(post) }
    }

    override fun getItemCount(): Int = items.size
}
