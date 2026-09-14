package com.venom.launcher.ui.components

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.venom.launcher.data.VenomWidgetHost

/**
 * Hosts a real Android widget inside Compose.
 *
 * The fiddly part of widget hosting is telling the remote view how big it is:
 * we measure in pixels, convert to dp, and push those numbers through
 * [AppWidgetHostView.updateAppWidgetSize] whenever our box changes size.
 */
@Composable
fun WidgetHostView(
    appWidgetId: Int,
    modifier: Modifier = Modifier,
    onMissing: () -> Unit = {},
) {
    val context = LocalContext.current
    val host = remember { VenomWidgetHost.get(context) }
    val manager = remember { AppWidgetManager.getInstance(context.applicationContext) }
    val info = remember(appWidgetId) {
        runCatching { manager.getAppWidgetInfo(appWidgetId) }.getOrNull()
    }

    var sizePx by remember { mutableStateOf(IntSize.Zero) }

    if (info == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(6.dp)
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Widget unavailable",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.45f),
            )
        }
        androidx.compose.runtime.LaunchedEffect(appWidgetId) { onMissing() }
        return
    }

    AndroidView(
        factory = { ctx -> createHostView(ctx, host, appWidgetId, info) },
        update = { view ->
            if (sizePx != IntSize.Zero) {
                pushSize(view, context, sizePx)
            }
        },
        modifier = modifier.onSizePxChanged { sizePx = it },
    )
}

private fun createHostView(
    context: Context,
    host: AppWidgetHost,
    appWidgetId: Int,
    info: android.appwidget.AppWidgetProviderInfo,
): AppWidgetHostView = host.createView(context, appWidgetId, info).apply {
    setAppWidget(appWidgetId, info)
}

private fun pushSize(view: AppWidgetHostView, context: Context, size: IntSize) {
    val density = context.resources.displayMetrics.density
    val wDp = (size.width / density).toInt().coerceAtLeast(40)
    val hDp = (size.height / density).toInt().coerceAtLeast(40)
    runCatching {
        view.updateAppWidgetSize(Bundle(), wDp, hDp, wDp, hDp)
    }
}

private fun Modifier.onSizePxChanged(block: (IntSize) -> Unit): Modifier =
    this.onSizeChanged { size -> block(size) }
