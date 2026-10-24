package com.home.servicelinks.data

import android.content.SharedPreferences
import com.home.servicelinks.model.AppShortcut
import com.home.servicelinks.model.CustomLink
import com.home.servicelinks.model.ServiceEntry
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class BackupResult(
    val services: List<ServiceEntry>,
    val shortcuts: List<AppShortcut>
)

object LinksRepository {

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
        encodeDefaults = true
    }

    fun loadServices(prefs: SharedPreferences): List<ServiceEntry> {
        val raw = prefs.getString("services", null) ?: return defaultServices()
        return runCatching {
            json.decodeFromString<List<ServiceEntry>>(raw)
        }.recoverCatching {
            importServicesJson(raw)
        }.getOrElse { defaultServices() }
    }

    fun saveServices(prefs: SharedPreferences, list: List<ServiceEntry>) {
        val encoded = json.encodeToString(list)
        prefs.edit().putString("services", encoded).apply()
    }

    fun loadShortcuts(prefs: SharedPreferences): List<AppShortcut> {
        val raw = prefs.getString("shortcuts", null) ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<AppShortcut>>(raw)
        }.recoverCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                AppShortcut(
                    packageName = o.optString("package", o.optString("packageName")),
                    label = o.optString("label"),
                    customIconUri = o.optString("customIconUri").takeIf { s -> s.isNotBlank() },
                    customIconName = o.optString("customIconName").takeIf { s -> s.isNotBlank() }
                )
            }
        }.getOrDefault(emptyList())
    }

    fun saveShortcuts(prefs: SharedPreferences, list: List<AppShortcut>) {
        val encoded = json.encodeToString(list)
        prefs.edit().putString("shortcuts", encoded).apply()
    }

    fun exportBackupJson(services: List<ServiceEntry>, shortcuts: List<AppShortcut>): String {
        val root = JSONObject()
        root.put("version", 2)
        root.put("exportedAt", System.currentTimeMillis())

        val arr = JSONArray()
        services.forEach { s ->
            val o = JSONObject()
            o.put("id", s.id)
            o.put("name", s.name)
            o.put("description", s.description)
            o.put("category", s.category)
            o.put("icon", s.icon)
            o.put("createdAt", s.created)

            val linksObj = JSONObject()
            if (s.lan.isNotBlank()) linksObj.put("lan", s.lan)
            if (s.tailscale.isNotBlank()) linksObj.put("tailscale", s.tailscale)
            if (s.cloudflare.isNotBlank()) linksObj.put("cloudflare", s.cloudflare)
            o.put("links", linksObj)

            if (s.customLinks.isNotEmpty()) {
                val customArr = JSONArray()
                s.customLinks.forEach { c ->
                    customArr.put(JSONObject().put("label", c.label).put("url", c.url))
                }
                o.put("customLinks", customArr)
            }

            arr.put(o)
        }
        root.put("services", arr)

        val scArr = JSONArray()
        shortcuts.forEach { sc ->
            val scObj = JSONObject()
            scObj.put("packageName", sc.packageName)
            scObj.put("label", sc.label)
            if (!sc.customIconUri.isNullOrBlank()) {
                scObj.put("customIconUri", sc.customIconUri)
            }
            if (!sc.customIconName.isNullOrBlank()) {
                scObj.put("customIconName", sc.customIconName)
            }
            scArr.put(scObj)
        }
        root.put("shortcuts", scArr)

        return root.toString(2)
    }

    fun exportServicesJson(list: List<ServiceEntry>): String {
        return exportBackupJson(list, emptyList())
    }

    fun importBackupJson(raw: String): BackupResult {
        val services = importServicesJson(raw)
        val shortcuts = mutableListOf<AppShortcut>()
        val trimmed = raw.trim()
        if (trimmed.startsWith("{")) {
            runCatching {
                val rootObj = JSONObject(trimmed)
                val scArr = rootObj.optJSONArray("shortcuts") ?: rootObj.optJSONArray("appShortcuts")
                if (scArr != null) {
                    for (i in 0 until scArr.length()) {
                        val o = scArr.optJSONObject(i) ?: continue
                        val pkg = o.optString("packageName", o.optString("package", "")).trim()
                        if (pkg.isBlank()) continue
                        val label = o.optString("label", "").trim()
                        val customUri = o.optString("customIconUri").takeIf { it.isNotBlank() }
                        val customName = o.optString("customIconName").takeIf { it.isNotBlank() }
                        shortcuts.add(AppShortcut(pkg, label, customUri, customName))
                    }
                }
            }
        }
        return BackupResult(services, shortcuts)
    }

    /**
     * Imports services from JSON text.
     * Supports both:
     * 1) Root object with "services" array: { "version": 1, "services": [ ... ] } (e.g. Service Links Backup.txt)
     * 2) Direct JSON array: [ ... ]
     * 3) Direct or nested "links": { "lan": "...", "tailscale": "...", "cloudflare": "..." }
     */
    fun importServicesJson(raw: String): List<ServiceEntry> {
        val trimmed = raw.trim()
        val servicesArray: JSONArray = when {
            trimmed.startsWith("{") -> {
                val rootObj = JSONObject(trimmed)
                when {
                    rootObj.has("services") -> rootObj.getJSONArray("services")
                    rootObj.has("data") -> rootObj.getJSONArray("data")
                    else -> JSONArray().apply { put(rootObj) }
                }
            }
            trimmed.startsWith("[") -> JSONArray(trimmed)
            else -> error("Invalid JSON file: must start with { or [")
        }

        val result = mutableListOf<ServiceEntry>()
        for (i in 0 until servicesArray.length()) {
            val o = servicesArray.optJSONObject(i) ?: continue
            val name = o.optString("name", "").trim()
            if (name.isBlank()) continue

            val description = o.optString("description", "").trim()
            val category = o.optString("category", "Other").trim()
            val icon = o.optString("icon", "").trim()
            val id = o.optString("id", UUID.randomUUID().toString())
            val created = o.optLong("createdAt", o.optLong("created", System.currentTimeMillis()))

            var lan = ""
            var tailscale = ""
            var cloudflare = ""
            val customLinks = mutableListOf<CustomLink>()

            // Check nested "links" object first
            if (o.has("links") && o.optJSONObject("links") != null) {
                val linksObj = o.getJSONObject("links")
                lan = linksObj.optString("lan", "").trim()
                tailscale = linksObj.optString("tailscale", "").trim()
                cloudflare = linksObj.optString("cloudflare", "").trim()

                // Any other key in "links" is captured as a custom link
                val keys = linksObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    if (key !in listOf("lan", "tailscale", "cloudflare")) {
                        val url = linksObj.optString(key, "").trim()
                        if (url.isNotBlank()) {
                            val label = key.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                            customLinks.add(CustomLink(label, url))
                        }
                    }
                }
            } else {
                // Fall back to top-level URL fields
                lan = o.optString("lan", "").trim()
                tailscale = o.optString("tailscale", "").trim()
                cloudflare = o.optString("cloudflare", "").trim()
            }

            // Check "customLinks" array
            val cArr = o.optJSONArray("customLinks")
            if (cArr != null) {
                for (j in 0 until cArr.length()) {
                    val cObj = cArr.optJSONObject(j) ?: continue
                    val label = cObj.optString("label", cObj.optString("name", "")).trim()
                    val url = cObj.optString("url", "").trim()
                    if (url.isNotBlank()) {
                        customLinks.add(CustomLink(label.ifBlank { "Link" }, url))
                    }
                }
            }

            result.add(
                ServiceEntry(
                    id = id,
                    name = name,
                    description = description,
                    category = category,
                    icon = icon,
                    lan = lan,
                    tailscale = tailscale,
                    cloudflare = cloudflare,
                    customLinks = customLinks,
                    created = created
                )
            )
        }

        if (result.isEmpty()) error("No valid services found in JSON")
        return result
    }

    fun defaultServices(): List<ServiceEntry> = listOf(
        ServiceEntry(
            name = "Trigkey",
            description = "Server",
            category = "Server",
            icon = "server",
            tailscale = "http://trigkey:8080",
            cloudflare = "https://trigkey.example.com",
            lan = "http://192.168.0.120:8080"
        ),
        ServiceEntry(
            name = "Synology",
            description = "Server",
            category = "Server",
            icon = "server",
            tailscale = "http://synology:5000",
            cloudflare = "https://nas.example.com",
            lan = "http://192.168.0.110:5000"
        ),
        ServiceEntry(
            name = "DietPi",
            description = "Monitoring",
            category = "Monitoring",
            icon = "server",
            tailscale = "http://dietpi:3000",
            lan = "http://192.168.0.105:3000"
        )
    )
}
