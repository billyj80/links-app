package com.home.servicelinks

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import com.home.servicelinks.data.LinksRepository
import com.home.servicelinks.model.AppShortcut
import com.home.servicelinks.model.ServiceEntry
import com.home.servicelinks.ui.components.*
import com.home.servicelinks.ui.i18n.getAppStrings
import com.home.servicelinks.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent { LinksApp() }
    }
}

@Composable
fun LinksApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val prefs = remember { context.getSharedPreferences("links_app", 0) }

    var language by remember { mutableStateOf(prefs.getString("language", "en") ?: "en") }
    val strings = remember(language) { getAppStrings(language) }

    var services by remember { mutableStateOf(LinksRepository.loadServices(prefs)) }
    var shortcuts by remember { mutableStateOf(LinksRepository.loadShortcuts(prefs)) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    var sort by remember { mutableStateOf("Custom order") }
    var searchAppsQuery by remember { mutableStateOf("") }
    var tab by remember { mutableIntStateOf(0) }
    var editingService by remember { mutableStateOf<ServiceEntry?>(null) }
    var editorOpen by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<ServiceEntry?>(null) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    var darkMode by remember { mutableStateOf(prefs.getString("theme", "Dark") ?: "Dark") }
    var compact by remember { mutableStateOf(prefs.getBoolean("compact", true)) }
    var colorLinks by remember { mutableStateOf(prefs.getBoolean("colorLinks", true)) }

    // Reorder state for Services and Apps tabs
    var reorderServices by remember { mutableStateOf(false) }
    var reorderApps by remember { mutableStateOf(false) }

    // App customization dialog state
    var editingShortcut by remember { mutableStateOf<AppShortcut?>(null) }
    var appPickerOpen by remember { mutableStateOf(false) }

    // Background loaded installed app list
    var installedApps by remember { mutableStateOf<List<AppShortcut>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(false) }

    fun loadInstalledAppsAsync() {
        if (installedApps.isNotEmpty() || isLoadingApps) return
        isLoadingApps = true
        scope.launch(Dispatchers.IO) {
            val pm = context.packageManager
            val list = runCatching {
                pm.getInstalledApplications(PackageManager.GET_META_DATA)
                    .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
                    .map { AppShortcut(it.packageName, pm.getApplicationLabel(it).toString()) }
                    .sortedBy { it.label.lowercase() }
            }.getOrDefault(emptyList())
            withContext(Dispatchers.Main) {
                installedApps = list
                isLoadingApps = false
            }
        }
    }

    LaunchedEffect(tab) {
        if (tab == 1) {
            loadInstalledAppsAsync()
        }
    }

    fun saveServices(v: List<ServiceEntry>) {
        services = v
        LinksRepository.saveServices(prefs, v)
    }

    fun saveShortcuts(v: List<AppShortcut>) {
        shortcuts = v
        LinksRepository.saveShortcuts(prefs, v)
    }

    fun openUrl(raw: String) {
        val u = if (raw.startsWith("http://") || raw.startsWith("https://")) raw else "http://$raw"
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)))
        }.onFailure {
            scope.launch { snackbarHostState.showSnackbar(strings.unableToOpenUrl + raw) }
        }
    }

    val filteredServices = services.filter {
        (category == "All" || it.category == category) &&
        (query.isBlank() || listOf(it.name, it.description, it.category).any { v -> v.contains(query, true) })
    }.let { l ->
        when (sort) {
            "Name A–Z" -> l.sortedBy { it.name.lowercase() }
            "Name Z–A" -> l.sortedByDescending { it.name.lowercase() }
            "Category" -> l.sortedWith(compareBy<ServiceEntry> { strings.formatCategory(it.category) }.thenBy { it.name })
            "Newest" -> l.sortedByDescending { it.created }
            "Oldest" -> l.sortedBy { it.created }
            else -> l
        }
    }

    val isDark = darkMode == "Dark" || (darkMode == "System" && isSystemInDarkTheme())

    // Whole app and all dialogs wrapped in LinksTheme so dark mode is fully inherited
    LinksTheme(darkMode = darkMode) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 8.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = when (tab) { 0 -> strings.tabLinks; 1 -> strings.tabApps; else -> strings.tabSettings },
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = when (tab) {
                                    0 -> strings.headerSubtitleLinks
                                    1 -> strings.headerSubtitleApps
                                    else -> strings.headerSubtitleSettings
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Top bar actions
                        if (tab == 0) {
                            FilledIconButton(
                                onClick = { editingService = null; editorOpen = true },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(Icons.Default.Add, strings.addService)
                            }
                        }

                        if (tab == 1) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { reorderApps = !reorderApps }
                                ) {
                                    Icon(
                                        imageVector = if (reorderApps) Icons.Default.Done else Icons.Default.SwapVert,
                                        contentDescription = if (reorderApps) strings.done else strings.reorderApps,
                                        tint = if (reorderApps) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                FilledIconButton(
                                    onClick = {
                                        loadInstalledAppsAsync()
                                        appPickerOpen = true
                                    },
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Icon(Icons.Default.Add, strings.addAppShortcut)
                                }
                            }
                        }
                    }

                    if (tab == 1) {
                        Spacer(Modifier.height(14.dp))
                        OutlinedTextField(
                            value = searchAppsQuery,
                            onValueChange = { searchAppsQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(strings.searchServicesPlaceholder) },
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )
                    }

                    if (tab == 0) {
                        Spacer(Modifier.height(14.dp))
                        // Restored original clean Search services bar style without extra tinting
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(strings.searchServicesPlaceholder) },
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box {
                                val currentSortLabel = when (sort) {
                                    "Name A–Z" -> strings.sortNameAZ
                                    "Name Z–A" -> strings.sortNameZA
                                    "Category" -> strings.sortCategory
                                    "Newest" -> strings.sortNewest
                                    "Oldest" -> strings.sortOldest
                                    else -> strings.sortCustomOrder
                                }
                                OutlinedButton(
                                    onClick = { sortMenuOpen = true },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                                ) {
                                    Icon(Icons.Default.Tune, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(currentSortLabel)
                                    Icon(Icons.Default.ArrowDropDown, null)
                                }
                                DropdownMenu(
                                    expanded = sortMenuOpen,
                                    onDismissRequest = { sortMenuOpen = false },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                ) {
                                    listOf(
                                        "Custom order" to strings.sortCustomOrder,
                                        "Name A–Z" to strings.sortNameAZ,
                                        "Name Z–A" to strings.sortNameZA,
                                        "Category" to strings.sortCategory,
                                        "Newest" to strings.sortNewest,
                                        "Oldest" to strings.sortOldest
                                    ).forEach { (key, label) ->
                                        DropdownMenuItem(
                                            text = { Text(label, color = MaterialTheme.colorScheme.onSurface) },
                                            onClick = { sort = key; sortMenuOpen = false }
                                        )
                                    }
                                }
                            }

                            // Minimal reorder button for services (icon only)
                            IconButton(
                                onClick = {
                                    reorderServices = !reorderServices
                                    if (reorderServices) {
                                        sort = "Custom order"
                                        category = "All"
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (reorderServices) Icons.Default.Done else Icons.Default.SwapVert,
                                    contentDescription = if (reorderServices) strings.done else strings.reorder,
                                    tint = if (reorderServices) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        ) { padding ->
            // Root Box allows the list content to reach all the way to the bottom of the screen
            // while the FloatingBottomBar overlays directly above it with no solid background!
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding())
            ) {
                when (tab) {
                    0 -> Column(Modifier.fillMaxSize()) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            (listOf("All") + AvailableCategories.filter { c -> services.any { it.category == c } }).forEach {
                                FilterChip(
                                    selected = category == it,
                                    onClick = { category = it },
                                    label = { Text(strings.formatCategory(it)) }
                                )
                            }
                        }
                        if (filteredServices.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(strings.noServicesFound, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 120.dp),
                                verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filteredServices, key = { it.id }) { service ->
                                    ServiceCard(
                                        service = service,
                                        compact = compact,
                                        colorLinks = colorLinks,
                                        reorder = reorderServices,
                                        strings = strings,
                                        onOpen = { openUrl(it) },
                                        onEdit = { editingService = service; editorOpen = true },
                                        onMove = { delta ->
                                            val idx = services.indexOfFirst { it.id == service.id }
                                            val dest = (idx + delta).coerceIn(0, services.lastIndex)
                                            if (idx >= 0 && idx != dest) {
                                                val m = services.toMutableList()
                                                val item = m.removeAt(idx)
                                                m.add(dest, item)
                                                saveServices(m)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        val pm = context.packageManager
                        val filteredShortcuts = if (searchAppsQuery.isBlank()) shortcuts
                        else shortcuts.filter { sc ->
                            val app = installedApps.find { it.packageName == sc.packageName }
                            val label = sc.label.ifBlank { app?.label ?: sc.packageName }
                            label.contains(searchAppsQuery, ignoreCase = true) ||
                                sc.packageName.contains(searchAppsQuery, ignoreCase = true)
                        }
                        if (isLoadingApps && shortcuts.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        } else if (shortcuts.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Apps, null, Modifier.size(52.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.height(8.dp))
                                    Text(strings.noAppShortcuts, style = MaterialTheme.typography.titleLarge)
                                    Text(strings.tapToChooseApp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 120.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filteredShortcuts, key = { it.packageName }) { sc ->
                                    val app = installedApps.find { it.packageName == sc.packageName }
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                    ) {
                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    pm.getLaunchIntentForPackage(sc.packageName)?.let { context.startActivity(it) }
                                                }
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Custom Icon rendering
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (sc.customIconUri != null) {
                                                    val bmp = remember(sc.customIconUri) {
                                                        BitmapFactory.decodeFile(sc.customIconUri)
                                                    }
                                                    if (bmp != null) {
                                                        Image(
                                                            bitmap = bmp.asImageBitmap(),
                                                            contentDescription = null,
                                                            modifier = Modifier.size(38.dp)
                                                        )
                                                    } else {
                                                        Icon(Icons.Default.Android, null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                                                    }
                                                } else if (sc.customIconName != null) {
                                                    val vector = vectorForAppIconName(sc.customIconName)
                                                    if (vector != null) {
                                                        Icon(vector, null, modifier = Modifier.size(26.dp), tint = MaterialTheme.colorScheme.primary)
                                                    } else {
                                                        Icon(Icons.Default.Android, null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                                                    }
                                                } else {
                                                    val drawable: Drawable? = runCatching { pm.getApplicationIcon(sc.packageName) }.getOrNull()
                                                    if (drawable != null) {
                                                        AndroidView(
                                                            factory = { android.widget.ImageView(it).apply { setImageDrawable(drawable) } },
                                                            modifier = Modifier.size(38.dp)
                                                        )
                                                    } else {
                                                        Icon(Icons.Default.Android, null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                                                    }
                                                }
                                            }

                                            Spacer(Modifier.width(14.dp))

                                            Column(Modifier.weight(1f)) {
                                                Text(sc.label.ifBlank { app?.label ?: sc.packageName }, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                                Text(sc.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }

                                            if (reorderApps) {
                                                // Reorder buttons: Move up & Move down
                                                IconButton(onClick = {
                                                    val idx = shortcuts.indexOfFirst { it.packageName == sc.packageName }
                                                    if (idx > 0) {
                                                        val m = shortcuts.toMutableList()
                                                        val item = m.removeAt(idx)
                                                        m.add(idx - 1, item)
                                                        saveShortcuts(m)
                                                    }
                                                }, modifier = Modifier.size(34.dp)) {
                                                    Icon(Icons.Default.KeyboardArrowUp, strings.moveUp, tint = MaterialTheme.colorScheme.primary)
                                                }
                                                IconButton(onClick = {
                                                    val idx = shortcuts.indexOfFirst { it.packageName == sc.packageName }
                                                    if (idx in 0 until shortcuts.lastIndex) {
                                                        val m = shortcuts.toMutableList()
                                                        val item = m.removeAt(idx)
                                                        m.add(idx + 1, item)
                                                        saveShortcuts(m)
                                                    }
                                                }, modifier = Modifier.size(34.dp)) {
                                                    Icon(Icons.Default.KeyboardArrowDown, strings.moveDown, tint = MaterialTheme.colorScheme.primary)
                                                }
                                            } else {
                                                IconButton(onClick = { editingShortcut = sc }, modifier = Modifier.size(34.dp)) {
                                                    Icon(Icons.Default.Edit, strings.edit, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else -> Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Language Selection Card
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                Text(
                                    text = strings.languageSectionTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(4.dp))
                                val languageOptions = listOf(
                                    "en" to "English",
                                    "pl" to "Polski"
                                )
                                languageOptions.forEach { (key, label) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                language = key
                                                prefs.edit().putString("language", key).apply()
                                            }
                                            .padding(vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = language == key,
                                            onClick = {
                                                language = key
                                                prefs.edit().putString("language", key).apply()
                                            },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = MaterialTheme.colorScheme.primary
                                            ),
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // Appearance Card with compact spacing and updated descriptive labels
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                Text(
                                    text = strings.appearanceSectionTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(4.dp))
                                val themeOptions = listOf(
                                    "System" to strings.themeFollowSystem,
                                    "Light" to strings.themeLight,
                                    "Dark" to strings.themeDark
                                )
                                themeOptions.forEach { (key, label) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                darkMode = key
                                                prefs.edit().putString("theme", key).apply()
                                            }
                                            .padding(vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = darkMode == key,
                                            onClick = {
                                                darkMode = key
                                                prefs.edit().putString("theme", key).apply()
                                            },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = MaterialTheme.colorScheme.primary
                                            ),
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(strings.serviceListSectionTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                SettingSwitch(strings.compactCardsTitle, strings.compactCardsSubtitle, compact) {
                                    compact = it
                                    prefs.edit().putBoolean("compact", it).apply()
                                }
                                SettingSwitch(strings.colorLinksTitle, strings.colorLinksSubtitle, colorLinks) {
                                    colorLinks = it
                                    prefs.edit().putBoolean("colorLinks", it).apply()
                                }
                                if (colorLinks) {
                                    Spacer(Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        LinkLegend("Tailscale", LinkTail)
                                        LinkLegend("Cloudflare", LinkCloud)
                                        LinkLegend("LAN", LinkLan)
                                    }
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(strings.backupSectionTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(Modifier.height(4.dp))
                                Text(strings.backupSubtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(10.dp))
                                val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
                                    if (uri != null) runCatching {
                                        context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                                            it.write(LinksRepository.exportBackupJson(services, shortcuts))
                                        }
                                    }.onSuccess {
                                        scope.launch { snackbarHostState.showSnackbar(strings.backupExportSuccess) }
                                    }.onFailure {
                                        scope.launch { snackbarHostState.showSnackbar(strings.backupExportFailed + it.localizedMessage) }
                                    }
                                }
                                val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                                    if (uri != null) runCatching {
                                        val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: error("Could not read file")
                                        val backupResult = LinksRepository.importBackupJson(raw)
                                        if (backupResult.services.isNotEmpty()) {
                                            saveServices(backupResult.services)
                                        }
                                        if (backupResult.shortcuts.isNotEmpty()) {
                                            saveShortcuts(backupResult.shortcuts)
                                        }
                                        backupResult
                                    }.onSuccess { result ->
                                        val msg = if (result.shortcuts.isNotEmpty()) {
                                            strings.backupImportSuccessWithShortcuts.format(result.services.size, result.shortcuts.size)
                                        } else {
                                            strings.backupImportSuccess.format(result.services.size)
                                        }
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }.onFailure {
                                        scope.launch { snackbarHostState.showSnackbar(strings.backupImportFailed + it.localizedMessage) }
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { export.launch("Service-Links-Backup.json") },
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                                    ) {
                                        Icon(Icons.Default.FileDownload, null)
                                        Spacer(Modifier.width(6.dp))
                                        Text(strings.exportButton)
                                    }
                                    OutlinedButton(
                                        onClick = { import.launch(arrayOf("application/json", "text/plain", "*/*")) },
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                                    ) {
                                        Icon(Icons.Default.FileUpload, null)
                                        Spacer(Modifier.width(6.dp))
                                        Text(strings.importButton)
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(strings.backupFormatNote, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(strings.aboutSectionTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(Modifier.height(4.dp))
                                Text("${services.size} ${strings.servicesCountWord} · ${shortcuts.size} ${strings.appShortcutsCountWord}", color = MaterialTheme.colorScheme.onSurface)
                                Spacer(Modifier.height(6.dp))
                                Text(strings.aboutDescription, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = { openUrl("https://buymeacoffee.com/billyj") },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFFFFDD00)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color(0xFFFFDD00).copy(alpha = 0.10f),
                                        contentColor = Color(0xFFB8860B)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("☕  Buy me a coffee", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                // Floating bar overlays on top of the list with no solid background underneath
                FloatingBottomBar(
                    currentTab = tab,
                    onTabSelected = { tab = it },
                    isDark = isDark,
                    labelLinks = strings.tabLinks,
                    labelApps = strings.tabApps,
                    labelSettings = strings.tabSettings,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        // Dialogs placed directly inside LinksTheme so they inherit the active theme
        if (editorOpen) {
            ServiceEditorDialog(
                existing = editingService,
                darkMode = darkMode,
                strings = strings,
                onDismiss = { editorOpen = false },
                onDelete = { deleteTarget = editingService; editorOpen = false },
                onSave = { entry ->
                    saveServices(if (editingService == null) services + entry else services.map { if (it.id == entry.id) entry else it })
                    editorOpen = false
                }
            )
        }

        // App shortcut customizer dialog
        editingShortcut?.let { sc ->
            AppCustomizerDialog(
                shortcut = sc,
                darkMode = darkMode,
                strings = strings,
                onDismiss = { editingShortcut = null },
                onDelete = { saveShortcuts(shortcuts.filterNot { it.packageName == sc.packageName }); editingShortcut = null },
                onSave = { updated ->
                    saveShortcuts(shortcuts.map { if (it.packageName == updated.packageName) updated else it })
                    editingShortcut = null
                }
            )
        }

        deleteTarget?.let { target ->
            AlertDialog(
                onDismissRequest = { deleteTarget = null },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(24.dp),
                title = { Text(strings.deleteServiceConfirmTitle.format(target.name), color = MaterialTheme.colorScheme.onSurface) },
                text = { Text(strings.deleteServiceConfirmText, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                confirmButton = {
                    Button(
                        onClick = {
                            saveServices(services.filterNot { it.id == target.id })
                            deleteTarget = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) { Text(strings.delete) }
                },
                dismissButton = {
                    TextButton(
                        onClick = { deleteTarget = null },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) { Text(strings.cancel) }
                }
            )
        }

        if (appPickerOpen) {
            var appQuery by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { appPickerOpen = false },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(24.dp),
                title = { Text(strings.addAppShortcut, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                text = {
                    Column(Modifier.heightIn(max = 500.dp)) {
                        OutlinedTextField(
                            value = appQuery,
                            onValueChange = { appQuery = it },
                            placeholder = { Text(strings.searchInstalledApps) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        if (isLoadingApps) {
                            Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        } else {
                            LazyColumn {
                                items(
                                    installedApps.filter { it.label.contains(appQuery, true) || it.packageName.contains(appQuery, true) }.take(150),
                                    key = { it.packageName }
                                ) { app ->
                                    TextButton(
                                        onClick = {
                                            saveShortcuts((shortcuts.filterNot { it.packageName == app.packageName }) + app)
                                            appPickerOpen = false
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                                    ) {
                                        Text(app.label, Modifier.weight(1f), fontWeight = FontWeight.Medium)
                                        Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { appPickerOpen = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) { Text(strings.close) }
                }
            )
        }
    }
}

@Composable
private fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.secondaryContainer
            )
        )
    }
}

@Composable
private fun LinkLegend(label: String, color: Color) {
    Surface(
        color = color,
        shape = CircleShape
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            color = PillText,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
