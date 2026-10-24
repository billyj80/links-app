package com.home.servicelinks.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.home.servicelinks.model.ServiceEntry
import com.home.servicelinks.ui.i18n.AppStrings
import com.home.servicelinks.ui.i18n.EnglishStrings
import com.home.servicelinks.ui.theme.*

@Composable
fun ServiceCard(
    service: ServiceEntry,
    compact: Boolean,
    colorLinks: Boolean,
    reorder: Boolean,
    strings: AppStrings = EnglishStrings,
    onOpen: (String) -> Unit,
    onEdit: () -> Unit,
    onMove: (Int) -> Unit
) {

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            // Header Row: Squircle icon + Title/Subtitle + Category/Actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Squircle Icon container matching screenshot
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconForService(service.icon, service.category),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(Modifier.width(14.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = service.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = service.description.ifBlank { strings.formatCategory(service.category) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = strings.formatCategory(service.category),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (reorder) {
                        Spacer(Modifier.width(6.dp))
                        Row {
                            IconButton(onClick = { onMove(-1) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.KeyboardArrowUp, strings.moveUp, tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { onMove(1) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.KeyboardArrowDown, strings.moveDown, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    } else {
                        Spacer(Modifier.width(4.dp))
                        IconButton(onClick = { onEdit() }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, strings.edit, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Network Links Row
            val hasLinks = service.tailscale.isNotBlank() || service.cloudflare.isNotBlank() || service.lan.isNotBlank() || service.customLinks.any { it.url.isNotBlank() }
            if (hasLinks) {
                Spacer(Modifier.height(14.dp))
                if (compact) {
                    // Compact View: Horizontal row of pill badges matching screenshot
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (service.tailscale.isNotBlank()) {
                            LinkPill(
                                label = "Tailscale",
                                icon = Icons.Outlined.Shield,
                                bgColor = if (colorLinks) LinkTail else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (colorLinks) PillText else MaterialTheme.colorScheme.onSurface,
                                onClick = { onOpen(service.tailscale) }
                            )
                        }
                        if (service.cloudflare.isNotBlank()) {
                            LinkPill(
                                label = "Cloudflare",
                                icon = Icons.Outlined.Cloud,
                                bgColor = if (colorLinks) LinkCloud else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (colorLinks) PillText else MaterialTheme.colorScheme.onSurface,
                                onClick = { onOpen(service.cloudflare) }
                            )
                        }
                        if (service.lan.isNotBlank()) {
                            LinkPill(
                                label = "LAN",
                                icon = Icons.Default.Wifi,
                                bgColor = if (colorLinks) LinkLan else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (colorLinks) PillText else MaterialTheme.colorScheme.onSurface,
                                onClick = { onOpen(service.lan) }
                            )
                        }
                        service.customLinks.forEach { custom ->
                            if (custom.url.isNotBlank()) {
                                LinkPill(
                                    label = custom.label.ifBlank { "Link" },
                                    icon = Icons.Default.Language,
                                    bgColor = if (colorLinks) Sky400 else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (colorLinks) PillText else MaterialTheme.colorScheme.onSurface,
                                    onClick = { onOpen(custom.url) }
                                )
                            }
                        }
                    }
                } else {
                    // Expanded View: Full width rows with URLs
                    val links = listOf(
                        Triple("Tailscale", service.tailscale, LinkTail),
                        Triple("Cloudflare", service.cloudflare, LinkCloud),
                        Triple("LAN", service.lan, LinkLan)
                    ) + service.customLinks.map { Triple(it.label, it.url, Sky400) }

                    links.filter { it.second.isNotBlank() }.forEach { (label, url, tint) ->
                        Spacer(Modifier.height(8.dp))
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    if (colorLinks) tint.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onOpen(url) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.OpenInNew,
                                null,
                                tint = if (colorLinks) tint else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(label, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Text(url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

fun iconForService(icon: String, category: String): ImageVector {
    if (icon.isNotBlank()) {
        // First: match directly against PresetAppIcons names (exact same mapping as the icon picker grid)
        val preset = PresetAppIcons.firstOrNull { it.first.equals(icon.trim(), ignoreCase = true) }
        if (preset != null) return preset.second

        // Fallback: loose keyword matching for legacy/free-text icon values
        when (icon.trim().lowercase()) {
            "server", "dns", "nas"                    -> return Icons.Default.Dns
            "globe", "web", "browser", "internet"     -> return Icons.Default.Language
            "media", "playcircle"                     -> return Icons.Default.PlayCircle
            "music", "audio", "sound"                 -> return Icons.Default.MusicNote
            "shield", "security", "guard"             -> return Icons.Default.Shield
            "key", "vpn", "lock"                      -> return Icons.Default.VpnKey
            "file", "document", "doc"                 -> return Icons.Default.Description
            "terminal", "code", "dev"                 -> return Icons.Default.Code
            "storage", "folder"                       -> return Icons.Default.Folder
            "bookmark"                                -> return Icons.Default.Bookmark
            "chat", "communication", "message"        -> return Icons.Default.Chat
            "home"                                    -> return Icons.Default.Home
            "cloud"                                   -> return Icons.Default.Cloud
            "gallery", "photo", "photos", "image"     -> return Icons.Default.PhotoLibrary
            "notes", "note"                           -> return Icons.Default.Notes
            "book", "books", "read"                   -> return Icons.Default.Book
            "manager", "admin", "task"                -> return Icons.Default.TaskAlt
            "video", "movie", "film"                  -> return Icons.Default.Movie
            "share", "filesharing", "shared"          -> return Icons.Default.Share
            "sync", "synchronize"                     -> return Icons.Default.Sync
            "monitor", "heart", "monitoring"          -> return Icons.Default.MonitorHeart
            "database", "db", "sql"                   -> return Icons.Default.Storage
            "memory", "cpu", "automation"             -> return Icons.Default.Memory
        }
    }
    return iconForCategory(category)
}

fun iconForCategory(c: String): ImageVector = when (c) {
    "Media" -> Icons.Default.PlayCircle
    "Storage" -> Icons.Default.Storage
    "Monitoring" -> Icons.Default.MonitorHeart
    "Development" -> Icons.Default.Code
    "Network" -> Icons.Default.Wifi
    "Security" -> Icons.Default.Shield
    "Automation" -> Icons.Default.Memory
    "Communication" -> Icons.Default.Chat
    "Database" -> Icons.Default.Storage
    "Server" -> Icons.Default.Dns
    else -> Icons.Default.Dns
}
