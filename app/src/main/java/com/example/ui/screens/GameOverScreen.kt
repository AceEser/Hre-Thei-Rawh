package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.GameViewModel
import com.example.ui.components.GameOverPodiumView
import com.example.ui.theme.ArcadeBackgroundDark

@Composable
fun GameOverScreen(viewModel: GameViewModel) {
    val players by viewModel.players.collectAsState()
    val currentRoom by viewModel.currentRoom.collectAsState()
    val currentUid = viewModel.currentUserId

    val room = currentRoom ?: return
    val isHost = room.hostId == currentUid

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArcadeBackgroundDark)
    ) {
        GameOverPodiumView(
            players = players,
            isHost = isHost,
            onPlayAgain = { viewModel.playAgain() },
            onReturnHome = { viewModel.returnHome() }
        )
    }
}
