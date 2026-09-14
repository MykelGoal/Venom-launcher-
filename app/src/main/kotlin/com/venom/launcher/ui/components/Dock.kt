package com.venom.launcher.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.venom.launcher.data.AppInfo
import com.venom.launcher.data.AppItem
import com.venom.launcher.data.IconPack
import com.venom.launcher.data.IconShape
import com.venom.launcher.data.LauncherItem
import com.venom.launcher.ui.theme.VenomGlass
import com.venom.launcher.ui.theme.VenomGlassBorder

/** Glass dock. Long-press a slot for its options. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DockRow(
    items: List<LauncherItem?>,
    slots: Int,
    appsByKey: Map<String, AppInfo>,
    pack: IconPack?,
    iconShape: IconShape,
    iconScale: Float,
    showLabels: Boolean,
    badges: Map<String, Int>,
    modifier: Modifier = Modifier,
    onLaunchApp: (AppInfo) -> Unit,
    onLongPress: (Int) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .background(VenomGlass, RoundedCornerShape(28.dp))
            .border(1.dp, VenomGlassBorder, RoundedCornerShape(28.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            for (slot in 0 until slots) {
                val item = items.getOrNull(slot)
                val app = (item as? AppItem)?.let { appsByKey[it.key] }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .combinedClickable(
                            onClick = { if (app != null) onLaunchApp(app) else onLongPress(slot) },
                            onLongClick = { onLongPress(slot) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (app != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AppIcon(
                                app = app,
                                pack = pack,
                                shape = iconShape,
                                size = (44.dp * iconScale).coerceIn(30.dp, 68.dp),
                                badgeCount = badges[app.packageName] ?: 0,
                            )
                            if (showLabels) {
                                Text(
                                    text = app.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.72f),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 2.dp, vertical = 2.dp),
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size((44.dp * iconScale).coerceIn(30.dp, 68.dp))
                                .background(
                                    Color.White.copy(alpha = 0.05f),
                                    RoundedCornerShape(14.dp),
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
    accent: Color = Color.White,
) {
    if (pageCount <= 1) return
    Row(
        modifier = modifier.padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            Box(
                modifier = Modifier
                    .size(width = if (selected) 18.dp else 6.dp, height = 6.dp)
                    .background(
                        if (selected) accent else Color.White.copy(alpha = 0.28f),
                        RoundedCornerShape(3.dp),
                    ),
            )
        }
    }
}
