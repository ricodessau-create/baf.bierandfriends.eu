package baf.bierandfriends.eu.ui.user

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import baf.bierandfriends.eu.data.models.UserProfile
import baf.bierandfriends.eu.databinding.ItemUserBinding
import baf.bierandfriends.eu.util.RankHelper
import com.bumptech.glide.Glide

class UserAdapter(
    private val users: List<UserProfile>,
    private val onClick: (UserProfile) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.binding.userName.text = user.username
        holder.binding.userRank.text = RankHelper.getRankDisplayName(user.rank)
        holder.binding.userRank.setTextColor(RankHelper.getRankColor(holder.itemView.context, user.rank))

        if (user.photoUrl.isNotEmpty()) {
            Glide.with(holder.itemView).load(user.photoUrl).circleCrop().into(holder.binding.userAvatar)
        }

        holder.binding.root.setOnClickListener { onClick(user) }
    }

    override fun getItemCount() = users.size
}
