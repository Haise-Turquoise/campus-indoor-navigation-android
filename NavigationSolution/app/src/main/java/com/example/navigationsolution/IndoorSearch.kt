package com.example.navigationsolution

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.navigationsolution.viewmodels.IndoorViewModel
import kotlinx.coroutines.launch

@Composable
fun IndoorSearch(
    navController: NavController,
    indoorViewModel: IndoorViewModel
) {
    var cur by remember { mutableStateOf("") }
    var dest by remember { mutableStateOf("") }

    // Building selection state
    val buildings = listOf("MC", "DC", "SLC", "E5", "E7", "QNC")
    var currentBuilding by remember { mutableStateOf("MC") }
    var targetBuilding by remember { mutableStateOf("MC") }

    // Animation state
    val scope = rememberCoroutineScope()
    val searchOffset = remember { Animatable(0f) }

    Column(
        modifier = Modifier
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
                onClick = { navController.navigate("IndoorMapScreen") }
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
            text = "Indoor Navigation",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Search Fields Container
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFEEEEF0) // Light gray background
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Current Building and Room Row
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Label for first row
                    Text(
                        text = "Start Location",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Building selector - Simplified version
                        var currentExpanded by remember { mutableStateOf(false) }

                        Box(
                            modifier = Modifier
                                .width(100.dp)
                                .height(56.dp)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = MaterialTheme.shapes.small
                                )
                                .clip(MaterialTheme.shapes.small)
                                .clickable { currentExpanded = !currentExpanded }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(currentBuilding)
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "Dropdown",
                                    tint = Color.DarkGray
                                )
                            }

                            DropdownMenu(
                                expanded = currentExpanded,
                                onDismissRequest = { currentExpanded = false },
                                modifier = Modifier
                                    .width(100.dp)
                                    .background(MaterialTheme.colorScheme.surface)
                            ) {
                                buildings.forEach { building ->
                                    DropdownMenuItem(
                                        text = { Text(building) },
                                        onClick = {
                                            currentBuilding = building
                                            currentExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Room input with specific placeholder
                        OutlinedTextField(
                            value = cur,
                            onValueChange = { cur = it },
                            placeholder = { Text("Starting Room") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Target Building and Room Row
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Label for second row
                    Text(
                        text = "Destination",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Building selector - Simplified version
                        var targetExpanded by remember { mutableStateOf(false) }

                        Box(
                            modifier = Modifier
                                .width(100.dp)
                                .height(56.dp)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = MaterialTheme.shapes.small
                                )
                                .clip(MaterialTheme.shapes.small)
                                .clickable { targetExpanded = !targetExpanded }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(targetBuilding)
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "Dropdown",
                                    tint = Color.DarkGray
                                )
                            }

                            DropdownMenu(
                                expanded = targetExpanded,
                                onDismissRequest = { targetExpanded = false },
                                modifier = Modifier
                                    .width(100.dp)
                                    .background(MaterialTheme.colorScheme.surface)
                            ) {
                                buildings.forEach { building ->
                                    DropdownMenuItem(
                                        text = { Text(building) },
                                        onClick = {
                                            targetBuilding = building
                                            targetExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Room input with specific placeholder
                        OutlinedTextField(
                            value = dest,
                            onValueChange = { dest = it },
                            placeholder = { Text("Target Room") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Search Button
                Button(
                    onClick = {
                        scope.launch {
                            searchOffset.animateTo(
                                targetValue = -50f,
                                animationSpec = tween(
                                    durationMillis = 500,
                                    easing = FastOutSlowInEasing
                                )
                            )

                            // Safely convert room numbers with null handling
                            val curRoom = cur.toIntOrNull() ?: 0
                            val destRoom = dest.toIntOrNull() ?: 0

                            indoorViewModel.updatePath(curRoom, destRoom)
                            indoorViewModel.setFloor(0)
                            navController.navigate("IndoorMapScreen")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .graphicsLayer {
                            translationY = searchOffset.value
                        },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4A639E) // Blue button color
                    )
                ) {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = "Search",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Search Route")
                }
            }
        }
    }
}

// Returns the floor number given a room ID
fun getFloor(roomId: Int): Int {
    return roomId / 1000
}