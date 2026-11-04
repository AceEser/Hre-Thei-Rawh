package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Player
import com.example.ui.GameViewModel
import com.example.ui.theme.ArcadeBackgroundDark
import com.example.ui.theme.ArcadeCoral
import com.example.ui.theme.ArcadeCyan
import com.example.ui.theme.ArcadeGreen
import com.example.ui.theme.ArcadeSurfaceDark
import com.example.ui.theme.ArcadeYellow

@Composable
fun LobbyScreen(viewModel: GameViewModel) {
    val context = LocalContext.current
    val currentRoom by viewModel.currentRoom.collectAsState()
    val players by viewModel.players.collectAsState()
    val currentUid = viewModel.currentUserId

    val room = currentRoom ?: return
    val isHost = room.hostId == currentUid

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArcadeBackgroundDark)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar with Leave Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LOBBY",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = ArcadeCyan,
                    letterSpacing = 1.sp
                )

                Button(
                    onClick = { viewModel.leaveRoom() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF261D52)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Leave", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Room Code Card (Tap to Copy)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Hre Thei Rawh Room Code", room.roomCode)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Room Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = ArcadeSurfaceDark),
                border = androidx.compose.foundation.BorderStroke(2.dp, ArcadeCyan)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ROOM CODE (TAP TO COPY)",
                        color = Color(0xFFC4BFDF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = room.roomCode,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            color = ArcadeYellow,
                            letterSpacing = 4.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Code",
                            tint = ArcadeCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Duration: ${room.matchDurationMinutes} Minutes",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ArcadeGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Connected Players List
            Text(
                text = "Players in Room (${players.size})",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(players) { player ->
                    PlayerLobbyItem(
                        player = player,
                        isCurrentPlayer = (player.userId == currentUid),
                        canKick = isHost && player.userId != currentUid,
                        onKick = { viewModel.kickPlayer(player.userId) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Start Match / Waiting Banner
            if (isHost) {
                Button(
                    onClick = { viewModel.startGame() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("start_match_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ArcadeCoral,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Start Game",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            } else {
                Surface(
                    color = Color(0xFF261D52),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF382C75)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = ArcadeCyan,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Waiting for host to start the match...",
                            color = Color(0xFFC4BFDF),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerLobbyItem(
    player: Player,
    isCurrentPlayer: Boolean,
    canKick: Boolean,
    onKick: () -> Unit
) {
    Surface(
        color = ArcadeSurfaceDark,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isCurrentPlayer) ArcadeCyan else Color(0xFF382C75)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar circle
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF261D52))
                        .border(1.5.dp, ArcadeYellow, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = player.nickname.take(1).uppercase(),
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = player.nickname + if (isCurrentPlayer) " (You)" else "",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        if (player.isHost) {
                            Surface(
                                color = ArcadeYellow.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Host",
                                        tint = ArcadeYellow,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "HOST",
                                        color = ArcadeYellow,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }

                    if (player.isSpectator) {
                        Text(
                            text = "Joined mid-round (Spectating)",
                            color = Color(0xFF7E76A3),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (canKick) {
                IconButton(
                    onClick = onKick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Kick Player",
                        tint = ArcadeCoral,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
