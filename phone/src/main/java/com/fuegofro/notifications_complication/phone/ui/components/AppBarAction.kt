@file:OptIn(ExperimentalMaterial3Api::class)

package com.fuegofro.notifications_complication.phone.ui.components

import androidx.annotation.StringRes
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource

@Composable
fun AppBarAction(icon: ImageVector, @StringRes labelRes: Int, onClick: () -> Unit) {
    val tooltipState = rememberTooltipState()
    val label = stringResource(labelRes)

    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(label) } },
        state = tooltipState
    ) {
        IconButton(onClick) {
            Icon(icon, contentDescription = label)
        }
    }
}
