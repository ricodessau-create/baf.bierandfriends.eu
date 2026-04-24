package com.deinprojekt.ui.market

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.deinprojekt.data.models.MarketItem
import com.deinprojekt.databinding.MarketListItemBinding

class MarketAdapter(
    private val items: List<MarketItem>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<MarketAdapter.MarketViewHolder>() {

    inner class MarketViewHolder(val binding: MarketListItemBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarketViewHolder {
        val binding = MarketListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MarketViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MarketViewHolder, position: Int) {
        val item = items[position]

        holder.binding.itemTitle.text = item.title
        holder.binding.itemDescription.text = item.description
        holder.binding.itemPrice.text = "${item.price} €"
        holder.binding.itemOwner.text = "Anbieter: ${item.ownerUuid}"

        if (item.imageUrl != null) {
            Glide.with(holder.itemView)
                .load(item.imageUrl)
                .into(holder.binding.itemImage)
        }

        holder.binding.root.setOnClickListener {
            onClick(item.id)
        }
    }

    override fun getItemCount(): Int = items.size
}
