package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.data.model.MessageType
import com.example.ui.theme.ArcadeCoral
import com.example.ui.theme.ArcadeCyan
import com.example.ui.theme.ArcadeGreen
import com.example.ui.theme.ArcadeSurfaceDark
import com.example.ui.theme.ArcadeYellow

val GameStickers = listOf(
    "🔥", "😂", "👏", "🤔", "❤️", "🎉", "👍", "😭", "😲", "😕"
)

@Composable
fun GuessChatView(
    messages: List<ChatMessage>,
    isDrawer: Boolean,
    hasGuessedCorrectly: Boolean,
    onSendMessage: (String) -> Unit,
    onSendSticker: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ArcadeSurfaceDark)
            .border(1.dp, Color(0xFF382C75), RoundedCornerShape(16.dp))
            .padding(10.dp)
    ) {
        // Chat Header with Quick Sticker Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "💬 Live Guesses & Chat",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = ArcadeCyan
            )

            // Sticker toolbar
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(GameStickers) { sticker ->
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF261D52))
                            .clickable { onSendSticker(sticker) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = sticker, fontSize = 14.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Message List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp, max = 150.dp)
                .background(Color(0xFF130E29), RoundedCornerShape(10.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Text(
                        text = "No guesses yet. Type your guess below!",
                        color = Color(0xFF7E76A3),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }

            items(messages) { msg ->
                MessageBubble(message = msg)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Guess Input Field
        val canType = !isDrawer && !hasGuessedCorrectly
        val placeholderText = when {
            isDrawer -> "You are drawing! Chat locked."
            hasGuessedCorrectly -> "🎉 You guessed correctly! Awesome!"
            else -> "Type your guess here..."
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                enabled = canType,
                placeholder = {
                    Text(text = placeholderText, fontSize = 12.sp, color = Color(0xFF7E76A3))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (textInput.isNotBlank()) {
                            onSendMessage(textInput.trim())
                            textInput = ""
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ArcadeCyan,
                    unfocusedBorderColor = Color(0xFF382C75),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    disabledBorderColor = Color(0xFF261D52),
                    disabledTextColor = Color(0xFF7E76A3)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("guess_input_field")
            )

            IconButton(
                onClick = {
                    if (textInput.isNotBlank()) {
                        onSendMessage(textInput.trim())
                        textInput = ""
                    }
                },
                enabled = canType && textInput.isNotBlank(),
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (canType && textInput.isNotBlank()) ArcadeCyan else Color(0xFF261D52))
                    .testTag("send_guess_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send Guess",
                    tint = if (canType && textInput.isNotBlank()) Color(0xFF00363D) else Color(0xFF7E76A3),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    when (message.type) {
        MessageType.CORRECT_GUESS.name -> {
            Surface(
                color = Color(0x3300E676),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ArcadeGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🎉 ${message.senderNickname} guessed the word!",
                    color = ArcadeGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        MessageType.STICKER.name -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${message.senderNickname}:",
                    fontWeight = FontWeight.SemiBold,
                    color = ArcadeYellow,
                    fontSize = 12.sp
                )
                Text(
                    text = message.content,
                    fontSize = 20.sp
                )
            }
        }
        MessageType.SYSTEM.name -> {
            Text(
                text = "🔔 ${message.content}",
                color = ArcadeCoral,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
        else -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${message.senderNickname}:",
                    fontWeight = FontWeight.SemiBold,
                    color = ArcadeCyan,
                    fontSize = 12.sp
                )
                Text(
                    text = message.content,
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }
    }
}
