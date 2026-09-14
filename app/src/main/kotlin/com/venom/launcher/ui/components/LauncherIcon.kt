package com.venom.launcher.ui.components

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.venom.launcher.data.AppInfo
import com.venom.launcher.data.IconPack
import com.venom.launcher.data.IconShape
import com.venom.launcher.data.IconLoader
import com.venom.launcher.ui.theme.VenomSquircle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Async icon loader with an in-memory LRU behind it. */
@Composable
fun rememberAppIcon(
    app: AppInfo?,
    pack: IconPack?,
    shape: IconShape,
): ImageBitmap? {
    val context = LocalContext.current
    if (app == null) return null

    val cacheKey = "${app.componentKey}|${shape.name}|${pack?.packageName}"
    var bitmap by remember(cacheKey) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(cacheKey) {
        bitmap = withContext(Dispatchers.IO) {
            IconLoader.load(context.applicationContext, app, pack, shape)
        }
    }
    return bitmap
}

@Composable
fun AppIcon(
    app: AppInfo?,
    pack: IconPack?,
    shape: IconShape,
    size: Dp,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
) {
    val bitmap = rememberAppIcon(app = app, pack = pack, shape = shape)

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = app?.label,
                modifier = Modifier
                    .size(size)
                    .then(
                        if (shape == IconShape.SYSTEM) Modifier
                        else Modifier.clip(VenomSquircle)
                    ),
                contentScale = ContentScale.Fit,
            )
        } else {
            Spacer(modifier = Modifier.size(size))
        }

        if (badgeCount > 0) {
            BadgeDot(
                count = badgeCount,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp),
            )
        }
    }
}

@Composable
fun BadgeDot(count: Int, modifier: Modifier = Modifier) {
    val size = if (count > 9) 22.dp else 20.dp
    Box(
        modifier = modifier
            .size(size)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .then(Modifier),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(size)) {
            drawCircle(color = androidx.compose.ui.graphics.Color(0xFFFF3B4E), radius = size.toPx() / 2f)
        }
        Text(
            text = if (count > 99) "99+" else count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = androidx.compose.ui.graphics.Color.White,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
fun IconLabel(
    text: String,
    maxLines: Int,
    width: Dp,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = androidx.compose.ui.graphics.Color.White,
        textAlign = TextAlign.Center,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** Shared helper so icon rendering stays consistent across home / drawer / dock. */
fun iconCacheKey(context: Context, app: AppInfo, shape: IconShape, pack: IconPack?): String =
    "${app.componentKey}|${shape.name}|${pack?.packageName}"
