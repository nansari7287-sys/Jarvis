package com.example.jarvis.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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

private val Void = Color(0xFF05050A)
private val Panel = Color(0xE611111D)
private val PanelLine = Color(0xFF292238)
private val Crimson = Color(0xFFFF3158)
private val Violet = Color(0xFF9B65FF)
private val Ice = Color(0xFFECEBFA)
private val Muted = Color(0xFF8E8BA5)

@Composable
fun DrakoXTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = Crimson,
            secondary = Violet,
            background = Void,
            surface = Panel,
            onPrimary = Color.White,
            onBackground = Ice,
            onSurface = Ice
        ),
        content = content
    )
}

/**
 * Premium visual foundation for DRΛKOX JΛRVIS.
 * Kept isolated from MainActivity so existing voice, accessibility,
 * automation and service lifecycles remain untouched until explicitly wired.
 */
private enum class DrakoXPage { HOME, HISTORY, CREATOR, SETTINGS }

@Composable
fun DrakoXHomeScreen(
    modifier: Modifier = Modifier,
    onSendMessage: (String) -> Unit = {},
    onMicClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onDeveloperClick: () -> Unit = {}
) {
    var message by remember { mutableStateOf("") }
    var page by remember { mutableStateOf(DrakoXPage.HOME) }

    DrakoXTheme {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF160914), Color(0xFF080811), Void, Color(0xFF10091A))))
        ) {
            CosmicBackdrop()
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Header(
                    onHistoryClick = { page = DrakoXPage.HISTORY },
                    onSettingsClick = { page = DrakoXPage.SETTINGS }
                )
                Spacer(Modifier.height(18.dp))
                SystemStrip()
                Spacer(Modifier.height(10.dp))

                when (page) {
                    DrakoXPage.HOME -> {
                        Column(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("YOUR PERSONAL INTELLIGENCE", color = Muted, fontSize = 9.sp, letterSpacing = 2.5.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(12.dp))
                            Text("I AM  DRΛKOX", color = Ice, fontSize = 25.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(2.dp))
                            Text("Ready when you are.", color = Muted, fontSize = 12.sp)
                            Spacer(Modifier.height(12.dp))
                            AnimatedNeonOrb()
                            Spacer(Modifier.height(10.dp))
                            Text("CORE  •  ONLINE", color = Color(0xFFFF718B), fontSize = 10.sp, letterSpacing = 2.4.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(18.dp))
                            QuickStatusRow()
                        }
                        ActionDock(
                            message = message,
                            onMessageChange = { message = it },
                            onMicClick = onMicClick,
                            onSend = {
                                val value = message.trim()
                                if (value.isNotEmpty()) {
                                    onSendMessage(value)
                                    message = ""
                                }
                            }
                        )
                    }
                    DrakoXPage.HISTORY -> HistoryPage(modifier = Modifier.weight(1f), onBack = { page = DrakoXPage.HOME })
                    DrakoXPage.CREATOR -> CreatorPage(modifier = Modifier.weight(1f), onBack = { page = DrakoXPage.HOME }, onOpenProfile = onDeveloperClick)
                    DrakoXPage.SETTINGS -> SettingsPage(
                        modifier = Modifier.weight(1f),
                        onBack = { page = DrakoXPage.HOME },
                        onOpenVault = onSettingsClick
                    )
                }

                Spacer(Modifier.height(12.dp))
                BottomNavigation(
                    selected = page,
                    onHome = { page = DrakoXPage.HOME },
                    onHistory = { page = DrakoXPage.HISTORY },
                    onDeveloper = { page = DrakoXPage.CREATOR },
                    onSettings = { page = DrakoXPage.SETTINGS }
                )
            }
        }
    }
}

@Composable
private fun ScreenHeading(kicker: String, title: String, subtitle: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text("‹", color = Crimson, fontSize = 34.sp, modifier = Modifier.clickable(onClick = onBack).padding(end = 12.dp))
        Column {
            Text(kicker, color = Crimson, fontSize = 8.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Text(title, color = Ice, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun FeaturePanel(title: String, detail: String, marker: String = "✦", onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(Panel).border(1.dp, PanelLine, RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(marker, color = Crimson, fontSize = 22.sp, modifier = Modifier.padding(end = 13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Ice, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(detail, color = Muted, fontSize = 9.sp, lineHeight = 14.sp)
        }
        if (onClick != null) Text("›", color = Violet, fontSize = 24.sp)
    }
}

@Composable
private fun HistoryPage(modifier: Modifier = Modifier, onBack: () -> Unit) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenHeading("SESSION ARCHIVE", "Command History", "Recent conversation storage", onBack)
        Spacer(Modifier.height(6.dp))
        FeaturePanel("History storage status", "The current AI conversation manager keeps up to 20 recent messages in memory. A persistent searchable archive is not yet connected to this screen.", "◷")
        FeaturePanel("Privacy", "This screen does not fabricate past commands or display sample records as real activity.", "◇")
        Spacer(Modifier.height(10.dp))
        Text("LIVE SESSION HISTORY CONNECTION — PENDING", color = Muted, fontSize = 8.sp, letterSpacing = 1.1.sp)
    }
}

@Composable
private fun CreatorPage(modifier: Modifier = Modifier, onBack: () -> Unit, onOpenProfile: () -> Unit) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        ScreenHeading("SYSTEM ARCHITECT", "Developer Profile", "The mind behind this interface", onBack)
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF351326), Color(0xFF171126), Color(0xFF10101B))))
                .border(1.dp, Crimson.copy(alpha = 0.35f), RoundedCornerShape(22.dp)).padding(22.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(76.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Crimson, Violet))), contentAlignment = Alignment.Center) {
                    Text("N", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(14.dp))
                Text("𝑵𝒂𝒆𝒆𝒎", color = Ice, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("DEVELOPER • DRΛKOX JΛRVIS", color = Color(0xFFFF8299), fontSize = 9.sp, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(8.dp))
                Text("Building intelligent experiences with a focus on voice, automation and useful AI.", color = Muted, fontSize = 10.sp)
            }
        }
        FeaturePanel("Project", "DRΛKOX JΛRVIS — personal AI assistant interface", "⌘")
        FeaturePanel("Core focus", "AI interaction • Voice control • Android automation", "✧")
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun SettingsPage(modifier: Modifier = Modifier, onBack: () -> Unit, onOpenVault: () -> Unit) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenHeading("CONFIGURATION", "System Settings", "Manage the existing assistant configuration", onBack)
        Spacer(Modifier.height(6.dp))
        FeaturePanel("AI provider & API vault", "Open the existing provider configuration for Gemini, ChatGPT and Grok. Existing credential handling is preserved.", "⌘", onOpenVault)
        FeaturePanel("Voice & microphone", "Voice controls remain connected to the current VoiceService and microphone action.", "♫")
        FeaturePanel("Accessibility & automation", "Existing Android accessibility and automation services are retained.", "⌁")
        FeaturePanel("Interface", "DRΛKOX dark-space theme with crimson and electric-violet accents.", "◈")
        Spacer(Modifier.height(10.dp))
        Text("ADVANCED OPTIONS OPEN THE EXISTING CONFIGURATION", color = Muted, fontSize = 8.sp, letterSpacing = 0.8.sp)
    }
}

@Composable
private fun Header(onHistoryClick: () -> Unit, onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(Crimson, Violet))),
            contentAlignment = Alignment.Center
        ) {
            Text("D", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("DRΛKOX JΛRVIS", color = Ice, fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
            Text("PERSONAL AI  /  VIRTUAL CORE", color = Muted, fontSize = 8.sp, letterSpacing = 1.2.sp)
        }
        IconButton(onClick = onHistoryClick) {
            Icon(Icons.Outlined.History, contentDescription = "History", tint = Ice)
        }
        IconButton(onClick = onSettingsClick) {
            Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = Ice)
        }
    }
}

@Composable
private fun SystemStrip() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xAA171321))
            .border(1.dp, PanelLine, RoundedCornerShape(12.dp))
            .padding(horizontal = 13.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF62F5B0)))
        Spacer(Modifier.width(8.dp))
        Text("SYSTEM ONLINE", color = Color(0xFF62F5B0), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
        Spacer(Modifier.height(10.dp))
        Text("SECURE SESSION", color = Muted, fontSize = 8.sp, letterSpacing = 1.sp)
        Spacer(Modifier.width(7.dp))
        Text("◆", color = Violet, fontSize = 9.sp)
    }
}

@Composable
private fun QuickStatusRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusPill("AI CORE", "READY", Crimson)
        StatusPill("VOICE", "STANDBY", Violet)
        StatusPill("MODE", "PERSONAL", Color(0xFF62DCCB))
    }
}

@Composable
private fun StatusPill(label: String, value: String, accent: Color) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xB312111C))
            .border(1.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = Muted, fontSize = 7.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(3.dp))
        Text(value, color = accent, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
    }
}

@Composable
private fun ActionDock(
    message: String,
    onMessageChange: (String) -> Unit,
    onMicClick: () -> Unit,
    onSend: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text("COMMAND INTERFACE", color = Muted, fontSize = 8.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
        Surface(
            color = Panel,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Brush.linearGradient(listOf(Crimson.copy(alpha = 0.7f), Violet.copy(alpha = 0.45f), PanelLine)), RoundedCornerShape(22.dp))
        ) {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = message,
                    onValueChange = onMessageChange,
                    modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Ice),
                    decorationBox = { inner ->
                        Box {
                            if (message.isEmpty()) Text("Ask anything, command anything...", color = Muted, fontSize = 12.sp)
                            inner()
                        }
                    }
                )
                IconButton(onClick = onMicClick) {
                    Icon(Icons.Outlined.Mic, contentDescription = "Voice input", tint = Crimson)
                }
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Crimson, Violet)))
                        .clickable(onClick = onSend),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(19.dp))
                }
            }
        }
        Text(
            "Developed By  𝑵𝒂𝒆𝒆𝒎",
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF69647C),
            fontSize = 8.sp,
            letterSpacing = 1.1.sp
        )
    }
}

@Composable
private fun BottomNavigation(
    selected: DrakoXPage,
    onHome: () -> Unit,
    onHistory: () -> Unit,
    onDeveloper: () -> Unit,
    onSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xE9100F19))
            .border(1.dp, PanelLine, RoundedCornerShape(18.dp))
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavItem("⌂", "HOME", selected == DrakoXPage.HOME, onHome)
        NavItem("◷", "HISTORY", selected == DrakoXPage.HISTORY, onHistory)
        NavItem("✧", "CREATOR", selected == DrakoXPage.CREATOR, onDeveloper)
        NavItem("⚙", "SETTINGS", selected == DrakoXPage.SETTINGS, onSettings)
    }
}

@Composable
private fun NavItem(symbol: String, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(symbol, color = if (selected) Crimson else Muted, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(3.dp))
        Text(label, color = if (selected) Ice else Muted, fontSize = 7.sp, letterSpacing = 0.8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CosmicBackdrop() {
    val transition = rememberInfiniteTransition(label = "backdrop")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(12000), RepeatMode.Reverse),
        label = "backdrop-drift"
    )
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0x443D102C), Color.Transparent), center = Offset(w * (0.18f + drift * 0.08f), h * 0.22f), radius = w * 0.72f),
            radius = w * 0.72f,
            center = Offset(w * (0.18f + drift * 0.08f), h * 0.22f)
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0x332B1755), Color.Transparent), center = Offset(w * 0.9f, h * 0.72f), radius = w * 0.65f),
            radius = w * 0.65f,
            center = Offset(w * 0.9f, h * 0.72f)
        )
        for (i in 0..74) {
            val x = ((i * 73) % 997) / 997f * w
            val y = ((i * 137) % 991) / 991f * h
            val alpha = 0.16f + ((i % 4) * 0.08f)
            drawCircle(Color.White.copy(alpha = alpha), radius = if (i % 9 == 0) 1.7.dp.toPx() else 0.8.dp.toPx(), center = Offset(x, y))
        }
        for (i in 0..8) {
            val y = h * (0.12f + i * 0.095f)
            drawLine(Color(0x0DFF5477), Offset(0f, y), Offset(w, y), strokeWidth = 0.7.dp.toPx())
        }
    }
}

@Composable
private fun AnimatedNeonOrb() {
    val transition = rememberInfiniteTransition(label = "jarvis-orb")
    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(tween(1700), RepeatMode.Reverse),
        label = "orb-pulse"
    )
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8500)),
        label = "orb-rotation"
    )

    Canvas(Modifier.size(232.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val base = size.minDimension * 0.245f * pulse
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x66FF3158), Color(0x229B65FF), Color.Transparent),
                center = center,
                radius = base * 2.55f
            ),
            radius = base * 2.55f,
            center = center
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFD5DE), Color(0xFFFF3158), Color(0xFF5D153D), Color(0xFF160F2C)),
                center = Offset(center.x - base * 0.25f, center.y - base * 0.3f),
                radius = base * 1.2f
            ),
            radius = base,
            center = center
        )
        drawCircle(Color(0xAAFF9AAC), radius = base * 1.42f, center = center, style = Stroke(width = 1.2.dp.toPx()))
        drawCircle(Color(0x668F65FF), radius = base * 1.67f, center = center, style = Stroke(width = 0.8.dp.toPx()))
        drawArc(
            color = Crimson,
            startAngle = rotation,
            sweepAngle = 105f,
            useCenter = false,
            topLeft = Offset(center.x - base * 1.78f, center.y - base * 1.78f),
            size = androidx.compose.ui.geometry.Size(base * 3.56f, base * 3.56f),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
        drawArc(
            color = Violet,
            startAngle = rotation + 155f,
            sweepAngle = 68f,
            useCenter = false,
            topLeft = Offset(center.x - base * 1.98f, center.y - base * 1.98f),
            size = androidx.compose.ui.geometry.Size(base * 3.96f, base * 3.96f),
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        )
        drawArc(
            color = Color(0x77FF8AA0),
            startAngle = rotation + 270f,
            sweepAngle = 35f,
            useCenter = false,
            topLeft = Offset(center.x - base * 2.12f, center.y - base * 2.12f),
            size = androidx.compose.ui.geometry.Size(base * 4.24f, base * 4.24f),
            style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

