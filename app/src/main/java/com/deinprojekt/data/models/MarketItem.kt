package com.deinprojekt.data.models

import com.google.firebase.Timestamp

data class MarketItem(
    val id: String = "",
    val createdAt: Timestamp? = null,
    val description: String = "",
    val ownerUuid: String = "",
    val price: Double = 0.0,
    val title: String = ""
)
