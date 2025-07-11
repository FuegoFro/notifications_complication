package com.fuegofro.notifications_complication.data

import kotlinx.serialization.Serializable

@Serializable
data class PackageSettings(
    val enabled: Boolean = false,
    val showOngoing: Boolean = false,
    val showSilent: Boolean = false
) {
    companion object {
        val DEFAULT = PackageSettings()
    }
}