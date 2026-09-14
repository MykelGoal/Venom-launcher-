package com.venom.launcher.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.text.BasicTextField
import com.venom.launcher.data.AppInfo
import com.venom.launcher.data.IconPack
import com.venom.launcher.data.LauncherSettings
import com.venom.launcher.ui.components.AppIcon

@Composable
fun DrawerScreen(
    apps: List<AppInfo>,
    settings: LauncherSettings,
    pack: IconPack?,
    badges: Map<String, Int>,
    modifier: Modifier = Modifier,
    onLaunch: (AppInfo) -> Unit,
    onLongPress: (AppInfo) -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val gridState = rememberLazyGridState()
    val accent = MaterialTheme.colorScheme.primary
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val visible = remember(apps, query, settings.hiddenApps) {
        val q = query.trim().lowercase()
        apps
            .filter { it.componentKey !in settings.hiddenApps }
            .filter { q.isBlank() || it.searchKey.contains(q) }
            .sortedWith(
                compareBy<AppInfo> { if (q.isBlank()) -1 else if (it.searchKey.startsWith(q)) 0 else 1 }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.label }
            )
    }

    // index of the first app starting with each letter, for the scrubber
    val letterIndex = remember(visible) {
        LinkedHashMap<Char, Int>().apply {
            visible.forEachIndexed { index, app ->
                putIfAbsent(app.firstLetter, index)
            }
        }
    }

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
                                text = "Search ${apps.size} apps",
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
