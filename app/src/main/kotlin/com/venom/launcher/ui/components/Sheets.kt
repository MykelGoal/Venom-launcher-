package com.venom.launcher.ui.components

import android.content.pm.ShortcutInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import com.venom.launcher.data.AppInfo
import com.venom.launcher.data.IconPack
import com.venom.launcher.data.IconShape

// ------------------------------------------------------------ app options ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppOptionsSheet(
    app: AppInfo,
    shortcuts: List<ShortcutInfo>,
    pack: IconPack?,
    iconShape: IconShape,
    isHidden: Boolean,
    modifier: Modifier = Modifier,
    onLaunch: () -> Unit,
    onAddToHome: () -> Unit,
    onAddToDock: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
    onToggleHidden: () -> Unit,
    onShortcut: (ShortcutInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accent = MaterialTheme.colorScheme.primary

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF12181C),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 14.dp),
            ) {
                AppIcon(app = app, pack = pack, shape = iconShape, size = 46.dp)
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.4f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            SheetAction("Open", accent = accent, onClick = onLaunch)

            if (shortcuts.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.035f))
                        .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(18.dp))
                        .padding(8.dp),
                ) {
                    Text(
                        text = "SHORTCUTS",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent.copy(alpha = 0.8f),
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 6.dp),
                    )
                    for (shortcut in shortcuts) {
                        ShortcutRow(shortcut = shortcut, onClick = { onShortcut(shortcut) })
                    }
                }
            }

            Column(modifier = Modifier.padding(top = 12.dp)) {
                SheetAction("Add to home screen", onClick = onAddToHome)
                SheetAction("Add to dock", onClick = onAddToDock)
                SheetAction(if (isHidden) "Unhide" else "Hide from drawer") {
                    onToggleHidden()
                }
                SheetAction("App info", onClick = onAppInfo)
                SheetAction("Uninstall", destructive = true, onClick = onUninstall)
            }
        }
    }
}

@Composable
private fun ShortcutRow(shortcut: ShortcutInfo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShortcutIcon(shortcut = shortcut)
        Text(
            text = shortcut.shortLabel?.toString()
                ?: shortcut.longLabel?.toString()
                ?: "Shortcut",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

@Composable
private fun ShortcutIcon(shortcut: ShortcutInfo) {
    val label = shortcut.shortLabel?.toString()
        ?: shortcut.longLabel?.toString()
        ?: ""
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.07f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.firstOrNull()?.uppercaseChar()?.toString()?.takeIf { it.isNotBlank() } ?: "\u2192",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun SheetAction(
    label: String,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
    accent: Color = Color.White,
    onClick: () -> Unit,
) {
    val tint = if (destructive) Color(0xFFFF5C6C) else Color.White
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (destructive) tint.copy(alpha = 0.06f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (destructive) tint else Color.White.copy(alpha = 0.92f),
        )
    }
}

// ------------------------------------------------------------ home menu ------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeMenuSheet(
    pageCount: Int,
    onWallpapers: () -> Unit,
    onWidgets: () -> Unit,
    onSettings: () -> Unit,
    onAddPage: () -> Unit,
    onRemovePage: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val accent = MaterialTheme.colorScheme.primary

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF12181C),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 28.dp),
        ) {
            SheetAction("Widgets", accent = accent, onClick = onWidgets)
            SheetAction("Wallpapers", onClick = onWallpapers)
            SheetAction("Venom settings", onClick = onSettings)
            SheetAction("Add page", onClick = onAddPage)
            if (pageCount > 1) {
                SheetAction("Remove this page", destructive = true) {
                    onRemovePage()
                }
            }
        }
    }
}

// --------------------------------------------------------- generic picker ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> PickerSheet(
    title: String,
    items: List<T>,
    selected: (T) -> Boolean,
    labelOf: (T) -> String,
    modifier: Modifier = Modifier,
    onPick: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val accent = MaterialTheme.colorScheme.primary

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF12181C),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(bottom = 10.dp),
            )
            LazyColumn(
                modifier = Modifier.heightIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(items) { item ->
                    val isSelected = selected(item)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) accent.copy(alpha = 0.14f)
                                else Color.Transparent
                            )
                            .border(
                                1.dp,
                                if (isSelected) accent.copy(alpha = 0.5f)
                                else Color.White.copy(alpha = 0.05f),
                                RoundedCornerShape(14.dp),
                            )
                            .clickable { onPick(item) }
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = labelOf(item),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) accent else Color.White,
                        )
                    }
                }
            }
        }
    }
}
