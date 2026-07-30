package baf.bierandfriends.eu.ui.tickets

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import baf.bierandfriends.eu.databinding.ItemTicketMessageBinding

class TicketMessageAdapter(
    initialMessages: List<Map<String, Any>>
) : RecyclerView.Adapter<TicketMessageAdapter.ViewHolder>() {

    private val messages = initialMessages.toMutableList()

    inner class ViewHolder(val binding: ItemTicketMessageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTicketMessageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val msg = messages[position]
        holder.binding.msgAuthor.text = msg["authorName"] as? String ?: "Unbekannt"
        holder.binding.msgText.text = msg["text"] as? String ?: ""
    }

    override fun getItemCount() = messages.size

    fun updateMessages(newMessages: List<Map<String, Any>>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize() = messages.size
            override fun getNewListSize() = newMessages.size

            override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
                val old = messages[oldPos]
                val new = newMessages[newPos]
                val oldId = old["id"] as? String
                val newId = new["id"] as? String
                return if (oldId != null && newId != null) {
                    oldId == newId
                } else {
                    old["authorName"] == new["authorName"] && old["text"] == new["text"]
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
