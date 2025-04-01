package com.example.navigationsolution.data

import kotlinx.serialization.Serializable

@Serializable
data class UserData(
    val uuid: String,
    val username: String,
    val pwd: String,
    val permission: Int,
    val email: String,
    val building_search_history: List<String> = emptyList() // 添加搜索历史字段【Add search history field】
)
