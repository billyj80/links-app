package com.home.servicelinks.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.home.servicelinks.model.CustomLink
import com.home.servicelinks.model.ServiceEntry
import com.home.servicelinks.ui.i18n.AppStrings
import com.home.servicelinks.ui.i18n.EnglishStrings
import com.home.servicelinks.ui.theme.LinksTheme
import java.util.UUID

val AvailableCategories = listOf(
    "Media", "Server", "Storage", "Monitoring", "Development",
    "Network", "Security", "Automation", "Communication", "Database", "Other"
)

@Composable
fun ServiceEditorDialog(
    existing: ServiceEntry?,
    darkMode: String = "Dark",
    strings: AppStrings = EnglishStrings,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onSave: (ServiceEntry) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var desc by remember { mutableStateOf(existing?.description ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: "Other") }
    var tailscale by remember { mutableStateOf(existing?.tailscale ?: "") }
    var cloudflare by remember { mutableStateOf(existing?.cloudflare ?: "") }
    var lan by remember { mutableStateOf(existing?.lan ?: "") }
    var selectedIconName by remember { mutableStateOf(existing?.icon ?: "") }

    // Multi-link support using idiomatic mutableStateListOf
    val customLinks = remember {
        mutableStateListOf<CustomLink>().apply {
            addAll(existing?.customLinks ?: emptyList())
        }
    }

    var catMenuOpen by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }

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
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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
                    text = if (existing == null) strings.addServiceTitle else strings.editServiceTitle,
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; if (it.isNotBlank()) nameError = false },
                        label = { Text(strings.serviceNameLabel) },
                        singleLine = true,
                        isError = nameError,
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (nameError) {
                        Text(
                            text = strings.serviceNameRequired,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text(strings.descriptionLabel) },
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Box {
                        OutlinedButton(
                            onClick = { catMenuOpen = true },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${strings.categoryLabel}: ${strings.formatCategory(category)}")
                        }
                        DropdownMenu(
                            expanded = catMenuOpen,
                            onDismissRequest = { catMenuOpen = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            AvailableCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(strings.formatCategory(cat), color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = { category = cat; catMenuOpen = false }
                                )
                            }
                        }
                    }

                    // Icon selection section
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    Text(
                        strings.iconOptionsTitle,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        strings.chooseFromPresets,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Grid of service preset icons (same list as AppCustomizerDialog)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PresetAppIcons.chunked(5).forEach { rowIcons ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowIcons.forEach { (pName, iconVector) ->
                                    val isSelected = selectedIconName.equals(pName, ignoreCase = true)
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
                                                selectedIconName = if (isSelected) "" else pName
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

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    Text(
                        strings.connectionEndpoints,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = tailscale,
                        onValueChange = { tailscale = it },
                        label = { Text(strings.tailscaleLabel) },
                        placeholder = { Text("e.g. http://my-host:8080") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = cloudflare,
                        onValueChange = { cloudflare = it },
                        label = { Text(strings.cloudflareLabel) },
                        placeholder = { Text("e.g. https://app.mydomain.com") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = lan,
                        onValueChange = { lan = it },
                        label = { Text(strings.lanLabel) },
                        placeholder = { Text("e.g. http://192.168.1.100:8080") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            strings.customLinksTitle,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(
                            onClick = {
                                customLinks.add(CustomLink("", ""))
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.addCustomLink)
                        }
                    }

                    customLinks.forEachIndexed { index, link ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = link.label,
                                onValueChange = { newLabel ->
                                    customLinks[index] = link.copy(label = newLabel)
                                },
                                label = { Text(strings.customLinkLabel) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = fieldColors,
                                modifier = Modifier.weight(0.4f)
                            )
                            OutlinedTextField(
                                value = link.url,
                                onValueChange = { newUrl ->
                                    customLinks[index] = link.copy(url = newUrl)
                                },
                                label = { Text(strings.customLinkUrl) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = fieldColors,
                                modifier = Modifier.weight(0.5f)
                            )
                            IconButton(onClick = {
                                customLinks.removeAt(index)
                            }) {
                                Icon(Icons.Default.Delete, "Remove link", tint = MaterialTheme.colorScheme.error)
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
                    if (existing != null && onDelete != null) {
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
                            if (name.isBlank()) {
                                nameError = true
                            } else {
                                val cleanedCustom = customLinks
                                    .filter { it.url.isNotBlank() }
                                    .map { it.copy(label = it.label.trim(), url = it.url.trim()) }

                                onSave(
                                    ServiceEntry(
                                        id = existing?.id ?: UUID.randomUUID().toString(),
                                        name = name.trim(),
                                        description = desc.trim(),
                                        category = category,
                                        icon = selectedIconName.trim(),
                                        lan = lan.trim(),
                                        tailscale = tailscale.trim(),
                                        cloudflare = cloudflare.trim(),
                                        customLinks = cleanedCustom,
                                        created = existing?.created ?: System.currentTimeMillis()
                                    )
                                )
                            }
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
