package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.WordChoice
import com.example.data.model.WordDictionary
import com.example.ui.theme.ArcadeCoral
import com.example.ui.theme.ArcadeCyan
import com.example.ui.theme.ArcadeGreen
import com.example.ui.theme.ArcadeSurfaceDark
import com.example.ui.theme.ArcadeYellow

@Composable
fun WordSelectionDialog(
    isDrawer: Boolean,
    drawerNickname: String,
    choices: List<WordChoice>,
    onWordSelected: (String) -> Unit
) {
    var showCustomInput by remember { mutableStateOf(false) }
    var customWordText by remember { mutableStateOf("") }
    var customWordError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = { /* Non-dismissible while selecting word */ }) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ArcadeSurfaceDark),
            border = androidx.compose.foundation.BorderStroke(2.dp, ArcadeCyan)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isDrawer) {
                    // Drawer selection UI
                    Text(
                        text = "🎨 Pick a Word to Draw",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = ArcadeYellow,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Choose a difficulty or enter your own custom secret word!",
                        fontSize = 13.sp,
                        color = Color(0xFFC4BFDF),
                        textAlign = TextAlign.Center
                    )

                    choices.forEach { choice ->
                        val badgeColor = when (choice.difficulty) {
                            "Easy" -> ArcadeGreen
                            "Medium" -> ArcadeYellow
                            else -> ArcadeCoral
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.5.dp, badgeColor.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                                .clickable { onWordSelected(choice.word) },
                            color = Color(0xFF261D52)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = choice.word,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = choice.hintCategory,
                                        fontSize = 11.sp,
                                        color = Color(0xFF7E76A3)
                                    )
                                }

                                Surface(
                                    color = badgeColor.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = choice.difficulty,
                                        color = badgeColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Custom word toggle
                    if (!showCustomInput) {
                        Button(
                            onClick = { showCustomInput = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF382C75),
                                contentColor = ArcadeCyan
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("use_my_own_word_button")
                        ) {
                            Icon(Icons.Default.Create, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Use My Own Word")
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = customWordText,
                                onValueChange = {
                                    customWordText = it
                                    customWordError = null
                                },
                                label = { Text("Custom Secret Word") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ArcadeCyan,
                                    unfocusedBorderColor = Color(0xFF382C75),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (customWordError != null) {
                                Text(
                                    text = customWordError ?: "",
                                    color = ArcadeCoral,
                                    fontSize = 12.sp
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showCustomInput = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF261D52)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Back")
                                }

                                Button(
                                    onClick = {
                                        val (isValid, err) = WordDictionary.validateCustomWord(customWordText)
                                        if (isValid) {
                                            onWordSelected(customWordText.trim())
                                        } else {
                                            customWordError = err
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ArcadeCyan,
                                        contentColor = Color(0xFF00363D)
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Confirm")
                                }
                            }
                        }
                    }

                } else {
                    // Non-drawer waiting UI
                    CircularProgressIndicator(
                        color = ArcadeCyan,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(48.dp)
                    )

                    Text(
                        text = "⏳ $drawerNickname is choosing a word...",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ArcadeYellow,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Get your fingers ready to guess fast for the highest points!",
                        fontSize = 13.sp,
                        color = Color(0xFFC4BFDF),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
