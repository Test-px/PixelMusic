package com.saurav.pixelmusic.data.model.youtube

import kotlinx.serialization.Serializable

@Serializable
data class StoredYoutubeAccount(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val handle: String = "",
    val avatarUrl: String = "",
    val cookie: String = "",
    val dataSyncId: String = "",
    val isPro: Boolean = false
)
