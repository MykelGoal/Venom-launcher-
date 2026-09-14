package com.venom.launcher.ui.screens

import android.Manifest
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.venom.launcher.data.AppInfo
import com.venom.launcher.data.ContactsRepository
import com.venom.launcher.data.IconPack
import com.venom.launcher.data.LauncherSettings
import com.venom.launcher.data.UsageRepository
import com.venom.launcher.ui.components.AppIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class DrawerTab(val label: String) {
    ALL("All"),
    RECENT("Recent"),
    GAMES("Games"),
    SOCIAL("Social"),
    TOOLS("Tools"),
}

@Composable
fun DrawerScreen(
    apps: List<AppInfo>,
    settings: LauncherSettings,
    pack: IconPack?,
    badges: Map<String, Int>,
    modifier: Modifier = Modifier,
    usage: Map<String, UsageRepository.UsageRow> = emptyMap(),
    gamePackages: Set<String> = emptySet(),
    onLaunch: (AppInfo) -> Unit,
    onLongPress: (AppInfo) -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(DrawerTab.ALL) }
    val focusRequester = remember { FocusRequester() }
    val gridState = rememberLazyGridState()
    val accent = MaterialTheme.colorScheme.primary
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val context = LocalContext.current

    // ------------------------------------------------------------ contacts --
    var contactsAllowed by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val contactsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> contactsAllowed = granted }

    val contacts by produceState<List<com.venom.launcher.data.ContactHit>>(
        initialValue = emptyList(), query, contactsAllowed
    ) {
        value = if (contactsAllowed && query.trim().length >= 2) {
            withContext(Dispatchers.IO) { ContactsRepository.search(context, query.trim()) }
        } else emptyList()
    }

    // ------------------------------------------------------------ filtering --
    val base = remember(apps, settings.hiddenApps) {
        apps.filter { it.componentKey !in settings.hiddenApps }
    }

    val categorized = remember(base, tab, gamePackages, usage) {
        when (tab) {
            DrawerTab.ALL -> base
            DrawerTab.RECENT -> base
                .sortedByDescending { usage[it.packageName]?.lastUsed ?: 0L }
                .take(24)
            DrawerTab.GAMES -> base.filter { it.packageName in gamePackages }
            DrawerTab.SOCIAL -> base.filter { categoryOf(it) == AppCategory.SOCIAL }
            DrawerTab.TOOLS -> base.filter { categoryOf(it) == AppCategory.TOOLS }
        }
    }

    val visible = remember(categorized, query, usage) {
        val q = query.trim().lowercase()
        if (q.isBlank()) {
            if (tab == DrawerTab.RECENT) categorized
            else categorized.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
        } else {
            categorized
                .map { it to scoreMatch(it.searchKey, q) }
                .filter { (_, score) -> score > 0 }
                .sortedWith(
                    compareByDescending<Pair<AppInfo, Int>> { it.second }
                        .thenByDescending { usage[it.first.packageName]?.totalMillis ?: 0L }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.first.label }
                )
                .map { it.first }
        }
    }

    // index of the first app starting with each letter, for the scrubber
    val letterIndex = remember(visible) {
        LinkedHashMap<Char, Int>().apply {
            visible.forEachIndexed { index, app -> putIfAbsent(app.firstLetter, index) }
        }
    }

    // --------------------------------------------------- universal results --
    val trimmed = query.trim()
    val math = remember(trimmed) { if (trimmed.length >= 3) evalMath(trimmed) else null }
    val settingHits = remember(trimmed) { matchSettings(trimmed) }

    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xF2070B0E))
            .statusBarsPadding(),
    ) {
        // ---- search ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(24.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                    cursorBrush = SolidColor(accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text(
                                text = "Search apps, people, settings, web…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.35f),
                            )
                        }
                        inner()
                    },
                )
            }
            Spacer(Modifier.size(8.dp))
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(24.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(24.dp))
                    .clickable { onOpenSettings() },
                contentAlignment = Alignment.Center,
            ) {
                Text("⚙", fontSize = 18.sp, color = Color.White.copy(alpha = 0.8f))
            }
        }

        // ---- category tabs ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DrawerTab.entries.forEach { entry ->
                val active = entry == tab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (active) accent.copy(alpha = 0.9f)
                            else Color.White.copy(alpha = 0.05f)
                        )
                        .clickable { tab = entry }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = entry.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (active) Color(0xFF06080A) else Color.White.copy(alpha = 0.6f),
                    )
                }
            }
        }

        // ---- universal results (only while searching) ----
        if (trimmed.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                math?.let { answer ->
                    ActionRow(
                        glyph = "=",
                        title = formatNumber(answer),
                        subtitle = trimmed,
                        accent = accent,
                        onClick = { copyText(context, formatNumber(answer)) },
                    )
                }

                settingHits.take(2).forEach { hit ->
                    ActionRow(
                        glyph = hit.glyph,
                        title = hit.title,
                        subtitle = "System settings",
                        accent = accent,
                        onClick = { openSystemSettings(context, hit.action) },
                    )
                }

                contacts.forEach { contact ->
                    ActionRow(
                        glyph = "☎",
                        title = contact.name,
                        subtitle = contact.number.ifBlank { "Contact" },
                        accent = accent,
                        onClick = { openContact(context, contact.contactId) },
                    )
                }

                if (!contactsAllowed && trimmed.length >= 2) {
                    ActionRow(
                        glyph = "☎",
                        title = "Search your contacts too",
                        subtitle = "Tap to allow contacts access",
                        accent = accent,
                        onClick = { contactsLauncher.launch(Manifest.permission.READ_CONTACTS) },
                    )
                }

                ActionRow(
                    glyph = "🌐",
                    title = "Search the web for “$trimmed”",
                    subtitle = "Opens your browser",
                    accent = accent,
                    onClick = { webSearch(context, trimmed) },
                )
            }
        }

        // ---- grid + alphabet scrubber ----
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(settings.drawerColumns),
                state = gridState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(items = visible, key = { it.componentKey }) { app ->
                    DrawerItem(
                        app = app,
                        pack = pack,
                        iconShape = settings.iconShape,
                        showLabel = settings.showLabels,
                        badgeCount = badges[app.packageName] ?: 0,
                        onClick = {
                            keyboard?.hide()
                            onLaunch(app)
                        },
                        onLongPress = { onLongPress(app) },
                    )
                }
                item { Spacer(Modifier.height(90.dp)) }
            }

            if (visible.size > 14 && query.isBlank()) {
                AlphabetScrubber(
                    letters = letterIndex.keys.toList(),
                    accent = accent,
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 4.dp),
                    onLetter = { letter ->
                        letterIndex[letter]?.let { index ->
                            scope.launch { gridState.animateScrollToItem(index) }
                        }
                    },
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
                .clickable { onClose() }
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 46.dp, height = 4.dp)
                    .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(2.dp)),
            )
        }
    }
}

// ------------------------------------------------------------------ rows ----

@Composable
private fun ActionRow(
    glyph: String,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.045f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(accent.copy(alpha = 0.12f), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(glyph, fontSize = 15.sp, color = accent)
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.45f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text("↗", color = Color.White.copy(alpha = 0.35f), fontSize = 14.sp)
    }
}

@Composable
private fun DrawerItem(
    app: AppInfo,
    pack: IconPack?,
    iconShape: com.venom.launcher.data.IconShape,
    showLabel: Boolean,
    badgeCount: Int,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickAndLongPress(onClick = onClick, onLongPress = onLongPress)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIcon(
            app = app,
            pack = pack,
            shape = iconShape,
            size = 52.dp,
            badgeCount = badgeCount,
        )
        if (showLabel) {
            Spacer(Modifier.size(6.dp))
            Text(
                text = app.label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun AlphabetScrubber(
    letters: List<Char>,
    accent: Color,
    modifier: Modifier = Modifier,
    onLetter: (Char) -> Unit,
) {
    Box(modifier = modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .pointerInput(letters.size) {
                    detectVerticalDragGestures { change, _ ->
                        val index = (change.position.y / size.height * letters.size)
                            .toInt()
                            .coerceIn(0, letters.lastIndex)
                        onLetter(letters[index])
                    }
                }
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            letters.forEach { letter ->
                Text(
                    text = letter.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = accent.copy(alpha = 0.75f),
                )
            }
        }
    }
}

/** Small helper so click + long-click stay readable at call sites. */
@Suppress("UNUSED_PARAMETER")
private fun Modifier.clickAndLongPress(
    onClick: () -> Unit,
    onLongPress: () -> Unit,
): Modifier = this.pointerInput(onClick, onLongPress) {
    detectTapGestures(
        onTap = { onClick() },
        onLongPress = { onLongPress() },
    )
}

// -------------------------------------------------------------- matching ----

private enum class AppCategory { SOCIAL, TOOLS, OTHER }

private val SOCIAL_HINTS = arrayOf(
    "whatsapp", "messenger", "facebook", "instagram", "snapchat", "telegram",
    "twitter", ".x.android", "threads", "tiktok", "linkedin", "reddit",
    "discord", "wechat", "line", "imo", "viber", "skype", "signal", "viber",
)

private val TOOL_HINTS = arrayOf(
    "calculator", "calculatrice", "clock", "calendar", "contacts", "files",
    "filemanager", "my files", "notes", "keep", "drive", "dropbox", "compress",
    "zip", "rar", "scanner", "pdf", "translate", "memo", "recorder", "cleaner",
)

private fun categoryOf(app: AppInfo): AppCategory {
    val key = (app.packageName + " " + app.label).lowercase()
    if (SOCIAL_HINTS.any { key.contains(it) }) return AppCategory.SOCIAL
    if (TOOL_HINTS.any { key.contains(it) }) return AppCategory.TOOLS
    return AppCategory.OTHER
}

/** Higher is better. 0 means "no match". */
private fun scoreMatch(key: String, query: String): Int {
    if (key == query) return 100
    if (key.startsWith(query)) return 80
    if (key.contains(" $query")) return 60
    if (key.contains(query)) return 40
    if (query.all { it.isDigit() } && t9Pattern(query).toRegex().containsMatchIn(key)) return 35
    if (isSubsequence(key, query)) return 20
    return 0
}

private fun t9Pattern(digits: String): String {
    val map = mapOf(
        '0' to "0 ", '1' to "1", '2' to "2abc", '3' to "3def", '4' to "4ghi",
        '5' to "5jkl", '6' to "6mno", '7' to "7pqrs", '8' to "8tuv", '9' to "9wxyz"
    )
    return buildString {
        append(".*")
        digits.forEach { d ->
            val letters = map[d] ?: return@forEach
            append('[').append(letters).append(']')
        }
        append(".*")
    }
}

private fun isSubsequence(text: String, query: String): Boolean {
    var i = 0
    for (ch in text) {
        if (i < query.length && ch == query[i]) i++
        if (i == query.length) return true
    }
    return i == query.length
}

// ------------------------------------------------------------ calculator ----

/** A deliberately small, safe arithmetic evaluator — no script engines. */
private fun evalMath(input: String): Double? {
    val expr = input.replace(" ", "")
    if (expr.isEmpty()) return null
    if (!expr.any { it.isDigit() }) return null
    if (expr.any { it !in "0123456789+-*/%.()" }) return null
    return runCatching {
        val tokens = tokenize(expr) ?: return@runCatching null
        val parser = MathParser(tokens)
        val value = parser.parseExpression()
        if (parser.done()) value else null
    }.getOrNull()?.takeIf { !it.isNaN() && !it.isInfinite() }
}

private fun tokenize(expr: String): List<String>? {
    val tokens = ArrayList<String>()
    var i = 0
    while (i < expr.length) {
        val c = expr[i]
        when {
            c.isDigit() || c == '.' -> {
                val start = i
                var dots = 0
                while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                    if (expr[i] == '.') dots++
                    if (dots > 1) return null
                    i++
                }
                val number = expr.substring(start, i)
                if (number.toDoubleOrNull() == null) return null
                tokens.add(number)
            }
            c in "+-*/%()" -> {
                tokens.add(c.toString())
                i++
            }
            else -> return null
        }
    }
    return tokens
}

private class MathParser(private val tokens: List<String>) {
    private var pos = 0
    fun done() = pos >= tokens.size
    private fun peek() = tokens.getOrNull(pos)
    private fun next() = tokens.getOrNull(pos++)

    fun parseExpression(): Double {
        var value = parseTerm()
        while (peek() in listOf("+", "-")) {
            val op = next()
            value = if (op == "+") value + parseTerm() else value - parseTerm()
        }
        return value
    }

    private fun parseTerm(): Double {
        var value = parseFactor()
        while (peek() in listOf("*", "/", "%")) {
            when (next()) {
                "*" -> value *= parseFactor()
                "/" -> value /= parseFactor()
                "%" -> value %= parseFactor()
            }
        }
        return value
    }

    private fun parseFactor(): Double {
        val token = next() ?: throw ArithmeticException("eof")
        if (token == "(") {
            val value = parseExpression()
            if (next() != ")") throw ArithmeticException(")")
            return value
        }
        val value = token.toDoubleOrNull() ?: throw ArithmeticException(token)
        if (peek() == "%") { next(); return value / 100.0 }
        return value
    }
}

private fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else "%.6f".format(value).trimEnd('0')

// --------------------------------------------------------------- actions ----

private data class SettingHit(val title: String, val glyph: String, val action: String)

private val SETTINGS_SHORTCUTS = listOf(
    SettingHit("Wi-Fi", "📶", Settings.ACTION_WIFI_SETTINGS),
    SettingHit("Bluetooth", "🅱", Settings.ACTION_BLUETOOTH_SETTINGS),
    SettingHit("Display & brightness", "☀", Settings.ACTION_DISPLAY_SETTINGS),
    SettingHit("Sound & vibration", "🔊", Settings.ACTION_SOUND_SETTINGS),
    SettingHit("Battery", "⚡", Settings.ACTION_BATTERY_SAVER_SETTINGS),
    SettingHit("Storage", "💾", Settings.ACTION_INTERNAL_STORAGE_SETTINGS),
    SettingHit("Location", "📍", Settings.ACTION_LOCATION_SOURCE_SETTINGS),
    SettingHit("Airplane mode", "✈", Settings.ACTION_AIRPLANE_MODE_SETTINGS),
    SettingHit("Language", "🅰", Settings.ACTION_LOCALE_SETTINGS),
    SettingHit("Date & time", "🕐", Settings.ACTION_DATE_SETTINGS),
    SettingHit("Security", "🔒", Settings.ACTION_SECURITY_SETTINGS),
    SettingHit("Apps", "📦", Settings.ACTION_APPLICATION_SETTINGS),
    SettingHit("Accessibility", "♿", Settings.ACTION_ACCESSIBILITY_SETTINGS),
    SettingHit("Developer options", "🛠", Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS),
)

private val SETTINGS_KEYWORDS = mapOf(
    "wifi" to 0, "wi-fi" to 0, "network" to 0, "internet" to 0,
    "bluetooth" to 1, "bt" to 1, "headphone" to 1,
    "display" to 2, "brightness" to 2, "screen" to 2, "dark" to 2,
    "sound" to 3, "volume" to 3, "ring" to 3, "vibrate" to 3,
    "battery" to 4, "power" to 4, "charging" to 4,
    "storage" to 5, "memory" to 5, "space" to 5,
    "location" to 6, "gps" to 6, "maps" to 6,
    "airplane" to 7, "flight" to 7, "offline" to 7,
    "language" to 8, "locale" to 8,
    "date" to 9, "time" to 9, "clock" to 9,
    "security" to 10, "lock" to 10, "password" to 10, "fingerprint" to 10,
    "apps" to 11, "app" to 11, "uninstall" to 11, "installed" to 11,
    "accessibility" to 12,
    "developer" to 13, "dev" to 13, "usb" to 13,
)

private fun matchSettings(query: String): List<SettingHit> {
    if (query.length < 3) return emptyList()
    val q = query.lowercase()
    val hits = LinkedHashMap<Int, SettingHit>()
    SETTINGS_KEYWORDS.forEach { (keyword, index) ->
        if (q.contains(keyword) || (keyword.length >= 4 && keyword.contains(q))) {
            hits.putIfAbsent(index, SETTINGS_SHORTCUTS[index])
        }
    }
    if (hits.isEmpty()) {
        SETTINGS_SHORTCUTS.forEachIndexed { index, hit ->
            if (hit.title.lowercase().contains(q)) hits.putIfAbsent(index, hit)
        }
    }
    return hits.values.toList()
}

private fun openSystemSettings(context: Context, action: String) {
    runCatching {
        context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.onFailure {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}

private fun webSearch(context: Context, query: String) {
    val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
        putExtra(SearchManager.QUERY, query)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val launched = runCatching { context.startActivity(intent) }.isSuccess
    if (!launched) {
        runCatching {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://www.google.com/search?q=${android.net.Uri.encode(query)}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}

private fun openContact(context: Context, contactId: Long) {
    val uri = android.content.ContentUris.withAppendedId(
        ContactsContract.Contacts.CONTENT_URI, contactId
    )
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

private fun copyText(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
    clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("result", text))
    android.widget.Toast.makeText(context, "Copied $text", android.widget.Toast.LENGTH_SHORT).show()
}
