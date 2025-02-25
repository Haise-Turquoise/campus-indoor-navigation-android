package com.example.navigationsolution

import MapRepository
import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import com.example.navigationsolution.ui.theme.AppTheme

const val NO_PATH = -1

// top and bottom bars
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndoorBars(navController: NavController, from:Int = NO_PATH, to:Int = NO_PATH,
               buildingId: Int = -1) {
    Box (
        modifier = Modifier
        .fillMaxSize(),
        ) {

        IndoorBox(from, to, buildingId)

        val topEdgePadding = 50.dp
        val sideEdgePadding = 10.dp

        Row (
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = topEdgePadding, horizontal = sideEdgePadding)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text("Show me...", modifier = Modifier.padding(horizontal = sideEdgePadding))

            IconButton(onClick = {}) {
                Icon(Icons.Filled.Menu , contentDescription = "")
            }
        }


        Row (
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .pointerInput(Unit) {
                    detectDragGestures { _, _ ->
                        navController.navigate(
                            route = IndoorSearchScreen(from, to, buildingId)
                        )
                    }
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column (
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ){
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Outlined.Search, contentDescription = "")
                    Text(text = "Swipe up to search")
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = sideEdgePadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (from == NO_PATH && to == NO_PATH) {
                        Text(text = "Building Name")
                    } else {
                        Text(text = "$from to $to")
                    }

                    Button(onClick = { }) {
                        Text(text = "View Outside")
                    }
                }
            }
        }
    }
}

// draws the map and the side buttons
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
fun IndoorBox(from: Int = NO_PATH, to: Int = NO_PATH,
              buildingId: Int = -1) {
    Box(Modifier
        .fillMaxSize()
        .background(Color(red = 0x00, green = 0x00, blue = 0x00, alpha = 0x99))) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        )
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            IndoorMap(from, to, buildingId)
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)  // This adds even spacing between buttons
        ){
            val buttonSize: Dp = 50.dp

            FloatingActionButton(
                onClick = {},
                shape = CircleShape,
                modifier = Modifier
                    .size(width = buttonSize, height = buttonSize)
            ) {
                Icon(Icons.Outlined.Person, contentDescription = "")
            }

            FloatingActionButton(
                onClick = {},
                shape = CircleShape,
                modifier = Modifier
                    .size(width = buttonSize, height = buttonSize)
            ) {
                Icon(Icons.Outlined.Settings, contentDescription = "")
            }

            FloatingActionButton(
                onClick = { },
                shape = CircleShape,
                modifier = Modifier
                    .size(width = buttonSize, height = buttonSize)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.navigation),
                    contentDescription = "Compass",
                    modifier = Modifier.size(24.dp)
                )
            }

            FloatingActionButton(
                onClick = {},
                shape = CircleShape,
                modifier = Modifier
                    .size(width = buttonSize, height = buttonSize)
            ) {
                Icon(Icons.Outlined.LocationOn, contentDescription = "")
            }
        }
    }
}

// displays the map
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
fun IndoorMap(from: Int = NO_PATH, to: Int = NO_PATH,
              buildingId: Int = -1
              ) {

    // from https://developer.android.com/develop/ui/compose/touch-input/pointer-input/multi-touch
    var scale by remember { mutableStateOf(1f) }
    var rotation by remember { mutableStateOf(0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoomChange, offsetChange, rotationChange ->
        scale *= zoomChange
        rotation += rotationChange
        offset += offsetChange
    }

    Image(
//        painter = painterResource(id = buildingId),
        bitmap = MapRepository.getMarkedPlan(buildingId, from, to)[0].asImageBitmap(), // ! FLOOR CONSTANT FOR TESTING, CHANGE THIS
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                rotationZ = rotation,
                translationX = offset.x,
                translationY = offset.y
            )
            .transformable(state = state)
            .fillMaxSize()
    )


}