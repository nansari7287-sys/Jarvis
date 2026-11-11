package com.example.jarvis.ui

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

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

private val Void = Color(0xFF050711)
private val Panel = Color(0xE6101524)
private val PanelLine = Color(0xFF263653)
private val Crimson = Color(0xFF27D9FF)
private val Violet = Color(0xFF9A63FF)
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
 * Premium visual foundation for JARVIS.
 * Kept isolated from MainActivity so existing voice, accessibility,
 * automation and service lifecycles remain untouched until explicitly wired.
 */
private enum class DrakoXPage { HOME, HISTORY, CREATOR, SETTINGS, PROVIDERS, EXTENSIONS, COOKIES, VOICE, TOOLS, AUTOMATION, THEMES }

@Composable
fun DrakoXHomeScreen(
    modifier: Modifier = Modifier,
    onSendMessage: (String) -> Unit = {},
    onMicClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onDeveloperClick: () -> Unit = {},
    activeProvider: String = "GEMINI",
    onProviderSelected: (String) -> Unit = {},
    onConfigureProviders: () -> Unit = {}
) {
    var message by remember { mutableStateOf("") }
    var page by remember { mutableStateOf(DrakoXPage.HOME) }

    DrakoXTheme {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF101A35), Color(0xFF080D1B), Void, Color(0xFF11102A))))
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
                if (page != DrakoXPage.HOME) SystemStrip()

                when (page) {
                    DrakoXPage.HOME -> {
                        Column(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            AnimatedNeonOrb()
                        }
                        ActionDock(
                            message = message,
                            onMessageChange = { message = it },
                            onMicClick = onMicClick,
                            onDeveloperClick = { page = DrakoXPage.CREATOR },
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
                        onOpenVault = { page = DrakoXPage.PROVIDERS },
                        onOpenExtensions = { page = DrakoXPage.EXTENSIONS },
                        onOpenCookies = { page = DrakoXPage.COOKIES },
                        onOpenVoice = { page = DrakoXPage.VOICE },
                        onOpenThemes = { page = DrakoXPage.THEMES }
                    )
                    DrakoXPage.PROVIDERS -> ProviderPage(
                        modifier = Modifier.weight(1f), activeProvider = activeProvider,
                        onBack = { page = DrakoXPage.SETTINGS },
                        onSelect = onProviderSelected, onConfigure = onConfigureProviders
                    )
                    DrakoXPage.EXTENSIONS -> ExtensionsHubPage(modifier = Modifier.weight(1f), onBack = { page = DrakoXPage.SETTINGS })
                    DrakoXPage.COOKIES -> CookieVaultPage(modifier = Modifier.weight(1f), onBack = { page = DrakoXPage.SETTINGS })
                    DrakoXPage.THEMES -> ThemeGalleryPage(modifier = Modifier.weight(1f), onBack = { page = DrakoXPage.SETTINGS })
                    DrakoXPage.VOICE -> InfoModulePage(
                        modifier = Modifier.weight(1f), title = "Voice & Personality",
                        subtitle = "Assistant voice preferences",
                        detail = "Voice selection and language controls will be connected to Android Text-to-Speech. Available voices depend on the speech engines installed on this device.",
                        onBack = { page = DrakoXPage.SETTINGS }
                    )
                    DrakoXPage.TOOLS -> InfoModulePage(
                        modifier = Modifier.weight(1f), title = "Tools Hub",
                        subtitle = "Your connected utilities in one place",
                        detail = "Code assistant, study tools, web search, API tester and device utilities will appear here as each module is connected.",
                        onBack = { page = DrakoXPage.HOME }
                    )
                    DrakoXPage.AUTOMATION -> InfoModulePage(
                        modifier = Modifier.weight(1f), title = "Automation",
                        subtitle = "Android actions and routines",
                        detail = "Existing accessibility and automation services remain in place. New routines will require clear permissions and confirmation for sensitive actions.",
                        onBack = { page = DrakoXPage.HOME }
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
private fun ProviderPage(
    modifier: Modifier = Modifier,
    activeProvider: String,
    onBack: () -> Unit,
    onSelect: (String) -> Unit,
    onConfigure: () -> Unit
) {
    val providers = listOf("GEMINI" to "Google Gemini", "CHATGPT" to "ChatGPT", "GROK" to "Grok")
    var selectedProvider by remember(activeProvider) { mutableStateOf(activeProvider) }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenHeading("AI CONNECTIVITY", "AI Provider", "Only one provider can be active at a time", onBack)
        providers.forEach { (id, label) ->
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(if (selectedProvider == id) Crimson.copy(alpha = 0.13f) else Panel)
                    .border(1.dp, if (selectedProvider == id) Crimson else PanelLine, RoundedCornerShape(16.dp))
                    .clickable { selectedProvider = id; onSelect(id) }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(label, color = Ice, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(if (selectedProvider == id) "ACTIVE PROVIDER" else "Tap to activate", color = if (selectedProvider == id) Color(0xFF62F5B0) else Muted, fontSize = 9.sp)
                }
                Text(if (selectedProvider == id) "●" else "○", color = if (activeProvider == id) Crimson else Muted, fontSize = 22.sp)
            }
        }
        FeaturePanel("API credentials", "Each provider keeps its own saved API key and model. Switching providers does not erase the others.", "◇", onConfigure)
        Text("Select one provider above. Your selection is saved immediately.", color = Muted, fontSize = 9.sp)
    }
}


@Composable
private fun CookieVaultPage(modifier: Modifier = Modifier, onBack: () -> Unit) {
    val context = LocalContext.current
    var savedFiles by remember { mutableStateOf(cookieFiles(context)) }
    var status by remember { mutableStateOf("Your cookie files stay encrypted on this device.") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val name = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && index >= 0) cursor.getString(index) else "cookie-export"
                } ?: "cookie-export"
                require(name.endsWith(".txt", true) || name.endsWith(".json", true)) { "Choose a .txt or .json cookie export." }
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Cannot read selected file.")
                require(bytes.isNotEmpty() && bytes.size <= 5 * 1024 * 1024) { "File is empty or larger than 5 MB." }
                saveEncryptedCookie(context, bytes, name)
                savedFiles = cookieFiles(context)
                status = "Encrypted import saved successfully."
            }.onFailure { status = it.message ?: "Import failed." }
        }
    }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        ScreenHeading("SECURE SESSION STORAGE", "Cookie Vault", "Import and manage your own website sessions", onBack)
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(listOf(Color(0xFF142B48), Color(0xFF101321)))).border(1.dp, Brush.linearGradient(listOf(Crimson, Violet)), RoundedCornerShape(18.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("ENCRYPTED VAULT", color = Color(0xFF70F5C1), fontSize = 9.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                Text("${savedFiles.size} saved file(s)", color = Ice, fontSize = 19.sp, fontWeight = FontWeight.Black)
                Text("AES-GCM • Android Keystore", color = Muted, fontSize = 9.sp)
            }
            Text("◇", color = Crimson, fontSize = 34.sp)
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Panel).border(1.dp, Brush.linearGradient(listOf(Crimson, Violet)), RoundedCornerShape(16.dp)).clickable { picker.launch(arrayOf("text/plain", "application/json", "application/octet-stream")) }.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("+", color = Crimson, fontSize = 36.sp)
            Text("ADD COOKIE FILE", color = Ice, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
            Text("Choose your own .TXT or .JSON export", color = Muted, fontSize = 9.sp)
        }
        Text(status, color = if (status.contains("success", true)) Color(0xFF70F5C1) else Muted, fontSize = 10.sp)
        Text("SAVED COOKIE FILES", color = Crimson, fontSize = 9.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
        if (savedFiles.isEmpty()) {
            FeaturePanel("Vault is empty", "Tap ADD COOKIE FILE above to import an authorised cookie export.", "▣")
        } else savedFiles.forEach { file ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Panel).border(1.dp, PanelLine, RoundedCornerShape(14.dp)).padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("▣", color = Violet, fontSize = 22.sp, modifier = Modifier.padding(end = 10.dp))
                Column(Modifier.weight(1f)) {
                    Text(file.name.removeSuffix(".jvc"), color = Ice, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Encrypted locally • ${file.length()} bytes", color = Muted, fontSize = 8.sp)
                }
                Text("DELETE", color = Crimson, fontSize = 9.sp, modifier = Modifier.clickable { file.delete(); savedFiles = cookieFiles(context); status = "Selected encrypted file deleted." }.padding(8.dp))
            }
        }
        Text("Cookie files can grant account access. Import only files belonging to accounts you own or are authorised to manage. JARVIS does not read Chrome's private storage.", color = Muted, fontSize = 9.sp, lineHeight = 14.sp)
    }
}

private fun cookieFiles(context: Context): List<File> =
    File(context.filesDir, "cookie_vault").listFiles()?.filter { it.extension == "jvc" }?.sortedByDescending { it.lastModified() } ?: emptyList()

private fun saveEncryptedCookie(context: Context, data: ByteArray, originalName: String) {
    val alias = "jarvis_cookie_vault_key"
    val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    val key = (store.getKey(alias, null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
        init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setRandomizedEncryptionRequired(true).build())
        generateKey()
    }
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, key)
    val encrypted = cipher.doFinal(data)
    val folder = File(context.filesDir, "cookie_vault").apply { mkdirs() }
    val safeName = originalName.substringAfterLast('/').replace(Regex("[^A-Za-z0-9._-]"), "_").take(48)
    File(folder, "${System.currentTimeMillis()}_${safeName}.jvc").writeBytes(cipher.iv + encrypted)
}


@Composable
private fun ExtensionsHubPage(modifier: Modifier = Modifier, onBack: () -> Unit) {
    var showAdd by remember { mutableStateOf(false) }
    var kind by remember { mutableStateOf("API") }
    var name by remember { mutableStateOf("") }
    var endpoint by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("Only add integrations you own or are authorised to use.") }
    var entries by remember { mutableStateOf(listOf("Gemini AI" to true, "ChatGPT" to false, "Grok" to false)) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenHeading("JARVIS MODULE ECOSYSTEM", "Extensions Hub", "Connect and manage your integrations", onBack)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("ALL ${entries.size}", "ACTIVE ${entries.count { it.second }}").forEach {
                Text(it, color = Violet, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(Panel).border(1.dp, PanelLine, RoundedCornerShape(9.dp)).padding(10.dp))
            }
        }
        entries.forEachIndexed { index, item ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Panel).border(1.dp, PanelLine, RoundedCornerShape(14.dp)).padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("✧", color = Violet, fontSize = 22.sp, modifier = Modifier.padding(end = 10.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.first, color = Ice, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(if (item.second) "CONNECTED" else "DISABLED", color = if (item.second) Color(0xFF62F5B0) else Muted, fontSize = 8.sp)
                }
                Text(if (item.second) "ON" else "OFF", color = if (item.second) Color(0xFF62F5B0) else Muted, fontSize = 10.sp, modifier = Modifier.clickable { entries = entries.toMutableList().also { it[index] = item.first to !item.second } }.padding(8.dp))
            }
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(Color(0xFF142B48), Color(0xFF111222)))).border(1.dp, Brush.linearGradient(listOf(Violet, Crimson)), RoundedCornerShape(16.dp)).clickable { showAdd = !showAdd }.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("+ ADD EXTENSION", color = Ice, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            Text("API • Cookie file • Email • Custom", color = Muted, fontSize = 9.sp)
        }
        if (showAdd) {
            Text("EXTENSION TYPE", color = Crimson, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("API", "Cookie", "Email", "Custom").forEach { type ->
                    Text(type, color = if (kind == type) Color.White else Muted, modifier = Modifier.weight(1f).clip(RoundedCornerShape(9.dp)).background(if (kind == type) Violet.copy(alpha=.35f) else Panel).border(1.dp, if (kind == type) Violet else PanelLine, RoundedCornerShape(9.dp)).clickable { kind = type }.padding(8.dp), fontSize = 9.sp)
                }
            }
            NeonField("Extension name", name, { name = it })
            if (kind == "API" || kind == "Custom") {
                NeonField("API base URL", endpoint, { endpoint = it })
                NeonField("API key", apiKey, { apiKey = it })
                NeonField("Model / endpoint (optional)", model, { model = it })
            } else FeaturePanel(if (kind == "Cookie") "Import cookie file" else "Email connection", if (kind == "Cookie") "Use Cookie Vault to securely import an authorised .txt or .json export." else "Email OAuth and provider-specific secure sign-in are not yet connected.", "◇")
            Text("SAVE EXTENSION", color = Ice, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Brush.horizontalGradient(listOf(Crimson, Violet))).clickable {
                if (name.isBlank()) notice = "Enter an extension name first."
                else if ((kind == "API" || kind == "Custom") && endpoint.isBlank()) notice = "Enter the API base URL."
                else { entries = entries + (name to false); notice = "$kind extension added to this screen. Provider execution needs its adapter."; showAdd = false; name = ""; endpoint = ""; apiKey = ""; model = "" }
            }.padding(14.dp))
        }
        Text(notice, color = Muted, fontSize = 9.sp)
    }
}

@Composable
private fun NeonField(label: String, value: String, onValue: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, color = Muted, fontSize = 9.sp)
        BasicTextField(value, onValue, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFF090A14)).border(1.dp, PanelLine, RoundedCornerShape(10.dp)).padding(12.dp), textStyle = androidx.compose.ui.text.TextStyle(color = Ice, fontSize = 11.sp), singleLine = true, decorationBox = { inner -> if (value.isEmpty()) Text(label, color = Muted, fontSize = 10.sp); inner() })
    }
}

@Composable
private fun ThemeGalleryPage(modifier: Modifier = Modifier, onBack: () -> Unit) {
    var chosen by remember { mutableStateOf("Quantum Purple") }
    val themes = listOf("Quantum Purple" to listOf(Color(0xFF9A63FF), Color(0xFF27D9FF)), "Cyber Red" to listOf(Color(0xFFFF3158), Color(0xFFFF8A45)), "Matrix Green" to listOf(Color(0xFF00E68A), Color(0xFF8CFF4D)), "Neon Blue" to listOf(Color(0xFF168BFF), Color(0xFF28E5FF)), "Solar Gold" to listOf(Color(0xFFFFB52E), Color(0xFFFFE18A)), "Stealth" to listOf(Color(0xFF8992A7), Color(0xFFCED5E4)), "Arctic White" to listOf(Color(0xFFB8DFFF), Color(0xFFFFFFFF)), "Deep Space" to listOf(Color(0xFF602CFF), Color(0xFF00D5FF)))
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenHeading("VISUAL EXPERIENCE", "Themes", "Select your JARVIS interface palette", onBack)
        themes.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                row.forEach { item ->
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Panel).border(1.dp, if (chosen == item.first) item.second[0] else PanelLine, RoundedCornerShape(14.dp)).clickable { chosen = item.first }.padding(10.dp)) {
                        Box(Modifier.fillMaxWidth().height(62.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(item.second)))
                        Text(item.first, color = Ice, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                        Text(if (chosen == item.first) "✓ SELECTED" else "TAP TO PREVIEW", color = if (chosen == item.first) Color(0xFF62F5B0) else Muted, fontSize = 8.sp)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Text("Selected: $chosen", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text("Theme preview selection is local to this screen; applying palette across every component and persisting it is the next integration step.", color = Muted, fontSize = 9.sp)
    }
}

@Composable
private fun InfoModulePage(modifier: Modifier = Modifier, title: String, subtitle: String, detail: String, onBack: () -> Unit) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ScreenHeading("JARVIS CONNECTIVITY", title, subtitle, onBack)
        FeaturePanel(title, detail, "◇")
    }
}


@Composable
private fun DashboardGrid(
    onChat: () -> Unit, onVoice: () -> Unit, onAutomation: () -> Unit,
    onExtensions: () -> Unit, onCookies: () -> Unit, onProvider: () -> Unit,
    onTools: () -> Unit, onCode: () -> Unit, onSettings: () -> Unit
) {
    val rows = listOf(
        listOf(Triple("AI CHAT", "▤", onChat), Triple("VOICE", "♫", onVoice), Triple("AUTOMATION", "⌘", onAutomation)),
        listOf(Triple("EXTENSIONS", "✣", onExtensions), Triple("COOKIE VAULT", "▣", onCookies), Triple("AI PROVIDER", "✧", onProvider)),
        listOf(Triple("TOOLS HUB", "⌁", onTools), Triple("CODE ASSIST", "</>", onCode), Triple("SETTINGS", "⚙", onSettings))
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { item ->
                    Column(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                            .background(Brush.verticalGradient(listOf(Color(0xFF14243B), Color(0xFF0B1020))))
                            .border(1.dp, Brush.linearGradient(listOf(Crimson.copy(alpha = 0.8f), Violet.copy(alpha = 0.65f))), RoundedCornerShape(14.dp))
                            .clickable(onClick = item.third).padding(vertical = 11.dp, horizontal = 3.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(item.second, color = if (item.first == "AI PROVIDER") Violet else Crimson, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(item.first, color = Ice, fontSize = 7.sp, letterSpacing = 0.7.sp, fontWeight = FontWeight.Bold)
                    }
                }
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
                Text("DEVELOPER • JARVIS", color = Color(0xFFFF8299), fontSize = 9.sp, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(8.dp))
                Text("Building intelligent experiences with a focus on voice, automation and useful AI.", color = Muted, fontSize = 10.sp)
            }
        }
        FeaturePanel("Project", "JARVIS — personal AI assistant interface", "⌘")
        FeaturePanel("Core focus", "AI interaction • Voice control • Android automation", "✧")
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun SettingsPage(modifier: Modifier = Modifier, onBack: () -> Unit, onOpenVault: () -> Unit, onOpenExtensions: () -> Unit, onOpenCookies: () -> Unit, onOpenVoice: () -> Unit, onOpenThemes: () -> Unit) {
    var selectedVoice by remember { mutableStateOf("Female") }
    var selectedLanguage by remember { mutableStateOf("Hindi") }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenHeading("CONFIGURATION", "System Settings", "Manage the existing assistant configuration", onBack)
        Spacer(Modifier.height(6.dp))
        FeaturePanel("AI provider & API vault", "Choose exactly one active provider: Gemini, ChatGPT or Grok. API credentials remain separately saved.", "⌘", onOpenVault)
        FeaturePanel("Appearance & Themes", "Choose a complete neon interface palette.", "◈", onOpenThemes)
        FeaturePanel("Voice & microphone", "Choose voice and language preferences.", "♫", onOpenVoice)
        FeaturePanel("Extensions Manager", "Explore and manage trusted JARVIS extensions.", "✣", onOpenExtensions)
        FeaturePanel("Cookie Vault", "Import your own exported website session files securely.", "▣", onOpenCookies)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("Female", "Male").forEach { voice ->
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                        .background(if (selectedVoice == voice) Crimson.copy(alpha = 0.20f) else Panel)
                        .border(1.dp, if (selectedVoice == voice) Crimson else PanelLine, RoundedCornerShape(12.dp))
                        .clickable { selectedVoice = voice }.padding(13.dp),
                    contentAlignment = Alignment.Center
                ) { Text("$voice voice", color = Ice, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("Hindi", "English").forEach { language ->
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                        .background(if (selectedLanguage == language) Violet.copy(alpha = 0.18f) else Panel)
                        .border(1.dp, if (selectedLanguage == language) Violet else PanelLine, RoundedCornerShape(12.dp))
                        .clickable { selectedLanguage = language }.padding(12.dp),
                    contentAlignment = Alignment.Center
                ) { Text(language, color = Ice, fontSize = 10.sp) }
            }
        }
        FeaturePanel("AI providers", "Gemini, ChatGPT and Grok — one active provider at a time.", "⌘", onOpenVault)
        FeaturePanel("Companion personality", "Friendly conversational style is a planned assistant mode. Responses still depend on the selected AI provider and its configured prompt.", "♡")
        FeaturePanel("YouTube & Music", "Voice commands can request YouTube searches. Playback behavior depends on the installed YouTube app and Android permissions.", "♫")
        FeaturePanel("Coding & Study Assistant", "Code explanation, debugging support and study guidance for supported topics.", "</>")
        FeaturePanel("Accessibility & automation", "Existing Android accessibility and automation services are retained.", "⌁")
        FeaturePanel("Interface", "JARVIS Quantum Neon theme gallery and accent presets.", "◈", onOpenThemes)
        Spacer(Modifier.height(10.dp))
        Text("VOICE CHOICES ARE A UI PREFERENCE PREVIEW; SYSTEM TTS WIRING IS STILL REQUIRED", color = Muted, fontSize = 8.sp, letterSpacing = 0.8.sp)
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
            Text("J", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("JARVIS", color = Ice, fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
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
    onDeveloperClick: () -> Unit,
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
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(Brush.horizontalGradient(listOf(Color(0x331A0A20), Color(0x443A102B), Color(0x331A0A20))))
                .border(1.dp, Brush.horizontalGradient(listOf(Crimson, Violet, Crimson)), RoundedCornerShape(12.dp))
                .padding(vertical = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("✦  DEVELOPED BY  NAEEM  ✦", color = Color(0xFFFFA5C0), fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.clickable(onClick = onDeveloperClick))
        }
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
    val transition = rememberInfiniteTransition(label = "jarvis-energy-core")
    val pulse by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.045f,
        animationSpec = infiniteRepeatable(tween(1900), RepeatMode.Reverse),
        label = "core-pulse"
    )
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(12000, easing = androidx.compose.animation.core.LinearEasing)),
        label = "outer-orbit"
    )
    val counterRotation by transition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(9000, easing = androidx.compose.animation.core.LinearEasing)),
        label = "inner-orbit"
    )

    Canvas(Modifier.size(176.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val unit = size.minDimension * 0.205f * pulse

        // Atmospheric glow and dark energy shell.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x66FF3158), Color(0x339B65FF), Color(0x005050FF)),
                center = center,
                radius = unit * 3.35f
            ),
            radius = unit * 3.35f,
            center = center
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFDCE5), Color(0xFFFF3158), Color(0xFF8B174C), Color(0xFF21102E)),
                center = Offset(center.x - unit * 0.22f, center.y - unit * 0.28f),
                radius = unit * 1.22f
            ),
            radius = unit * 1.02f,
            center = center
        )

        // Layered orbital tracks — deliberately asymmetric, not an arc-reactor motif.
        drawOval(
            color = Color(0x99FF7894),
            topLeft = Offset(center.x - unit * 2.02f, center.y - unit * 0.72f),
            size = androidx.compose.ui.geometry.Size(unit * 4.04f, unit * 1.44f),
            style = Stroke(width = 1.4.dp.toPx())
        )
        drawArc(
            color = Crimson,
            startAngle = rotation,
            sweepAngle = 118f,
            useCenter = false,
            topLeft = Offset(center.x - unit * 2.35f, center.y - unit * 2.35f),
            size = androidx.compose.ui.geometry.Size(unit * 4.7f, unit * 4.7f),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
        drawArc(
            color = Violet,
            startAngle = counterRotation + 38f,
            sweepAngle = 82f,
            useCenter = false,
            topLeft = Offset(center.x - unit * 2.68f, center.y - unit * 2.68f),
            size = androidx.compose.ui.geometry.Size(unit * 5.36f, unit * 5.36f),
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        )
        drawArc(
            color = Color(0x88FFB3C2),
            startAngle = rotation + 210f,
            sweepAngle = 36f,
            useCenter = false,
            topLeft = Offset(center.x - unit * 1.72f, center.y - unit * 1.72f),
            size = androidx.compose.ui.geometry.Size(unit * 3.44f, unit * 3.44f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Floating energy nodes.
        val orbitRadius = unit * 2.38f
        for (index in 0 until 6) {
            val angle = Math.toRadians((rotation + index * 60f).toDouble())
            val point = Offset(
                center.x + kotlin.math.cos(angle).toFloat() * orbitRadius,
                center.y + kotlin.math.sin(angle).toFloat() * orbitRadius
            )
            drawCircle(
                color = if (index % 2 == 0) Color(0xFFFFB5C5) else Color(0xFFB99AFF),
                radius = if (index % 2 == 0) 3.2.dp.toPx() else 2.4.dp.toPx(),
                center = point
            )
        }

        // Bright central nucleus.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color(0xFFFFA9BC), Color(0xFFFF3158), Color(0xFF5A174F)),
                center = Offset(center.x - unit * 0.12f, center.y - unit * 0.16f),
                radius = unit * 0.68f
            ),
            radius = unit * 0.58f,
            center = center
        )
        drawCircle(
            color = Color(0xCCFFE8EF),
            radius = unit * 0.13f,
            center = Offset(center.x - unit * 0.12f, center.y - unit * 0.16f)
        )
    }
}

