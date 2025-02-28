package com.example.navigationsolution

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.navigationsolution.ui.theme.updateTextScale

@Composable
fun Settings(navController: NavController, from:Int = NO_PATH, to:Int = NO_PATH,
             imageID: Int = -1) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Top App Bar with back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.navigate(IndoorMapScreen(from, to, imageID)) }
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Title
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Settings

        val sideEdgePadding = 10.dp
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = sideEdgePadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "High Contrast",
                style = MaterialTheme.typography.titleSmall
            )

            // based on https://developer.android.com/develop/ui/compose/components/switch
            var highContrastChecked by remember { mutableStateOf(false) }

            Switch(
                checked = highContrastChecked,
                onCheckedChange = {
                    highContrastChecked = it
                    altColours = !altColours
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedThumbColor = MaterialTheme.colorScheme.secondary,
                    uncheckedTrackColor = MaterialTheme.colorScheme.secondaryContainer,
                )
            )

        }

        Row (
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = sideEdgePadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Text Size",
                style = MaterialTheme.typography.titleSmall
            )

            // from https://developer.android.com/develop/ui/compose/components/slider
            var sliderPosition by remember { mutableFloatStateOf(0f) }

            Slider(
                value = sliderPosition,
                valueRange = 0f..1f,
                onValueChange = {
                    sliderPosition = it
                    updateTextScale(it.toDouble())
                }
            )
        }


    }
}