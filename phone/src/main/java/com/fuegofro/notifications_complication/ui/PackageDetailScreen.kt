@file:OptIn(ExperimentalMaterial3Api::class)

package com.fuegofro.notifications_complication.ui

import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fuegofro.notifications_complication.R
import com.fuegofro.notifications_complication.data.EnabledPackagesDataStore.Companion.enabledPackagesDataStore
import com.fuegofro.notifications_complication.data.PackageSettings
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.launch

@Composable
fun PackageDetailScreen(
    packageId: String,
    onNavigateUp: () -> Unit,
    refreshNotifications: suspend () -> Unit,
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val enabledPackagesDataStore = context.enabledPackagesDataStore
    val coroutineScope = rememberCoroutineScope()
    
    val packageSettingsMap by enabledPackagesDataStore.packageSettings.collectAsStateWithLifecycle(
        initialValue = emptyMap()
    )
    
    var packageSettings by remember { mutableStateOf(PackageSettings.DEFAULT) }
    
    LaunchedEffect(packageSettingsMap, packageId) {
        packageSettings = packageSettingsMap[packageId] ?: PackageSettings.DEFAULT
    }
    
    val packageInfo = remember(packageId) {
        try {
            val applicationInfo = packageManager.getApplicationInfo(packageId, 0)
            val icon = packageManager.getApplicationIcon(applicationInfo)
            val label = packageManager.getApplicationLabel(applicationInfo).toString()
            Triple(icon, label, packageId)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }
    
    fun updateSettings(update: PackageSettings.() -> PackageSettings) {
        coroutineScope.launch {
            val newSettings = packageSettings.update()
            packageSettings = newSettings
            enabledPackagesDataStore.setPackageSettings(packageId, newSettings)
            refreshNotifications()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                title = { Text(stringResource(R.string.package_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.button_up),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        if (packageInfo != null) {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Package header
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            modifier = Modifier.size(64.dp),
                            painter = rememberDrawablePainter(packageInfo.first),
                            contentDescription = null,
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 16.dp)
                        ) {
                            Text(
                                text = packageInfo.second,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = packageInfo.third,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Basic settings
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.package_detail_basic_settings),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    updateSettings { copy(enabled = !enabled) }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.package_detail_notifications_enabled),
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = packageSettings.enabled,
                                onCheckedChange = { enabled ->
                                    updateSettings { copy(enabled = enabled) }
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Advanced settings
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.package_detail_advanced_settings),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = packageSettings.enabled) { 
                                    updateSettings { copy(showOngoing = !showOngoing) }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = stringResource(R.string.package_detail_show_ongoing))
                                Text(
                                    text = stringResource(R.string.package_detail_show_ongoing_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = packageSettings.showOngoing,
                                onCheckedChange = { showOngoing ->
                                    updateSettings { copy(showOngoing = showOngoing) }
                                },
                                enabled = packageSettings.enabled
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = packageSettings.enabled) { 
                                    updateSettings { copy(showSilent = !showSilent) }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = stringResource(R.string.package_detail_show_silent))
                                Text(
                                    text = stringResource(R.string.package_detail_show_silent_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = packageSettings.showSilent,
                                onCheckedChange = { showSilent ->
                                    updateSettings { copy(showSilent = showSilent) }
                                },
                                enabled = packageSettings.enabled
                            )
                        }
                    }
                }
            }
        }
    }
}
