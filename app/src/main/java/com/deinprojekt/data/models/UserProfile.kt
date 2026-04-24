package com.deinprojekt.data.models

data class UserProfile(
    val username: String = "",
    val email: String = "",
    val rank_app: String = "Member",
    val rank_ingame: String = "",
    val uuid: String = "",
    val synced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
