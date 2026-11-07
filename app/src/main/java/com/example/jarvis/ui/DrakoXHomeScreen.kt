package com.example.jarvis.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Void = Color(0xFF070A10)
private val Panel = Color(0xFF101722)
private val Neon = Color(0xFF55DDF5)
private val Muted = Color(0xFF91A3B5)

@Composable
fun DrakoXTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = Neon,
            secondary = Color(0xFF9B8CFF),
            background = Void,
            surface = Panel,
            onPrimary = Void,
            onBackground = Color(0xFFEAF5FF),
            onSurface = Color(0xFFEAF5FF)
        ),
        content = content
    )
}

/**
 * Compose-first DRΛKOX JΛRVIS home surface.
 * This screen is intentionally isolated until MainActivity's existing
 * voice, accessibility and automation lifecycle is wired into callbacks.
 */
@Composable
fun DrakoXHomeScreen(
    modifier: Modifier = Modifier,
    onSendMessage: (String) -> Unit = {},
    onMicClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    var message by remember { mutableStateOf("") }

    DrakoXTheme {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0B111B), Void, Color(0xFF080D15))
                    )
                )
                .padding(horizontal = 22.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DRΛKOX",
                        color = Color(0xFFEAF7FF),
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.2.sp
                    )
                    Text(
                        text = "J A R V I S   /   PERSONAL AI",
                        color = Muted,
                        fontSize = 9.sp,
                        letterSpacing = 1.6.sp
                    )
                }
                IconButton(onClick = onHistoryClick) {
                    Icon(Icons.Outlined.History, contentDescription = "Chat history", tint = Muted)
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = Muted)
                }
            }

            Spacer(Modifier.height(30.dp))

            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "SYSTEM ONLINE",
                    color = Neon,
                    fontSize = 10.sp,
                    letterSpacing = 2.4.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(24.dp))
                AnimatedNeonOrb()
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "At your service.",
                    color = Color(0xFFEAF5FF),
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    text = "Your conversations. Your commands. Your JARVIS.",
                    color = Muted,
                    fontSize = 11.sp
                )
            }

            Surface(
                color = Panel,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x3355DDF5), RoundedCornerShape(24.dp))
            ) {
                Row(
                    modifier = Modifier.padding(start = 17.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = message,
                        onValueChange = { message = it },
                        modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                        decorationBox = { inner ->
                            Box {
                                if (message.isEmpty()) {
                                    Text("Message JARVIS...", color = Muted, fontSize = 13.sp)
                                }
                                inner()
                            }
                        }
                    )
                    IconButton(onClick = onMicClick) {
                        Icon(Icons.Outlined.Mic, contentDescription = "Voice input", tint = Neon)
                    }
                    IconButton(
                        onClick = {
                            val value = message.trim()
                            if (value.isNotEmpty()) {
                                onSendMessage(value)
                                message = ""
                            }
                        }
                    ) {
                        Icon(Icons.Outlined.Send, contentDescription = "Send message", tint = Neon)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "PRIVATE SESSION  •  ENCRYPTED LOCAL SETTINGS",
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF637487),
                fontSize = 8.sp,
                letterSpacing = 1.1.sp
            )
        }
    }
}

@Composable
private fun AnimatedNeonOrb() {
    val transition = rememberInfiniteTransition(label = "jarvis-orb")
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1900), RepeatMode.Reverse),
        label = "orb-pulse"
    )
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(9000)),
        label = "orb-rotation"
    )

    Canvas(modifier = Modifier.size(214.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val base = size.minDimension * 0.27f * pulse
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x6655DDF5), Color(0x2255DDF5), Color.Transparent),
                center = center,
                radius = base * 2.2f
            ),
            radius = base * 2.2f,
            center = center
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFB9F7FF), Color(0xFF2AAFCB), Color(0xFF102A40)),
                center = Offset(center.x - base * 0.25f, center.y - base * 0.3f),
                radius = base * 1.2f
            ),
            radius = base,
            center = center
        )
        drawCircle(
            color = Color(0xAA8CEEFF),
            radius = base * 1.43f,
            center = center,
            style = Stroke(width = 1.3.dp.toPx())
        )
        drawArc(
            color = Neon,
            startAngle = rotation,
            sweepAngle = 92f,
            useCenter = false,
            topLeft = Offset(center.x - base * 1.72f, center.y - base * 1.72f),
            size = androidx.compose.ui.geometry.Size(base * 3.44f, base * 3.44f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
        drawArc(
            color = Color(0x669B8CFF),
            startAngle = rotation + 180f,
            sweepAngle = 58f,
            useCenter = false,
            topLeft = Offset(center.x - base * 1.9f, center.y - base * 1.9f),
            size = androidx.compose.ui.geometry.Size(base * 3.8f, base * 3.8f),
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
