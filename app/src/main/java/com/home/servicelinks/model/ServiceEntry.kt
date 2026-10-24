package com.home.servicelinks.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class CustomLink(
    val label: String = "",
    val url: String = ""
)

@Serializable
data class ServiceEntry(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val category: String = "Other",
    val icon: String = "",
    val lan: String = "",
    val tailscale: String = "",
    val cloudflare: String = "",
    val customLinks: List<CustomLink> = emptyList(),
    val created: Long = System.currentTimeMillis()
)
