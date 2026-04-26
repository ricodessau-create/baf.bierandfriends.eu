package baf.bierandfriends.eu.ui.tickets

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import baf.bierandfriends.eu.data.models.Ticket
import baf.bierandfriends.eu.databinding.ItemTicketBinding

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
        holder.binding.ticketStatus.text = ticket.status.replaceFirstChar { it.uppercase() }
        holder.binding.ticketAuthor.text = ticket.authorName
        holder.binding.root.setOnClickListener { onClick(ticket) }
    }

    override fun getItemCount(): Int = items.size
}
