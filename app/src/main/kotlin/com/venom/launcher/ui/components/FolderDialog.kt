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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.venom.launcher.data.AppInfo
import com.venom.launcher.data.FolderItem
import com.venom.launcher.data.IconPack
import com.venom.launcher.data.IconShape

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FolderDialog(
    folder: FolderItem,
    appsByKey: Map<String, AppInfo>,
    pack: IconPack?,
    iconShape: IconShape,
    badges: Map<String, Int>,
    onRename: (String) -> Unit,
    onLaunch: (AppInfo) -> Unit,
    onRemoveFromFolder: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var editingName by remember(folder.id) { mutableStateOf(false) }
    var nameDraft by remember(folder.id) { mutableStateOf(folder.label) }
    val accent = MaterialTheme.colorScheme.primary

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xF213181C), RoundedCornerShape(28.dp))
                    .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(28.dp))
                    .padding(18.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    if (editingName) {
                        OutlinedTextField(
                            value = nameDraft,
                            onValueChange = { nameDraft = it },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = {
                            onRename(nameDraft.trim().ifBlank { "Folder" })
                            editingName = false
                        }) {
                            Text("Save", color = accent)
                        }
                    } else {
                        Text(
                            text = folder.label.ifBlank { "Folder" },
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            modifier = Modifier
                                .weight(1f)
                                .combinedClickable(
                                    onClick = { editingName = true },
                                    onLongClick = { editingName = true },
                                ),
                        )
                        TextButton(onClick = { editingName = true }) {
                            Text("Rename", color = accent)
                        }
                    }
                }

                if (folder.items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 26.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Empty folder",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.4f),
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .heightIn(max = 320.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(items = folder.items, key = { it }) { key ->
                            val app = appsByKey[key]
                            if (app != null) {
                                Column(
                                    modifier = Modifier
                                        .combinedClickable(
                                            onClick = { onLaunch(app) },
                                            onLongClick = { onRemoveFromFolder(key) },
                                        ),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    AppIcon(
                                        app = app,
                                        pack = pack,
                                        shape = iconShape,
                                        size = 48.dp,
                                        badgeCount = badges[app.packageName] ?: 0,
                                    )
                                    Text(
                                        text = app.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.75f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = "long-press an app to pull it out",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}
