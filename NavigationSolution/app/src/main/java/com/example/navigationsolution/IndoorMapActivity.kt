package com.example.navigationsolution

import android.annotation.SuppressLint
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

const val NOPATH = -1

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndoorBars(navController: NavController, from:Int = NOPATH, to:Int = NOPATH,
               imageID: Int = R.drawable.uwlogo) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Show me...") },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Filled.Menu , contentDescription = "")
                    }
                },
                colors =
                    TopAppBarColors(
                        containerColor = Color(red = 0xFF, green = 0xFF, blue = 0xFF, alpha = 0xC9),
                        scrolledContainerColor = Color.Transparent,
                        navigationIconContentColor = Color(android.graphics.Color.BLACK),
                        titleContentColor = Color(android.graphics.Color.BLACK),
                        actionIconContentColor = Color(android.graphics.Color.BLACK),

                    )
            )
        },

        content = {  IndoorBox(from, to, imageID) },

        bottomBar = {
            BottomAppBar(modifier = Modifier
                .pointerInput(Unit) {
                    detectDragGestures { _, _ ->  navController.navigate(
                        route = IndoorSearchScreen(from, to, imageID))}
                }) {

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
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (from == NOPATH && to == NOPATH) {
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

        )

}


@Composable
fun IndoorBox(from: Int = NOPATH, to: Int = NOPATH,
              imageID: Int = R.drawable.uwlogo) {
    Box(Modifier
        .fillMaxSize()
        .background(Color(red = 0x00, green = 0x00, blue = 0x00, alpha = 0x99))) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            IndoorMap(from, to, imageID)
        }

        Column(
            modifier = Modifier.align(Alignment.CenterEnd)
        ){
            val buttonSize: Dp = 50.dp
            FloatingActionButton(
                onClick = {},
                shape = CircleShape,
                modifier = Modifier
                    .size(width = buttonSize, height = buttonSize)
                    .offset(y = (-10).dp)
            ) {
                Icon(Icons.Outlined.Person , contentDescription = "")
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
                onClick = {},
                shape = CircleShape,
                modifier = Modifier
                    .size(width = buttonSize, height = buttonSize)
                    .offset(y = 10.dp)
            ) {
                Icon(Icons.Outlined.LocationOn, contentDescription = "")
            }
        }
    }
}

@Composable
fun IndoorMap(from: Int = NOPATH, to: Int = NOPATH,
              imageID: Int = R.drawable.uwlogo
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

    Image(painter = painterResource(id = imageID),
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