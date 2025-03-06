package com.example.navigationsolution.data

import kotlinx.serialization.Serializable

@Serializable
data class UserData(
    val uuid: String,
    val username: String,
    val pwd: String,
    val permission: Int,
    val email: String
)
