package com.venom.launcher.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.venom.launcher.data.AppInfo
import com.venom.launcher.data.BatteryState
import com.venom.launcher.data.HomeLayout
import com.venom.launcher.data.IconPack
import com.venom.launcher.data.LauncherSettings
import com.venom.launcher.ui.components.ClockHeader
import com.venom.launcher.ui.components.DockRow
import com.venom.launcher.ui.components.HomePage
import com.venom.launcher.ui.components.PageIndicator
import com.venom.launcher.ui.components.PredictionRow
import com.venom.launcher.ui.components.WallpaperBackground
import com.venom.launcher.ui.components.WidgetHostView

/**
 * The home screen: wallpaper, clock, prediction row, paged icon grid, dock.
 *
 * Gestures
 *  - swipe up / down anywhere on the grid -> configurable actions
 *  - swipe left / right on the clock strip -> configurable actions
 *  - double tap anywhere -> configurable action
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    battery: BatteryState,
    layout: HomeLayout,
    appsByKey: Map<String, AppInfo>,
    settings: LauncherSettings,
    pack: IconPack?,
    badges: Map<String, Int>,
    predictions: List<AppInfo>,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    onLaunchApp: (AppInfo) -> Unit,
    onOpenFolder: (page: Int, slot: Int) -> Unit,
    onMove: (page: Int, from: Int, to: Int) -> Unit,
    onCreateFolder: (page: Int, from: Int, to: Int) -> Unit,
    onDropIntoFolder: (page: Int, folderSlot: Int, key: String) -> Unit,
    onRemoveItem: (page: Int, slot: Int) -> Unit,
    onMissingWidget: (Int) -> Unit,
    onLongPressEmpty: () -> Unit,
    onDockLongPress: (Int) -> Unit,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onDoubleTap: () -> Unit,
    onOpenChargeLab: () -> Unit,
) {
    val density = LocalDensity.current
    val swipeThresholdPx = remember(density) { with(density) { 78.dp.toPx() } }
    val accent = MaterialTheme.colorScheme.primary

    Box(modifier = modifier.fillMaxSize()) {
        WallpaperBackground(
            dim = settings.wallpaperDim,
            blur = settings.wallpaperBlur,
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.statusBarsPadding())

            // ---- gesture strip: clock + predictions (horizontal swipes) ----
            var horizontalAccumulator by remember { mutableFloatStateOf(0f) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(settings.swipeLeft, settings.swipeRight) {
                        detectHorizontalDragGestures(
                            onDragStart = { horizontalAccumulator = 0f },
                            onDragEnd = {
                                when {
                                    horizontalAccumulator < -swipeThresholdPx -> onSwipeLeft()
                                    horizontalAccumulator > swipeThresholdPx -> onSwipeRight()
                                }
                                horizontalAccumulator = 0f
                            },
                            onDragCancel = { horizontalAccumulator = 0f },
                            onHorizontalDrag = { _, amount -> horizontalAccumulator += amount },
                        )
                    },
            ) {
                if (settings.showClock) {
                    ClockHeader(
                        battery = battery,
                        onOpenChargeLab = onOpenChargeLab,
                    )
                }
                if (settings.predictiveRow) {
                    PredictionRow(
                        apps = predictions,
                        pack = pack,
                        iconShape = settings.iconShape,
                        badges = badges,
                        onLaunch = onLaunchApp,
                        onLongPress = onLaunchApp,
                    )
                }
            }

            // ---- paged grid (vertical swipes + double tap) ----
            var dragTotal by remember { mutableFloatStateOf(0f) }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .draggable(
                        state = rememberDraggableState { delta -> dragTotal += delta },
                        orientation = Orientation.Vertical,
                        onDragStarted = { dragTotal = 0f },
                        onDragStopped = { velocity ->
                            when {
                                dragTotal < -swipeThresholdPx || velocity < -1400f -> onSwipeUp()
                                dragTotal > swipeThresholdPx || velocity > 1400f -> onSwipeDown()
                            }
                            dragTotal = 0f
                        },
                    )
                    .pointerInput(settings.doubleTap) {
                        detectTapGestures(onDoubleTap = { onDoubleTap() })
                    },
            ) {
                val pageCount = layout.pages.size
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    val items = layout.pages.getOrNull(page) ?: emptyList()
                    HomePage(
                        items = items,
                        columns = settings.gridColumns,
                        rows = settings.gridRows,
                        appsByKey = appsByKey,
                        pack = pack,
                        iconShape = settings.iconShape,
                        iconScale = settings.iconScale,
                        showLabels = settings.showLabels,
                        labelLines = settings.labelLines,
                        badges = badges,
                        widgetContent = { widget, w, h ->
                            WidgetHostView(
                                appWidgetId = widget.widgetId,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .fillMaxSize(),
                                onMissing = { onMissingWidget(widget.widgetId) },
                            )
                        },
                        onLaunchApp = onLaunchApp,
                        onOpenFolder = { slot -> onOpenFolder(page, slot) },
                        onMove = { from, to -> onMove(page, from, to) },
                        onCreateFolder = { from, to -> onCreateFolder(page, from, to) },
                        onDropIntoFolder = { folderSlot, key ->
                            onDropIntoFolder(page, folderSlot, key)
                        },
                        onRemove = { slot -> onRemoveItem(page, slot) },
                        onLongPressEmpty = onLongPressEmpty,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                    )
                }

                PageIndicator(
                    pageCount = pageCount,
                    currentPage = pagerState.currentPage,
                    accent = accent,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp),
                )
            }

            if (settings.showDock) {
                DockRow(
                    items = layout.dock,
                    slots = settings.dockCount,
                    appsByKey = appsByKey,
                    pack = pack,
                    iconShape = settings.iconShape,
                    iconScale = settings.iconScale,
                    showLabels = false,
                    badges = badges,
                    onLaunchApp = onLaunchApp,
                    onLongPress = onDockLongPress,
                )
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}
