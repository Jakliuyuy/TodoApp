package com.yuanbao.todo.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val done: Boolean = false,
    val important: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class Filter { ALL, ACTIVE, DONE }
