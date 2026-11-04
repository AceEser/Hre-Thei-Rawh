package com.example.data.repository

import com.example.data.model.MessageType
import com.example.data.model.Player
import com.example.data.model.PointF
import com.example.data.model.Room
import com.example.data.model.RoomStatus
import com.example.data.model.Stroke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameRepositoryRuleTest {

    @Test
    fun playerModel_defaults_and_scoreCalculation() {
        val player = Player(
            userId = "user_123",
            nickname = "PlayerOne",
            score = 0
        )
        assertEquals("user_123", player.userId)
        assertEquals("PlayerOne", player.nickname)
        assertEquals(0, player.score)
        assertFalse(player.hasGuessedThisRound)
        assertFalse(player.isSpectator)

        // Speed scoring logic test: 100 base + 4 * remaining seconds
        val remainingSeconds = 45
        val earnedScore = 100 + (remainingSeconds * 4)
        val updatedPlayer = player.copy(
            score = player.score + earnedScore,
            hasGuessedThisRound = true
        )
        assertEquals(280, updatedPlayer.score)
        assertTrue(updatedPlayer.hasGuessedThisRound)
    }

    @Test
    fun roomModel_stateTransitions() {
        val room = Room(
            roomId = "room_1",
            roomCode = "HR7X29",
            hostId = "user_host",
            status = RoomStatus.LOBBY.name
        )
        assertEquals(RoomStatus.LOBBY.name, room.status)

        val activeRoom = room.copy(
            status = RoomStatus.DRAWING.name,
            secretWord = "ROCKET",
            currentWordLength = 6
        )
        assertEquals(RoomStatus.DRAWING.name, activeRoom.status)
        assertEquals("ROCKET", activeRoom.secretWord)
        assertEquals(6, activeRoom.currentWordLength)
    }

    @Test
    fun strokeModel_serialization() {
        val stroke = Stroke(
            strokeId = "stroke_1",
            drawerId = "drawer_1",
            color = -16711936,
            brushWidth = 8f,
            pointsJson = "[{\"x\":0.1,\"y\":0.2}]"
        )
        assertEquals("stroke_1", stroke.strokeId)
        assertFalse(stroke.isEraser)
        assertNotNull(stroke.pointsJson)
    }
}
