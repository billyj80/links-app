package com.home.servicelinks.model

import kotlinx.serialization.Serializable

@Serializable
data class AppShortcut(
    val packageName: String,
    val label: String,
    val customIconUri: String? = null,
    val customIconName: String? = null
)
