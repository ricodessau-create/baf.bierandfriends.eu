package baf.bierandfriends.eu.ui.tickets

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import baf.bierandfriends.eu.data.models.Ticket
import baf.bierandfriends.eu.databinding.ItemTicketBinding
import baf.bierandfriends.eu.util.RankHelper

class TicketsAdapter(
    private val items: List<Ticket>,
    private val onClick: (Ticket) -> Unit = {}
) : RecyclerView.Adapter<TicketsAdapter.TicketViewHolder>() {

    inner class TicketViewHolder(val binding: ItemTicketBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketViewHolder {
        val binding = ItemTicketBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TicketViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TicketViewHolder, position: Int) {
        val ticket = items[position]
        holder.binding.ticketTitle.text = ticket.title
        holder.binding.ticketAuthor.text = "Von: ${ticket.authorName}"

        val statusDisplay = when (ticket.status.lowercase()) {
            "offen" -> "🔴 Offen"
            "in bearbeitung" -> "🟡 In Bearbeitung"
            "geschlossen" -> "🟢 Geschlossen"
            else -> ticket.status
        }
        holder.binding.ticketStatus.text = statusDisplay
        holder.binding.ticketOpenButton.setOnClickListener { onClick(ticket) }
        holder.binding.root.setOnClickListener { onClick(ticket) }
    }

    override fun getItemCount(): Int = items.size
}
