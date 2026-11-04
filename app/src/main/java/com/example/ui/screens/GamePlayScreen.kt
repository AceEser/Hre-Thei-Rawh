package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Player
import com.example.data.model.RoomStatus
import com.example.ui.GameViewModel
import com.example.ui.components.DrawingCanvasSection
import com.example.ui.components.FloatingReactionsOverlay
import com.example.ui.components.GuessChatView
import com.example.ui.components.WordSelectionDialog
import com.example.ui.theme.ArcadeBackgroundDark
import com.example.ui.theme.ArcadeCoral
import com.example.ui.theme.ArcadeCyan
import com.example.ui.theme.ArcadeGreen
import com.example.ui.theme.ArcadeSurfaceDark
import com.example.ui.theme.ArcadeYellow

@Composable
fun GamePlayScreen(viewModel: GameViewModel) {
    val currentRoom by viewModel.currentRoom.collectAsState()
    val players by viewModel.players.collectAsState()
    val strokes by viewModel.strokes.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val reactions by viewModel.reactions.collectAsState()
    val wordChoices by viewModel.wordChoices.collectAsState()
    val currentUid = viewModel.currentUserId

    val room = currentRoom ?: return
    val isDrawer = room.currentDrawerId == currentUid
    val me = players.find { it.userId == currentUid }
    val hasGuessedCorrectly = me?.hasGuessedThisRound ?: false

    // Pulsing timer animation when under 10 seconds
    val timerPulse = remember { Animatable(1f) }
    LaunchedEffect(room.roundTimeRemaining <= 10) {
        if (room.roundTimeRemaining <= 10 && room.roundTimeRemaining > 0) {
            timerPulse.animateTo(
                targetValue = 1.25f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 400),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            timerPulse.snapTo(1f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArcadeBackgroundDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // TOP STATUS BAR: Timer, Word Hint / Secret Word, Round Counter, Leave
            GameTopBar(
                room = room,
                isDrawer = isDrawer,
                timeRemaining = room.roundTimeRemaining,
                timerScale = timerPulse.value,
                onLeave = { viewModel.leaveRoom() }
            )

            // PLAYERS SCORE RAIL (Horizontal chips)
            PlayersScoreRail(players = players, currentDrawerId = room.currentDrawerId)

            // MAIN DRAWING CANVAS
            DrawingCanvasSection(
                isDrawingEnabled = isDrawer && room.status == RoomStatus.DRAWING.name,
                strokes = strokes,
                currentUserId = currentUid,
                onStrokeDrawn = { stroke -> viewModel.sendStroke(stroke) },
                onUndo = { viewModel.undoStroke() },
                onClear = { viewModel.clearCanvas() },
                modifier = Modifier.weight(1f)
            )

            // GUESSING CHAT & STICKERS
            GuessChatView(
                messages = messages,
                isDrawer = isDrawer,
                hasGuessedCorrectly = hasGuessedCorrectly,
                onSendMessage = { guess -> viewModel.submitGuess(guess) },
                onSendSticker = { sticker -> viewModel.sendSticker(sticker) }
            )
        }

        // FLOATING EMOJI REACTIONS OVERLAY
        FloatingReactionsOverlay(
            reactions = reactions,
            modifier = Modifier.fillMaxSize()
        )

        // WORD SELECTION MODAL (When Drawer must pick word)
        if (room.status == RoomStatus.SELECTING_WORD.name) {
            WordSelectionDialog(
                isDrawer = isDrawer,
                drawerNickname = room.currentDrawerNickname,
                choices = wordChoices,
                onWordSelected = { word -> viewModel.selectWord(word) }
            )
        }
    }
}

@Composable
private fun GameTopBar(
    room: com.example.data.model.Room,
    isDrawer: Boolean,
    timeRemaining: Int,
    timerScale: Float,
    onLeave: () -> Unit
) {
    Surface(
        color = ArcadeSurfaceDark,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF382C75)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Countdown Timer
            Surface(
                color = if (timeRemaining <= 10) ArcadeCoral.copy(alpha = 0.25f) else Color(0xFF261D52),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (timeRemaining <= 10) ArcadeCoral else ArcadeCyan
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Timer",
                        tint = if (timeRemaining <= 10) ArcadeCoral else ArcadeCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${timeRemaining}s",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = if (timeRemaining <= 10) ArcadeCoral else ArcadeCyan
                    )
                }
            }

            // Word Hint (or Secret Word for Drawer)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                if (isDrawer) {
                    Text(
                        text = "DRAW THIS:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ArcadeYellow
                    )
                    Text(
                        text = room.secretWord.uppercase().ifBlank { "..." },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "GUESS THE WORD (${room.currentWordLength} letters):",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC4BFDF)
                    )
                    Text(
                        text = room.revealedHint.ifBlank { "_ ".repeat(room.currentWordLength.coerceAtLeast(3)).trim() },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = ArcadeYellow
                    )
                }
            }

            // Leave Icon Button
            IconButton(
                onClick = onLeave,
                modifier = Modifier.size(32.dp).testTag("leave_game_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Leave",
                    tint = Color(0xFF7E76A3),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun PlayersScoreRail(
    players: List<Player>,
    currentDrawerId: String
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(players) { player ->
            val isCurrentDrawer = player.userId == currentDrawerId
            Surface(
                color = if (isCurrentDrawer) Color(0xFF382C75) else ArcadeSurfaceDark,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isCurrentDrawer) ArcadeYellow else if (player.hasGuessedThisRound) ArcadeGreen else Color(0xFF261D52)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Small avatar
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCurrentDrawer) ArcadeYellow else Color(0xFF261D52)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isCurrentDrawer) "✏️" else player.nickname.take(1).uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Column {
                        Text(
                            text = player.nickname,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            text = "${player.score} pts",
                            fontSize = 10.sp,
                            color = ArcadeGreen
                        )
                    }

                    if (player.hasGuessedThisRound) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Guessed Correctly",
                            tint = ArcadeGreen,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
