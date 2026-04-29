package baf.bierandfriends.eu.data.models

import com.google.firebase.Timestamp

data class MarketItem(
    val id: String = "",
    val createdAt: Timestamp? = null,
    val description: String = "",
    val ownerUuid: String = "",
    val ownerName: String = "",
    val price: Double = 0.0,
    val title: String = "",
    val imageUrl: String? = null,
    val category: String = "sonstiges",
    val type: String = "verkauf"
)
