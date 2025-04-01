package com.example.navigationsolution

import MapRepository.roomExists
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.navigationsolution.service.SessionManager
import com.example.navigationsolution.viewmodels.IndoorViewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.focus.onFocusChanged

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
fun IndoorSearch(
    navController: NavController,
    indoorViewModel: IndoorViewModel
) {
    var cur by remember { mutableStateOf("") }
    var dest by remember { mutableStateOf("") }
    
    // 控制起点和目标输入框的下拉建议显示【Control dropdown suggestions display for start and target input fields】
    var startSuggestionsVisible by remember { mutableStateOf(false) }
    var targetSuggestionsVisible by remember { mutableStateOf(false) }

    // Building selection state
    val buildings = listOf("E7", "E6")
    var currentBuilding by remember { mutableStateOf("E7") }
    var targetBuilding by remember { mutableStateOf("E7") }
    
    // 获取搜索历史【Get search history】
    val searchHistory by indoorViewModel.searchHistory.collectAsState()
    
    // 限制只显示前三个搜索记录【Limit to show only the first three search records】
    val limitedHistory = searchHistory.take(3)
    
    // 从搜索历史中提取房间号【Extract room numbers from search history】
    // 直接使用房间号，不再使用建筑物名称【Directly use room numbers, no longer using building names】
    val roomSuggestions = limitedHistory.take(3) // 只保留前3个【Keep only the first 3】

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
                onClick = { navController.navigate(IndoorMapScreen) }
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回【Back】",
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
                                    contentDescription = "下拉菜单【Dropdown】",
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

                        // Room input with specific placeholder and suggestions
                        Box(modifier = Modifier.weight(1f)) {
                            var hasFocus by remember { mutableStateOf(false) }
                            
                            OutlinedTextField(
                                value = cur,
                                onValueChange = { 
                                    cur = it
                                    // 不在每次输入时都显示/隐藏建议，而是根据焦点状态决定【Don't show/hide suggestions with each input, decide based on focus state】
                                },
                                placeholder = { Text("Starting Room") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { focusState ->
                                        hasFocus = focusState.isFocused
                                        // 只在获得焦点时控制显示建议，但不在失去焦点时立即隐藏【Only control suggestion display when gaining focus, but don't hide immediately when losing focus】
                                        if (focusState.isFocused) {
                                            startSuggestionsVisible = roomSuggestions.isNotEmpty()
                                        }
                                    }
                            )
                            
                            // 起点房间号的建议下拉框【Suggestion dropdown for start room number】
                            DropdownMenu(
                                expanded = startSuggestionsVisible && hasFocus,
                                onDismissRequest = { 
                                    // 关闭菜单但不影响焦点和输入状态【Close menu but don't affect focus and input state】
                                    startSuggestionsVisible = false 
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                            ) {
                                roomSuggestions.forEach { room ->
                                    DropdownMenuItem(
                                        text = { Text("Room $room") },
                                        onClick = {
                                            currentBuilding = "E7"
                                            cur = room
                                            // 选择后关闭建议，但不影响焦点【Close suggestions after selection, but don't affect focus】
                                            startSuggestionsVisible = false
                                        }
                                    )
                                }
                            }
                        }
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
                                    contentDescription = "下拉菜单【Dropdown】",
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

                        // Room input with specific placeholder and suggestions
                        Box(modifier = Modifier.weight(1f)) {
                            var hasFocus by remember { mutableStateOf(false) }
                            
                            OutlinedTextField(
                                value = dest,
                                onValueChange = { 
                                    dest = it
                                    // 不在每次输入时都显示/隐藏建议，而是根据焦点状态决定【Don't show/hide suggestions with each input, decide based on focus state】
                                },
                                placeholder = { Text("Target Room") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { focusState ->
                                        hasFocus = focusState.isFocused
                                        // 只在获得焦点时控制显示建议，但不在失去焦点时立即隐藏【Only control suggestion display when gaining focus, but don't hide immediately when losing focus】
                                        if (focusState.isFocused) {
                                            targetSuggestionsVisible = roomSuggestions.isNotEmpty()
                                        }
                                    }
                            )
                            
                            // 目标房间号的建议下拉框【Suggestion dropdown for target room number】
                            DropdownMenu(
                                expanded = targetSuggestionsVisible && hasFocus,
                                onDismissRequest = {
                                    // 关闭菜单但不影响焦点和输入状态【Close menu but don't affect focus and input state】
                                    targetSuggestionsVisible = false 
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                            ) {
                                roomSuggestions.forEach { room ->
                                    DropdownMenuItem(
                                        text = { Text("Room $room") },
                                        onClick = {
                                            targetBuilding = "E7"
                                            dest = room
                                            // 选择后关闭建议，但不影响焦点【Close suggestions after selection, but don't affect focus】
                                            targetSuggestionsVisible = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Search Button
                val context = LocalContext.current
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
                            val curRoom = cur.toIntOrNull() ?: NO_PATH
                            val destRoom = dest.toIntOrNull() ?: NO_PATH



                            // Check if room is invalid before attempting search
                            if (!roomExists(indoorViewModel.getBuildingID(currentBuilding), curRoom) ||
                                !roomExists(indoorViewModel.getBuildingID(targetBuilding), destRoom)
                            ) {
                                val text = "Invalid Room"
                                val duration = Toast.LENGTH_SHORT

                                val toast = Toast.makeText(context, text, duration)
                                toast.show()

                                searchOffset.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(
                                        durationMillis = 500,
                                        easing = FastOutSlowInEasing
                                    )
                                )
                            } else {
                                indoorViewModel.updatePath(
                                    newFrom = curRoom,
                                    newTo = destRoom,
                                    newBuildingFrom = indoorViewModel.getBuildingID(currentBuilding),
                                    newBuildingTo = indoorViewModel.getBuildingID(targetBuilding)
                                )
                                indoorViewModel.setFloor(0)
                                navController.navigate(IndoorMapScreen)
                            }
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
                        contentDescription = "搜索【Search】",
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