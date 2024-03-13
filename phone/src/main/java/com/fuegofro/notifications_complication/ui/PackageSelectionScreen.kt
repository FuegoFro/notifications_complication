@file:OptIn(ExperimentalMaterial3Api::class)

package com.fuegofro.notifications_complication.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fuegofro.notifications_complication.NotificationListener
import com.fuegofro.notifications_complication.R
import com.fuegofro.notifications_complication.data.EnabledPackagesDataStore.Companion.enabledPackagesDataStore
import com.fuegofro.notifications_complication.ui.components.AppBarAction
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PackageSelectionScreen(
    onNavigateUp: () -> Unit,
    notificationListenerBinderFlow: Flow<NotificationListener.NotificationListenerBinder?>,
) {
    val coroutineScope = rememberCoroutineScope()

    val packageManager = LocalContext.current.packageManager

    val (installedPackages, setInstalledPackages) =
        remember {
            mutableStateOf<List<PackageInfo>?>(
                null,
            )
        }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val startupIntent = Intent(Intent.ACTION_MAIN)
            startupIntent.addCategory(Intent.CATEGORY_LAUNCHER)

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

            val packagesWithLauncherActivities =
                packageManager
                    .queryIntentActivities(startupIntent, 0)
                    .asSequence()
                    .map { it.activityInfo.packageName }
                    .toSet()
            setInstalledPackages(
                packages
                    .asSequence()
                    .filter {
                        it.enabled && packagesWithLauncherActivities.contains(it.packageName)
                    }
                    .map {
                        PackageInfo(
                            name = it.packageName,
                            icon = it.loadIcon(packageManager),
                            label = it.loadLabel(packageManager).toString(),
                        )
                    }
                    .sortedBy { it.label.lowercase() }
                    .toList(),
            )
        }
    }

    val (disableAllDialogVisible, setDisableAllDialogVisible) = remember { mutableStateOf(false) }
    val enabledPackagesDataStore = LocalContext.current.enabledPackagesDataStore
    val enabledPackages =
        enabledPackagesDataStore.enabledPackages
            .collectAsStateWithLifecycle(initialValue = null)
            .value
    val binder =
        notificationListenerBinderFlow.collectAsStateWithLifecycle(initialValue = null).value
    suspend fun setPackageEnabled(packageId: String, enabled: Boolean) {
        enabledPackagesDataStore.setPackageEnabled(packageId, enabled)
        binder?.forceRefresh()
    }

    var filterOnlyEnabled by remember { mutableStateOf(false) }
    val (currentSearch, setCurrentSearch) = remember { mutableStateOf<String?>(null) }
    val hasSearch = currentSearch != null
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(hasSearch) {
        if (hasSearch) {
            focusRequester.requestFocus()
        }
    }

    if (currentSearch != null) {
        SearchBar(
            modifier =
                Modifier.focusRequester(focusRequester)
                    // The SearchBar is taller than the TopAppBar, plus we're hiding the divider by
                    // making it the same color as the search bar. In order to prevent the list from
                    // moving when you open/close search, we shift it up by consuming the top
                    // insets, 8dp for the difference in height and 1dp for the divider. It's...
                    // hacky and the resulting text field isn't vertically aligned, but I think it's
                    // better than the list moving. Short of implementing this all from scratch it's
                    // probably the best we've got.
                    .consumeWindowInsets(PaddingValues(top = 9.dp)),
            query = currentSearch,
            onQueryChange = setCurrentSearch,
            onSearch = {},
            active = true,
            onActiveChange = { newActive ->
                if (!newActive) {
                    setCurrentSearch(null)
                }
            },
            placeholder = { Text(stringResource(id = R.string.package_list_search)) },
            leadingIcon = {
                IconButton(onClick = { setCurrentSearch(null) }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(id = android.R.string.cancel)
                    )
                }
            },
            colors =
                SearchBarDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    dividerColor = MaterialTheme.colorScheme.primaryContainer,
                )
        ) {
            // Need to re-set the background color here, since the SearchBar's containerColor sets
            // the background for the search field *and* the results view.
            Surface(modifier = Modifier.fillMaxHeight()) {
                PackageList(
                    PaddingValues(),
                    installedPackages,
                    enabledPackages,
                    ::setPackageEnabled,
                    filterOnlyEnabled,
                    currentSearch,
                )
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                title = { Text(stringResource(R.string.package_list_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.button_up),
                        )
                    }
                },
                actions = {
                    // Only want to show "enable all" when we have a non-null, empty set
                    if (enabledPackages?.isEmpty() == true) {
                        AppBarAction(
                            icon = Icons.Filled.ToggleOn,
                            labelRes = R.string.package_list_select_all,
                        ) {
                            coroutineScope.launch {
                                installedPackages
                                    ?.asSequence()
                                    ?.map { it.name }
                                    ?.let { enabledPackagesDataStore.enableAll(it) }
                            }
                        }
                    } else {
                        AppBarAction(
                            icon = Icons.Filled.ToggleOff,
                            labelRes = R.string.package_list_deselect_all,
                        ) {
                            setDisableAllDialogVisible(true)
                        }
                    }
                    if (filterOnlyEnabled) {
                        AppBarAction(
                            icon = Icons.Filled.FilterListOff,
                            labelRes = R.string.package_list_filter_all,
                        ) {
                            filterOnlyEnabled = false
                        }
                    } else {
                        AppBarAction(
                            icon = Icons.Filled.FilterList,
                            labelRes = R.string.package_list_filter_only_enabled,
                        ) {
                            filterOnlyEnabled = true
                        }
                    }
                    AppBarAction(
                        icon = Icons.Filled.Search,
                        labelRes = R.string.package_list_search
                    ) {
                        setCurrentSearch("")
                    }
                },
            )
        },
    ) { paddingValues ->
        PackageList(
            paddingValues,
            installedPackages,
            enabledPackages,
            ::setPackageEnabled,
            filterOnlyEnabled,
            filterSearch = null,
        )
        if (disableAllDialogVisible) {
            AlertDialog(
                title = { Text(stringResource(R.string.package_list_dialog_deselect_all_title)) },
                text = { Text(stringResource(R.string.package_list_dialog_deselect_all_body)) },
                onDismissRequest = { setDisableAllDialogVisible(false) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            setDisableAllDialogVisible(false)
                            coroutineScope.launch { enabledPackagesDataStore.disableAll() }
                        }
                    ) {
                        Text(stringResource(R.string.package_list_deselect_all))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { setDisableAllDialogVisible(false) }) {
                        Text(stringResource(id = android.R.string.cancel))
                    }
                },
            )
        }
    }
}

private data class PackageInfo(val name: String, val icon: Drawable, val label: String)

@Composable
private fun PackageList(
    paddingValues: PaddingValues,
    installedPackages: List<PackageInfo>?,
    enabledPackages: Set<String>?,
    setPackageEnabled: suspend (String, Boolean) -> Unit,
    filterOnlyEnabled: Boolean,
    filterSearch: String?,
) {
    val filterSearchLowered = filterSearch?.lowercase()
    val coroutineScope = rememberCoroutineScope()

    if (installedPackages == null || enabledPackages == null) {
        Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.package_list_loading))
        }
    } else {
        LazyColumn(
            contentPadding = paddingValues,
        ) {
            item { Spacer(modifier = Modifier.size(4.dp)) }

            items(installedPackages, key = { installedPackage -> installedPackage.name }) {
                packageInfo ->
                val enabled = enabledPackages.contains(packageInfo.name)
                val hideForDisabled = filterOnlyEnabled && !enabled
                val hideForSearch =
                    filterSearchLowered != null &&
                        !packageInfo.label.lowercase().contains(filterSearchLowered)

                if (!hideForDisabled && !hideForSearch) {
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
