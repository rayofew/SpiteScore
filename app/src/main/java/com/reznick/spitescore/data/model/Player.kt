package com.reznick.spitescore.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Player(
    val id: String,
    val name: String,
    val seat: Int
)
