package io.biotrace.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "chat_history",
    indices = [
        Index(value = ["timestamp"])
    ]
)
data class ChatMessageEntity(
    @PrimaryKey
    val id: String,

    val role: String, // "USER", "ASSISTANT"

    val content: String,

    val timestamp: Instant,

    @ColumnInfo(name = "tool_calls")
    val toolCalls: String?, // JSON serialized tool calls if any

    val citations: String? // JSON serialized source citations if any
)
