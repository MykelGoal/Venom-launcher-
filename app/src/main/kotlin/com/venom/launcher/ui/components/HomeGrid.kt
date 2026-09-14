package com.venom.launcher.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.venom.launcher.data.AppInfo
import com.venom.launcher.data.AppItem
import com.venom.launcher.data.FolderItem
import com.venom.launcher.data.IconPack
import com.venom.launcher.data.IconShape
import com.venom.launcher.data.LauncherItem
import com.venom.launcher.data.WidgetItem
import kotlin.math.roundToInt

/**
 * One page of the home screen.
 *
 * Long-press and drag an icon to move it; drop it on another icon to merge them
 * into a folder; drag it into the bottom strip to remove it. Widgets render
 * across their full span, and cells they cover are skipped.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomePage(
    items: List<LauncherItem?>,
    columns: Int,
    rows: Int,
    appsByKey: Map<String, AppInfo>,
    pack: IconPack?,
    iconShape: IconShape,
    iconScale: Float,
    showLabels: Boolean,
    labelLines: Int,
    badges: Map<String, Int>,
    widgetContent: @Composable (WidgetItem, Dp, Dp) -> Unit,
    modifier: Modifier = Modifier,
    onLaunchApp: (AppInfo) -> Unit,
    onOpenFolder: (Int) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onCreateFolder: (from: Int, to: Int) -> Unit,
    onDropIntoFolder: (folderSlot: Int, key: String) -> Unit,
    onRemove: (Int) -> Unit,
    onLongPressEmpty: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current

    var dragFrom by remember { mutableIntStateOf(-1) }
    var dragPos by remember { mutableStateOf(Offset.Zero) }
    var draggingItem by remember { mutableStateOf<LauncherItem?>(null) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(columns, rows, items.size) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        val slot = slotAt(offset, size.width.toFloat() / columns, size.height.toFloat() / rows, columns, items.size)
                        val item = items.getOrNull(slot)
                        if (slot >= 0 && item != null) {
                            dragFrom = slot
                            draggingItem = item
                            dragPos = offset
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        } else {
                            onLongPressEmpty()
                        }
                    },
                    onDrag = { _, amount -> dragPos += amount },
                    onDragEnd = {
                        val to = slotAt(
                            dragPos,
                            size.width.toFloat() / columns,
                            size.height.toFloat() / rows,
                            columns,
                            items.size,
                        )
                        val from = dragFrom
                        val dragged = draggingItem
                        if (from >= 0 && dragged != null) {
                            val inRemoveZone = dragPos.y > size.height - with(density) { 84.dp.toPx() }
                            when {
                                inRemoveZone -> onRemove(from)
                                to >= 0 && to != from -> {
                                    when (val target = items.getOrNull(to)) {
                                        null -> onMove(from, to)
                                        is FolderItem -> {
                                            val key = (dragged as? AppItem)?.key
                                            if (key != null) onDropIntoFolder(to, key)
                                        }

                                        is AppItem -> if (dragged is AppItem) onCreateFolder(from, to)
                                        else -> onMove(from, to)
                                    }
                                }
                            }
                        }
                        dragFrom = -1
                        draggingItem = null
                    },
                    onDragCancel = {
                        dragFrom = -1
                        draggingItem = null
                    },
                )
            },
    ) {
        val cellW = maxWidth / columns
        val cellH = maxHeight / rows
        val iconSize = (minOf(cellW, cellH) * 0.62f * iconScale).coerceIn(28.dp, 96.dp)
        val removeZonePx = with(density) { 84.dp.toPx() }

        // Cells covered by a multi-span widget are not drawn on their own.
        val covered = BooleanArray(items.size)
        items.forEachIndexed { index, item ->
            if (item is WidgetItem) {
                val col = index % columns
                val row = index / columns
                for (r in row until (row + item.spanY).coerceAtMost(rows)) {
                    for (c in col until (col + item.spanX).coerceAtMost(columns)) {
                        covered[r * columns + c] = true
                    }
                }
            }
        }

        items.forEachIndexed { index, item ->
            if (item == null) return@forEachIndexed
            val col = index % columns
            val row = index / columns
            if (row >= rows) return@forEachIndexed

            val spanX = (item as? WidgetItem)?.spanX ?: 1
            val spanY = (item as? WidgetItem)?.spanY ?: 1
            val isDragged = index == dragFrom

            Box(
                modifier = Modifier
                    .offset(x = cellW * col, y = cellH * row)
                    .size(cellW * spanX, cellH * spanY)
                    .graphicsLayer {
                        alpha = if (isDragged) 0.25f else 1f
                    },
                contentAlignment = Alignment.Center,
            ) {
                when (item) {
                    is WidgetItem -> widgetContent(
                        item,
                        cellW * spanX,
                        cellH * spanY,
                    )

                    is AppItem -> {
                        val app = appsByKey[item.key]
                        if (app != null) {
                            GridIcon(
                                label = app.label,
                                showLabel = showLabels,
                                labelLines = labelLines,
                                iconSize = iconSize,
                                width = cellW * spanX,
                                badgeCount = badges[app.packageName] ?: 0,
                                onClick = { onLaunchApp(app) },
                            ) {
                                AppIcon(
                                    app = app,
                                    pack = pack,
                                    shape = iconShape,
                                    size = iconSize,
                                )
                            }
                        }
                    }

                    is FolderItem -> {
                        FolderIcon(
                            folder = item,
                            appsByKey = appsByKey,
                            pack = pack,
                            iconShape = iconShape,
                            iconSize = iconSize,
                            width = cellW,
                            showLabel = showLabels,
                            labelLines = labelLines,
                            onOpen = { onOpenFolder(index) },
                        )
                    }
                }
            }
        }

        // Floating copy that follows the finger
        val dragged = draggingItem
        if (dragged != null) {
            val px = with(density) { iconSize.toPx() }
            val inRemoveZone = dragPos.y > constraints.maxHeight - removeZonePx
            Box(
                modifier = Modifier
                    .offset {
                        androidx.compose.ui.unit.IntOffset(
                            (dragPos.x - px / 2f).roundToInt(),
                            (dragPos.y - px / 2f).roundToInt(),
                        )
                    }
                    .size(iconSize)
                    .graphicsLayer {
                        scaleX = if (inRemoveZone) 0.7f else 1.18f
                        scaleY = if (inRemoveZone) 0.7f else 1.18f
                        alpha = if (inRemoveZone) 0.6f else 1f
                        shadowElevation = 24f
                    },
                contentAlignment = Alignment.Center,
            ) {
                val app = (dragged as? AppItem)?.let { appsByKey[it.key] }
                if (app != null) {
                    AppIcon(app = app, pack = pack, shape = iconShape, size = iconSize)
                } else {
                    Text("▣", color = Color.White, fontSize = 26.sp)
                }
            }
        }

        // Remove target
        if (dragged != null) {
            val inRemoveZone = dragPos.y > constraints.maxHeight - removeZonePx
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp)
                    .background(
                        if (inRemoveZone) Color(0xFFFF3B4E) else Color.White.copy(alpha = 0.06f),
                        RoundedCornerShape(20.dp),
                    )
                    .border(
                        1.dp,
                        if (inRemoveZone) Color.White.copy(alpha = 0.5f)
                        else Color.White.copy(alpha = 0.12f),
                        RoundedCornerShape(20.dp),
                    )
                    .padding(horizontal = 22.dp, vertical = 10.dp),
            ) {
                Text(
                    text = "Remove",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                )
            }
        }
    }
}

private fun slotAt(
    offset: Offset,
    cellW: Float,
    cellH: Float,
    columns: Int,
    size: Int,
): Int {
    if (cellW <= 0f || cellH <= 0f) return -1
    val col = (offset.x / cellW).toInt().coerceIn(0, columns - 1)
    val row = (offset.y / cellH).toInt().coerceAtLeast(0)
    val index = row * columns + col
    return if (index in 0 until size) index else -1
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GridIcon(
    label: String,
    showLabel: Boolean,
    labelLines: Int,
    iconSize: Dp,
    width: Dp,
    badgeCount: Int,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .size(width)
            .combinedClickable(onClick = onClick, onLongClick = {})
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        icon()
        if (showLabel) {
            androidx.compose.foundation.layout.Spacer(Modifier.size(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = labelLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderIcon(
    folder: FolderItem,
    appsByKey: Map<String, AppInfo>,
    pack: IconPack?,
    iconShape: IconShape,
    iconSize: Dp,
    width: Dp,
    showLabel: Boolean,
    labelLines: Int,
    onOpen: () -> Unit,
) {
    val preview = folder.items.take(4).mapNotNull { appsByKey[it] }
    val smallSize = iconSize * 0.44f

    Column(
        modifier = Modifier
            .size(width)
            .combinedClickable(onClick = onOpen, onLongClick = {})
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(iconSize * 0.30f))
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.10f),
                    RoundedCornerShape(iconSize * 0.30f),
                ),
            contentAlignment = Alignment.Center,
        ) {
            // 2x2 preview of what's inside
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            ) {
                for (rowItems in preview.chunked(2)) {
                    androidx.compose.foundation.layout.Row {
                        for (app in rowItems) {
                            AppIcon(
                                app = app,
                                pack = pack,
                                shape = iconShape,
                                size = smallSize,
                                modifier = Modifier.padding(2.dp),
                            )
                        }
                    }
                }
                if (preview.isEmpty()) {
                    Text("＋", color = Color.White.copy(alpha = 0.6f), fontSize = 18.sp)
                }
            }
        }
        if (showLabel) {
            androidx.compose.foundation.layout.Spacer(Modifier.size(5.dp))
            Text(
                text = folder.label.ifBlank { "Folder" },
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = labelLines,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
