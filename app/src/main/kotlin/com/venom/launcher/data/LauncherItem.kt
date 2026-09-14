package com.venom.launcher.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Anything that can sit in a cell of the home grid or the dock.
 *
 * Sealed + kotlinx.serialization so the whole home layout round-trips through
 * DataStore as a single JSON blob.
 */
@Serializable
sealed interface LauncherItem {
    val id: String
}

@Serializable
@SerialName("app")
data class AppItem(
    override val id: String,
    val key: String,
) : LauncherItem

@Serializable
@SerialName("widget")
data class WidgetItem(
    override val id: String,
    val widgetId: Int,
    /** Cells wide / tall. The grid renders one widget across its whole span. */
    val spanX: Int = 1,
    val spanY: Int = 1,
) : LauncherItem

@Serializable
@SerialName("folder")
data class FolderItem(
    override val id: String,
    val label: String = "Folder",
    val items: List<String> = emptyList(),
) : LauncherItem

/** The persisted shape of the entire home screen. */
@Serializable
data class HomeLayout(
    val pages: List<List<LauncherItem?>> = listOf(List(DEFAULT_CELLS) { null }),
    val dock: List<LauncherItem?> = emptyList(),
) {
    companion object {
        const val DEFAULT_CELLS = 20 // 4 columns x 5 rows
    }
}
