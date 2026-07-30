package baf.bierandfriends.eu.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import baf.bierandfriends.eu.data.models.ChatMessage
import baf.bierandfriends.eu.databinding.ItemChatMessageBinding
import baf.bierandfriends.eu.util.RankHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(
    initialMessages: List<ChatMessage>,
    private val currentUid: String,
    private val onNameClick: ((authorName: String) -> Unit)? = null
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    private val messages = initialMessages.toMutableList()

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMAN)

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

        val tsMillis = msg.createdAt?.toDate()?.time
        holder.binding.chatTimestamp.text = if (tsMillis != null) {
            dateFormat.format(Date(tsMillis))
        } else {
            ""
        }

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

        if (!isOwn && onNameClick != null) {
            holder.binding.chatAuthor.setOnClickListener {
                onNameClick.invoke(msg.authorName)
            }
        } else {
            holder.binding.chatAuthor.setOnClickListener(null)
        }
    }

    override fun getItemCount(): Int = messages.size

    fun getCurrentList(): List<ChatMessage> = messages

    fun updateMessages(newMessages: List<ChatMessage>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize() = messages.size
            override fun getNewListSize() = newMessages.size

            override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
                val old = messages[oldPos]
                val new = newMessages[newPos]
                return if (old.id.isNotEmpty() && new.id.isNotEmpty()) {
                    old.id == new.id
                } else {
                    old.authorUid == new.authorUid &&
                        old.text == new.text &&
                        old.createdAt == new.createdAt
                }
            }

            override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
                return messages[oldPos] == newMessages[newPos]
            }
        }

        val diffResult = DiffUtil.calculateDiff(diffCallback)
        messages.clear()
        messages.addAll(newMessages)
        diffResult.dispatchUpdatesTo(this)
    }
}
