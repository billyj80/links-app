package com.home.servicelinks.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import com.home.servicelinks.model.AppShortcut
import com.home.servicelinks.ui.i18n.AppStrings
import com.home.servicelinks.ui.i18n.EnglishStrings
import com.home.servicelinks.ui.theme.LinksTheme
import java.io.File
import java.io.FileOutputStream

val PresetAppIcons = listOf(
    "Server" to Icons.Default.Dns,
    "Globe" to Icons.Default.Language,
    "Media" to Icons.Default.PlayCircle,
    "Music" to Icons.Default.MusicNote,
    "Security" to Icons.Default.Shield,
    "Key" to Icons.Default.VpnKey,
    "File" to Icons.Default.Description,
    "Terminal" to Icons.Default.Code,
    "Storage" to Icons.Default.Folder,
    "Bookmark" to Icons.Default.Bookmark,
    "Chat" to Icons.Default.Chat,
    "Home" to Icons.Default.Home,
    "Cloud" to Icons.Default.Cloud,
    "Gallery" to Icons.Default.PhotoLibrary,
    "Notes" to Icons.Default.Notes,
    "Book" to Icons.Default.Book,
    "Manager" to Icons.Default.TaskAlt,
    "Video" to Icons.Default.Movie,
    "Share" to Icons.Default.Share,
    "Sync" to Icons.Default.Sync
)

fun vectorForAppIconName(name: String?): ImageVector? {
    if (name == null) return null
    return PresetAppIcons.firstOrNull { it.first.equals(name, ignoreCase = true) }?.second
        ?: when (name.lowercase()) {
            "cloud" -> Icons.Default.Cloud
            "gallery", "photos", "photo" -> Icons.Default.PhotoLibrary
            "note", "notes" -> Icons.Default.Notes
            "book", "books" -> Icons.Default.Book
            "manager", "admin" -> Icons.Default.TaskAlt
            "video", "movie", "film" -> Icons.Default.Movie
            "share", "filesharing", "shared" -> Icons.Default.Share
            "sync", "synchronize" -> Icons.Default.Sync
            else -> null
        }
}

@Composable
fun AppCustomizerDialog(
    shortcut: AppShortcut,
    darkMode: String = "Dark",
    strings: AppStrings = EnglishStrings,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onSave: (AppShortcut) -> Unit
) {
    val context = LocalContext.current
    var label by remember { mutableStateOf(shortcut.label) }
    var customIconUri by remember { mutableStateOf(shortcut.customIconUri) }
    var selectedPresetName by remember { mutableStateOf(shortcut.customIconName) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val savedPath = saveImageToInternalStorage(context, uri, shortcut.packageName)
            if (savedPath != null) {
                customIconUri = savedPath
                selectedPresetName = null
            }
        }
    }

    LinksTheme(darkMode = darkMode) {
        val fieldColors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
        )

        AlertDialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    strings.customizeAppTitle,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 560.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Icon preview inline with Display name field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = label,
                            onValueChange = { label = it },
                            label = { Text(strings.displayNameLabel) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = fieldColors,
                            modifier = Modifier.weight(1f)
                        )
                        // Live Icon Preview
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            if (customIconUri != null) {
                                val bitmap = remember(customIconUri) {
                                    BitmapFactory.decodeFile(customIconUri)
                                }
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.size(44.dp)
                                    )
                                } else {
                                    Icon(Icons.Default.Android, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                }
                            } else if (selectedPresetName != null) {
                                val vector = vectorForAppIconName(selectedPresetName)
                                if (vector != null) {
                                    Icon(vector, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                } else {
                                    Icon(Icons.Default.Android, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                }
                            } else {
                                val pm = context.packageManager
                                val drawable = remember { runCatching { pm.getApplicationIcon(shortcut.packageName) }.getOrNull() }
                                if (drawable != null) {
                                    AndroidView(
                                        factory = { android.widget.ImageView(it).apply { setImageDrawable(drawable) } },
                                        modifier = Modifier.size(44.dp)
                                    )
                                } else {
                                    Icon(Icons.Default.Android, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                }
                            }
                        }
                    }

                    Text(
                        strings.iconOptionsTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { imagePicker.launch("image/*") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Icon(Icons.Default.Image, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.galleryButton)
                        }

                        OutlinedButton(
                            onClick = {
                                customIconUri = null
                                selectedPresetName = null
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Icon(Icons.Default.Restore, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.defaultButton)
                        }
                    }

                    Text(
                        strings.chooseFromPresets,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Grid of preset icons
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PresetAppIcons.chunked(5).forEach { rowIcons ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowIcons.forEach { (pName, iconVector) ->
                                    val isSelected = selectedPresetName == pName && customIconUri == null
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .clickable {
                                                selectedPresetName = pName
                                                customIconUri = null
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = iconVector,
                                            contentDescription = pName,
                                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                repeat(5 - rowIcons.size) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onDelete != null) {
                        TextButton(
                            onClick = { onDelete(); onDismiss() },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(strings.delete)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) {
                        Text(strings.cancel)
                    }
                    Spacer(Modifier.width(4.dp))
                    Button(
                        onClick = {
                            onSave(
                                shortcut.copy(
                                    label = label.trim().ifBlank { shortcut.label },
                                    customIconUri = customIconUri,
                                    customIconName = selectedPresetName
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(strings.save)
                    }
                }
            },
            dismissButton = {}
        )
    }
}

private fun saveImageToInternalStorage(context: Context, uri: Uri, packageName: String): String? {
    return runCatching {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        val iconsDir = File(context.filesDir, "custom_app_icons")
        if (!iconsDir.exists()) iconsDir.mkdirs()

        val safeName = packageName.replace(".", "_") + "_" + System.currentTimeMillis() + ".png"
        val destFile = File(iconsDir, safeName)

        val out = FileOutputStream(destFile)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        out.flush()
        out.close()

        destFile.absolutePath
    }.getOrNull()
}
