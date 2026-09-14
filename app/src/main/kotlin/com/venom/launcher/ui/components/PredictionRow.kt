package com.venom.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.venom.launcher.data.AppInfo
import com.venom.launcher.data.IconPack
import com.venom.launcher.data.IconShape
import com.venom.launcher.ui.theme.VenomGlass
import com.venom.launcher.ui.theme.VenomGlassBorder

/**
 * Venom's signature row: the apps you're most likely to want *right now*,
 * ranked from a per-hour usage histogram. It changes through the day.
 */
@Composable
fun PredictionRow(
    apps: List<AppInfo>,
    pack: IconPack?,
    iconShape: IconShape,
    badges: Map<String, Int>,
    modifier: Modifier = Modifier,
    onLaunch: (AppInfo) -> Unit,
    onLongPress: (AppInfo) -> Unit,
) {
    if (apps.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(VenomGlass)
            .border(1.dp, VenomGlassBorder, RoundedCornerShape(22.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "▸",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 8.dp),
        )

        LazyRow(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(items = apps, key = { it.componentKey }) { app ->
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onLaunch(app) }
                        .then(Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    AppIcon(
                        app = app,
                        pack = pack,
                        shape = iconShape,
                        size = 38.dp,
                        badgeCount = badges[app.packageName] ?: 0,
                    )
                }
            }
        }
    }
}
