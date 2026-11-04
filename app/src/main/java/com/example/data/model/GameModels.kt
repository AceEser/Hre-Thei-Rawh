package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class RoomStatus {
    LOBBY,
    SELECTING_WORD,
    DRAWING,
    ROUND_END,
    GAME_OVER
}

@IgnoreExtraProperties
data class Room(
    @DocumentId val id: String = "",
    val roomId: String = "",
    val roomCode: String = "",
    val hostId: String = "",
    val status: String = RoomStatus.LOBBY.name,
    val matchDurationMinutes: Int = 10,
    val currentRound: Int = 1,
    val totalRounds: Int = 3,
    val currentDrawerId: String = "",
    val currentDrawerNickname: String = "",
    val currentWordLength: Int = 0,
    val revealedHint: String = "",
    val secretWord: String = "",
    val roundTimeRemaining: Int = 60,
    val roundDurationSeconds: Int = 60,
    @ServerTimestamp val roundStartTime: Timestamp? = null,
    @ServerTimestamp val roundEndTime: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Player(
    @DocumentId val id: String = "",
    val userId: String = "",
    val nickname: String = "",
    val avatarColor: String = "#00E5FF",
    val score: Int = 0,
    val isHost: Boolean = false,
    val isSpectator: Boolean = false,
    val hasGuessedThisRound: Boolean = false,
    @ServerTimestamp val joinedAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class PointF(
    val x: Float = 0f,
    val y: Float = 0f
)

@IgnoreExtraProperties
data class Stroke(
    @DocumentId val id: String = "",
    val strokeId: String = "",
    val drawerId: String = "",
    val color: Int = -16711936, // default ARGB
    val brushWidth: Float = 8f,
    val isEraser: Boolean = false,
    val pointsJson: String = "[]",
    @ServerTimestamp val createdAt: Timestamp? = null
)

enum class MessageType {
    CHAT,
    CORRECT_GUESS,
    STICKER,
    SYSTEM
}

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val messageId: String = "",
    val senderId: String = "",
    val senderNickname: String = "",
    val content: String = "",
    val type: String = MessageType.CHAT.name,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Reaction(
    @DocumentId val id: String = "",
    val reactionId: String = "",
    val senderNickname: String = "",
    val emoji: String = "🔥",
    @ServerTimestamp val createdAt: Timestamp? = null
)
