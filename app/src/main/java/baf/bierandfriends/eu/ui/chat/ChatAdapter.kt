package baf.bierandfriends.eu.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import baf.bierandfriends.eu.data.models.ChatMessage
import baf.bierandfriends.eu.databinding.ItemChatMessageBinding
import baf.bierandfriends.eu.util.RankHelper

class ChatAdapter(
    private val messages: List<ChatMessage>,
    private val currentUid: String
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(val binding: ItemChatMessageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatMessageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val msg = messages[position]
        val isOwn = msg.authorUid == currentUid

        holder.binding.chatAuthor.text = if (isOwn) "Du" else msg.authorName
        holder.binding.chatText.text = msg.text

        val rankColor = RankHelper.getRankColor(holder.itemView.context, msg.authorRank)
        holder.binding.chatAuthor.setTextColor(rankColor)

        if (isOwn) {
            holder.binding.chatBubble.setBackgroundColor(
                holder.itemView.context.getColor(baf.bierandfriends.eu.R.color.baf_gold_dark)
            )
        } else {
            holder.binding.chatBubble.setBackgroundColor(
                holder.itemView.context.getColor(baf.bierandfriends.eu.R.color.baf_card)
            )
        }
    }

    override fun getItemCount(): Int = messages.size
}
