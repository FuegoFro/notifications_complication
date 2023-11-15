@file:OptIn(ExperimentalMaterial3Api::class)

package com.fuegofro.notifications_complication.phone.ui

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fuegofro.notifications_complication.phone.R
import com.fuegofro.notifications_complication.phone.enabledPackagesDataStore
import com.fuegofro.notifications_complication.phone.ui.components.AppBarAction
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.launch

@Composable
fun PackageSelectionScreen() {
    val coroutineScope = rememberCoroutineScope()

    val packageManager = LocalContext.current.packageManager
    val enabledPackagesDataStore = LocalContext.current.enabledPackagesDataStore

    val (installedPackages, setInstalledPackages) =
        remember {
            mutableStateOf<List<PackageInfo>?>(
                null,
            )
        }
    LaunchedEffect(Unit) {
        @SuppressLint("QueryPermissionsNeeded")
        val packages =
            if (Build.VERSION.SDK_INT >= 33) {
                packageManager.getInstalledApplications(
                    PackageManager.ApplicationInfoFlags.of(
                        PackageManager.GET_META_DATA.toLong(),
                    ),
                )
            } else {
                packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            }
        setInstalledPackages(
            packages
                .asSequence()
                .filter { it.enabled && it.flags.and(ApplicationInfo.FLAG_SYSTEM) == 0 }
                .map {
                    PackageInfo(
                        name = it.packageName,
                        icon = it.loadIcon(packageManager),
                        label = it.loadLabel(packageManager).toString()
                    )
                }
                .sortedBy { it.label.lowercase() }
                .toList()
        )
    }

    @Suppress("SimpleRedundantLet")
    val enabledPackages =
        enabledPackagesDataStore.data.collectAsStateWithLifecycle(initialValue = null).value?.let {
            preferences ->
            preferences.asMap().keys.map { it.name }.toSet()
        }

    val setPackageEnabled: suspend (String, Boolean) -> Unit =
        { packageId: String, enabled: Boolean ->
            val key = booleanPreferencesKey(packageId)
            enabledPackagesDataStore.edit {
                if (enabled) {
                    it[key] = true
                } else {
                    it.remove(key)
                }
            }
        }

    var filterOnlyEnabled by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                title = { Text("Pick apps") },
                actions = {
                    if (filterOnlyEnabled) {
                        AppBarAction(
                            icon = Icons.Filled.FilterListOff,
                            labelRes = R.string.package_list_filter_all
                        ) {
                            filterOnlyEnabled = false
                        }
                    } else {
                        AppBarAction(
                            icon = Icons.Filled.FilterList,
                            labelRes = R.string.package_list_filter_only_enabled
                        ) {
                            filterOnlyEnabled = true
                        }
                    }
                    // Only want to show "enable all" when we have a non-null, empty set
                    if (enabledPackages?.isEmpty() == true) {
                        AppBarAction(
                            icon = Icons.Filled.ToggleOn,
                            labelRes = R.string.package_list_select_all
                        ) {
                            coroutineScope.launch {
                                enabledPackagesDataStore.edit { dataStore ->
                                    installedPackages?.forEach { packageInfo ->
                                        dataStore[booleanPreferencesKey(packageInfo.name)] = true
                                    }
                                }
                            }
                        }
                    } else {
                        AppBarAction(
                            icon = Icons.Filled.ToggleOff,
                            labelRes = R.string.package_list_deselect_all
                        ) {
                            coroutineScope.launch {
                                enabledPackagesDataStore.edit { dataStore -> dataStore.clear() }
                            }
                        }
                    }
                },
            )
        }
    ) { paddingValues ->
        PackageList(
            paddingValues,
            installedPackages,
            enabledPackages,
            setPackageEnabled,
            filterOnlyEnabled
        )
    }
}

private data class PackageInfo(val name: String, val icon: Drawable, val label: String)

@Composable
private fun PackageList(
    paddingValues: PaddingValues,
    installedPackages: List<PackageInfo>?,
    enabledPackages: Set<String>?,
    setPackageEnabled: suspend (String, Boolean) -> Unit,
    filterOnlyEnabled: Boolean
) {
    val coroutineScope = rememberCoroutineScope()

    if (installedPackages == null || enabledPackages == null) {
        Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
            Text(text = "Loading...")
        }
    } else {
        // TODO - This is *way* more performant/smooth than the LazyColumn 🤔
        // Column(
        //     modifier =
        //         Modifier.verticalScroll(rememberScrollState())
        //             .padding(paddingValues)
        //             .padding(vertical = 4.dp),
        // ) {
        //     installedPackages.forEach { packageInfo ->
        //         val enabled = enabledPackages.contains(packageInfo.name)
        //         if (!filterOnlyEnabled || enabled) {
        //             PackageRow(
        //                 modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        //                 packageInfo = packageInfo,
        //                 enabled = enabled,
        //                 setEnabled = { newEnabled: Boolean ->
        //                     coroutineScope.launch {
        //                         setPackageEnabled(packageInfo.name, newEnabled)
        //                     }
        //                 },
        //             )
        //         }
        //     }
        // }

        LazyColumn(
            /*modifier = Modifier.padding(vertical = 8.dp),*/ contentPadding = paddingValues
        ) {
            item { Spacer(modifier = Modifier.size(4.dp)) }

            items(installedPackages, key = { installedPackage -> installedPackage.name }) {
                packageInfo ->
                val enabled = enabledPackages.contains(packageInfo.name)
                if (!filterOnlyEnabled || enabled) {
                    PackageRow(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        packageInfo = packageInfo,
                        enabled = enabled,
                        setEnabled = { newEnabled: Boolean ->
                            coroutineScope.launch {
                                setPackageEnabled(packageInfo.name, newEnabled)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PackageRow(
    packageInfo: PackageInfo,
    enabled: Boolean,
    setEnabled: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(Modifier.clickable { setEnabled(!enabled) }.then(modifier)) {
        Image(
            modifier = Modifier.size(48.dp).align(Alignment.CenterVertically),
            painter = rememberDrawablePainter(packageInfo.icon),
            contentDescription = null,
        )
        Text(
            modifier = Modifier.weight(1f).align(Alignment.CenterVertically).padding(start = 8.dp),
            text = packageInfo.label,
        )
        Switch(checked = enabled, onCheckedChange = setEnabled)
    }
}
