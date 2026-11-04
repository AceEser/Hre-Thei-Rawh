package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Player
import com.example.ui.theme.ArcadeCoral
import com.example.ui.theme.ArcadeCyan
import com.example.ui.theme.ArcadeGreen
import com.example.ui.theme.ArcadeSurfaceDark
import com.example.ui.theme.ArcadeYellow
import kotlin.random.Random

val GoldMedal = Color(0xFFFFD700)
val SilverMedal = Color(0xFFC0C0C0)
val BronzeMedal = Color(0xFFCD7F32)

@Composable
fun GameOverPodiumView(
    players: List<Player>,
    isHost: Boolean,
    onPlayAgain: () -> Unit,
    onReturnHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sortedPlayers = remember(players) { players.sortedByDescending { it.score } }
    val firstPlace = sortedPlayers.getOrNull(0)
    val secondPlace = sortedPlayers.getOrNull(1)
    val thirdPlace = sortedPlayers.getOrNull(2)

    Box(modifier = modifier.fillMaxSize()) {
        // Confetti Canvas in background
        ConfettiCanvas(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Trophy Header
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = "Trophy",
                tint = GoldMedal,
                modifier = Modifier.size(54.dp)
            )

            Text(
                text = "MATCH COMPLETED!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = ArcadeYellow,
                letterSpacing = 1.sp
            )

            Text(
                text = "Hre Thei Rawh Champions",
                fontSize = 13.sp,
                color = Color(0xFFC4BFDF),
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 3D-styled Podium (2nd Place, 1st Place, 3rd Place)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                // 2nd Place (Silver)
                if (secondPlace != null) {
                    PodiumPillar(
                        player = secondPlace,
                        rank = 2,
                        rankLabel = "2nd",
                        pillarHeight = 110.dp,
                        pillarColor = Color(0xFF78909C),
                        accentColor = SilverMedal,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 1st Place (Gold)
                if (firstPlace != null) {
                    PodiumPillar(
                        player = firstPlace,
                        rank = 1,
                        rankLabel = "1st",
                        pillarHeight = 150.dp,
                        pillarColor = Color(0xFFE65100),
                        accentColor = GoldMedal,
                        modifier = Modifier.weight(1.15f)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 3rd Place (Bronze)
                if (thirdPlace != null) {
                    PodiumPillar(
                        player = thirdPlace,
                        rank = 3,
                        rankLabel = "3rd",
                        pillarHeight = 85.dp,
                        pillarColor = Color(0xFF5D4037),
                        accentColor = BronzeMedal,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Complete Leaderboard Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = ArcadeSurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF382C75))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Leaderboard",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ArcadeCyan
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(sortedPlayers) { index, player ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF261D52))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "#${index + 1}",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = when (index) {
                                            0 -> GoldMedal
                                            1 -> SilverMedal
                                            2 -> BronzeMedal
                                            else -> Color(0xFF7E76A3)
                                        }
                                    )
                                    Text(
                                        text = player.nickname,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                }

                                Text(
                                    text = "${player.score} pts",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = ArcadeGreen
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons: Play Again & Return Home
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onReturnHome,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF261D52)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Home")
                }

                if (isHost) {
                    Button(
                        onClick = onPlayAgain,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ArcadeCyan,
                            contentColor = Color(0xFF00363D)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("play_again_button")
                    ) {
                        Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play Again", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PodiumPillar(
    player: Player,
    rank: Int,
    rankLabel: String,
    pillarHeight: androidx.compose.ui.unit.Dp,
    pillarColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Player Avatar & Nickname
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(accentColor)
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = player.nickname.take(1).uppercase(),
                fontWeight = FontWeight.Black,
                color = Color.Black,
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = player.nickname,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            textAlign = TextAlign.Center
        )

        Text(
            text = "${player.score} pts",
            fontSize = 11.sp,
            color = ArcadeGreen,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Pillar block
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(pillarHeight)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(pillarColor)
                .border(1.5.dp, accentColor, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = rankLabel,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = accentColor,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun ConfettiCanvas(modifier: Modifier = Modifier) {
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
    }

    val confettiList = remember {
        val colors = listOf(ArcadeCyan, ArcadeCoral, ArcadeYellow, ArcadeGreen, GoldMedal, Color.White)
        List(40) {
            ConfettiParticle(
                xFrac = Random.nextFloat(),
                speed = Random.nextFloat() * 0.4f + 0.6f,
                size = Random.nextFloat() * 6f + 4f,
                color = colors.random()
            )
        }
    }

    Canvas(modifier = modifier) {
        val p = animProgress.value
        confettiList.forEach { c ->
            val y = ((p * c.speed + c.xFrac) % 1f) * size.height
            val x = (c.xFrac * size.width) + kotlin.math.sin((p * 10f) + c.xFrac * 5f) * 20f
            drawCircle(
                color = c.color,
                radius = c.size,
                center = Offset(x, y)
            )
        }
    }
}

private data class ConfettiParticle(
    val xFrac: Float,
    val speed: Float,
    val size: Float,
    val color: Color
)
