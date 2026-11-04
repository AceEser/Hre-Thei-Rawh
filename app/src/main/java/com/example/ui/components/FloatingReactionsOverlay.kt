package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Reaction
import kotlinx.coroutines.delay
import kotlin.random.Random

data class FloatingParticle(
    val id: String,
    val emoji: String,
    val startXFraction: Float,
    val animY: Animatable<Float, *>,
    val animAlpha: Animatable<Float, *>,
    val swayX: Float
)

@Composable
fun FloatingReactionsOverlay(
    reactions: List<Reaction>,
    modifier: Modifier = Modifier
) {
    val particles = remember { mutableStateListOf<FloatingParticle>() }

    // When new reactions arrive, trigger floating particles
    LaunchedEffect(reactions) {
        if (reactions.isNotEmpty()) {
            val latest = reactions.first()
            val particleId = latest.id.ifBlank { latest.reactionId }
            if (particles.none { it.id == particleId }) {
                val startX = Random.nextFloat() * 0.7f + 0.15f
                val sway = (Random.nextFloat() - 0.5f) * 60f
                val particle = FloatingParticle(
                    id = particleId,
                    emoji = latest.emoji,
                    startXFraction = startX,
                    animY = Animatable(0f),
                    animAlpha = Animatable(1f),
                    swayX = sway
                )
                particles.add(particle)

                // Launch animation
                val duration = 2400
                particle.animY.animateTo(
                    targetValue = -500f,
                    animationSpec = tween(durationMillis = duration, easing = LinearEasing)
                )
                particle.animAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 600, easing = LinearEasing)
                )
                delay(100)
                particles.remove(particle)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        particles.forEach { particle ->
            val yOffset = particle.animY.value.dp
            val xOffset = (particle.swayX * (particle.animY.value / -500f)).dp

            Text(
                text = particle.emoji,
                fontSize = 32.sp,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = ((particle.startXFraction * 800) + xOffset.toPx()).toInt(),
                            y = (1000 + yOffset.toPx()).toInt()
                        )
                    }
                    .alpha(particle.animAlpha.value)
            )
        }
    }
}
