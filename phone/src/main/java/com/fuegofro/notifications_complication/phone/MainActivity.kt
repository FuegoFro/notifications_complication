package com.fuegofro.notifications_complication.phone

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fuegofro.notifications_complication.phone.ui.theme.NotificationsComplicationTheme
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.launch

val Context.enabledPackagesDataStore by preferencesDataStore("enabledPackages")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NotificationsComplicationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    PackageList(packageManager, enabledPackagesDataStore)
                }
            }
        }
    }
}

data class PackageInfo(val name: String, val icon: Drawable, val label: String)

@Composable
fun PackageList(packageManager: PackageManager, enabledPackagesDataStore: DataStore<Preferences>) {
    val coroutineScope = rememberCoroutineScope()

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

    if (installedPackages == null || enabledPackages == null) {
        Box(contentAlignment = Alignment.Center) { Text(text = "Loading...") }
    } else {
        // TODO - This is *way* more performant/smooth than the LazyColumn 🤔
        Column(
            modifier = Modifier.padding(top = 8.dp).verticalScroll(rememberScrollState()),
        ) {
            installedPackages.forEach { packageInfo ->
                PackageRow(
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                    packageInfo = packageInfo,
                    enabled = enabledPackages.contains(packageInfo.name),
                    setEnabled = { enabled: Boolean ->
                        coroutineScope.launch { setPackageEnabled(packageInfo.name, enabled) }
                    },
                )
            }
        }

        // LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
        //     items(
        //         installedPackages,
        //         key = { installedPackage -> installedPackage.name }
        //     ) { packageInfo ->
        //         PackageRow(...)
        //     }
        // }
    }
}

@Composable
fun PackageRow(
    packageInfo: PackageInfo,
    enabled: Boolean,
    setEnabled: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier) {
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
