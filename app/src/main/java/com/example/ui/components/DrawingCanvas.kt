package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke as CanvasStroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PointF
import com.example.data.model.Stroke
import com.example.ui.theme.ArcadeCoral
import com.example.ui.theme.ArcadeCyan
import com.example.ui.theme.ArcadeSurfaceDark
import com.example.ui.theme.ArcadeYellow
import com.example.ui.theme.DrawingPaletteColors
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

@Composable
fun DrawingCanvasSection(
    isDrawingEnabled: Boolean,
    strokes: List<Stroke>,
    currentUserId: String,
    onStrokeDrawn: (Stroke) -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentColor by remember { mutableStateOf(Color.Black) }
    var currentBrushWidth by remember { mutableStateOf(8f) }
    var isEraser by remember { mutableStateOf(false) }

    // Live points for current active in-progress drag
    val currentDragPoints = remember { mutableStateListOf<PointF>() }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Main Drawing Canvas (16:10 or 4:3 aspect ratio card)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.25f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(2.5.dp, if (isDrawingEnabled) ArcadeCyan else Color(0xFF382C75), RoundedCornerShape(16.dp))
                .shadow(8.dp, RoundedCornerShape(16.dp))
        ) {
            val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
            val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("drawing_canvas")
                    .then(
                        if (isDrawingEnabled) {
                            Modifier.pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        currentDragPoints.clear()
                                        currentDragPoints.add(
                                            PointF(
                                                x = (offset.x / widthPx).coerceIn(0f, 1f),
                                                y = (offset.y / heightPx).coerceIn(0f, 1f)
                                            )
                                        )
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        currentDragPoints.add(
                                            PointF(
                                                x = (change.position.x / widthPx).coerceIn(0f, 1f),
                                                y = (change.position.y / heightPx).coerceIn(0f, 1f)
                                            )
                                        )
                                    },
                                    onDragEnd = {
                                        if (currentDragPoints.isNotEmpty()) {
                                            // Serialize points to JSON
                                            val pointsArray = JSONArray()
                                            currentDragPoints.forEach { pt ->
                                                pointsArray.put(
                                                    JSONObject().apply {
                                                        put("x", pt.x.toDouble())
                                                        put("y", pt.y.toDouble())
                                                    }
                                                )
                                            }

                                            val finalColor = if (isEraser) Color.White.toArgb() else currentColor.toArgb()
                                            val newStroke = Stroke(
                                                strokeId = UUID.randomUUID().toString(),
                                                drawerId = currentUserId,
                                                color = finalColor,
                                                brushWidth = if (isEraser) currentBrushWidth * 2f else currentBrushWidth,
                                                isEraser = isEraser,
                                                pointsJson = pointsArray.toString()
                                            )
                                            onStrokeDrawn(newStroke)
                                            currentDragPoints.clear()
                                        }
                                    },
                                    onDragCancel = {
                                        currentDragPoints.clear()
                                    }
                                )
                            }
                        } else Modifier
                    )
            ) {
                // 1. Render all committed strokes from Firestore
                strokes.forEach { stroke ->
                    val strokeColor = Color(stroke.color)
                    val strokeWidthPx = stroke.brushWidth * density

                    try {
                        val array = JSONArray(stroke.pointsJson)
                        if (array.length() > 0) {
                            val path = Path()
                            val firstObj = array.getJSONObject(0)
                            val firstX = (firstObj.getDouble("x") * size.width).toFloat()
                            val firstY = (firstObj.getDouble("y") * size.height).toFloat()
                            path.moveTo(firstX, firstY)

                            for (i in 1 until array.length()) {
                                val obj = array.getJSONObject(i)
                                val x = (obj.getDouble("x") * size.width).toFloat()
                                val y = (obj.getDouble("y") * size.height).toFloat()
                                path.lineTo(x, y)
                            }

                            drawPath(
                                path = path,
                                color = strokeColor,
                                style = CanvasStroke(
                                    width = strokeWidthPx,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    } catch (e: Exception) {
                        // ignore malformed stroke points
                    }
                }

                // 2. Render live ongoing drag points for the active drawer
                if (currentDragPoints.isNotEmpty()) {
                    val activeColor = if (isEraser) Color.White else currentColor
                    val activeWidthPx = (if (isEraser) currentBrushWidth * 2f else currentBrushWidth) * density
                    val path = Path()
                    path.moveTo(
                        currentDragPoints.first().x * size.width,
                        currentDragPoints.first().y * size.height
                    )
                    for (i in 1 until currentDragPoints.size) {
                        path.lineTo(
                            currentDragPoints[i].x * size.width,
                            currentDragPoints[i].y * size.height
                        )
                    }
                    drawPath(
                        path = path,
                        color = activeColor,
                        style = CanvasStroke(
                            width = activeWidthPx,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            // Spectator / Guesser overlay tag when you cannot draw
            if (!isDrawingEnabled) {
                Surface(
                    color = Color(0x99100B24),
                    shape = RoundedCornerShape(bottomStart = 12.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = "👀 Watching Live",
                        color = ArcadeCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Drawing Toolbar (Only displayed when the player is the active drawer!)
        if (isDrawingEnabled) {
            DrawingToolbar(
                selectedColor = currentColor,
                selectedWidth = currentBrushWidth,
                isEraser = isEraser,
                onSelectColor = { color ->
                    currentColor = color
                    isEraser = false
                },
                onSelectWidth = { width ->
                    currentBrushWidth = width
                },
                onToggleEraser = {
                    isEraser = !isEraser
                },
                onUndo = onUndo,
                onClear = onClear
            )
        }
    }
}

@Composable
fun DrawingToolbar(
    selectedColor: Color,
    selectedWidth: Float,
    isEraser: Boolean,
    onSelectColor: (Color) -> Unit,
    onSelectWidth: (Float) -> Unit,
    onToggleEraser: () -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit
) {
    val brushSizes = listOf(4f, 8f, 16f, 26f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ArcadeSurfaceDark)
            .border(1.dp, Color(0xFF382C75), RoundedCornerShape(14.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Color Palette Selector Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(DrawingPaletteColors) { color ->
                val isSelected = !isEraser && selectedColor == color
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) ArcadeCyan else Color.White.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                        .clickable { onSelectColor(color) }
                )
            }
        }

        // Action Controls Row: Brush Sizes, Eraser, Undo, Clear
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brush Width Selector
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                brushSizes.forEach { size ->
                    val isSelected = selectedWidth == size
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) ArcadeCyan.copy(alpha = 0.25f) else Color.Transparent)
                            .border(
                                1.dp,
                                if (isSelected) ArcadeCyan else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelectWidth(size) },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size((size / 2).coerceIn(4f, 18f).dp)
                                .clip(CircleShape)
                                .background(if (isSelected) ArcadeCyan else Color.White)
                        )
                    }
                }
            }

            // Eraser, Undo, Clear
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Eraser button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isEraser) ArcadeCoral.copy(alpha = 0.25f) else Color.Transparent)
                        .border(
                            1.dp,
                            if (isEraser) ArcadeCoral else Color(0xFF4A3B85),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onToggleEraser() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isEraser) "Eraser ON" else "Eraser",
                        color = if (isEraser) ArcadeCoral else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Undo
                IconButton(
                    onClick = onUndo,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = ArcadeYellow,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Clear
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Canvas",
                        tint = ArcadeCoral,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
