package com.example.navigationsolution

import MapRepository
import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import com.example.navigationsolution.ui.theme.AppTheme
import com.example.navigationsolution.viewmodels.IndoorViewModel
import kotlin.math.max
import kotlin.math.min
import kotlinx.serialization.Serializable
import com.example.navigationsolution.ui.auth.InfoScreen
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.layout.width

const val NO_PATH = -1

// top and bottom bars
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndoorBars(navController: NavController, indoorViewModel: IndoorViewModel) {
    val from = indoorViewModel.from
    val to = indoorViewModel.to
    val buildingId = indoorViewModel.buildingId

    Box (
        modifier = Modifier
        .fillMaxSize(),
        ) {

        IndoorBox(navController, indoorViewModel)

        val topEdgePadding = 50.dp
        val sideEdgePadding = 10.dp

        var showBuildingDropdown by remember { mutableStateOf(false) }
        val buildings = listOf("MC", "DC", "SLC", "E5", "E7", "QNC")
        var selectedBuilding by remember { mutableStateOf("MC") }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = topEdgePadding, horizontal = sideEdgePadding)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Show me ${selectedBuilding}...",
                    modifier = Modifier.padding(horizontal = sideEdgePadding),
                    style = MaterialTheme.typography.titleLarge
                )

                IconButton(onClick = { showBuildingDropdown = true }) {
                    Icon(Icons.Filled.Menu, contentDescription = "Building Menu")
                }

                DropdownMenu(
                    expanded = showBuildingDropdown,
                    onDismissRequest = { showBuildingDropdown = false },
                    modifier = Modifier
                        .width(250.dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        "Select Building",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.primary
                        ),
                        textAlign = TextAlign.Center
                    )

                    Divider(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    buildings.forEach { building ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    building,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            },
                            onClick = {
                                selectedBuilding = building
                                showBuildingDropdown = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )

                        if (building != buildings.last()) {
                            Divider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
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
                            route = IndoorSearchScreen
                        )
                    }
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            val bottomBoxPadding = 50.dp
            Column (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = bottomBoxPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ){
                val bottomInternalPadding = 10.dp
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = bottomInternalPadding),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Outlined.Search, contentDescription = "")
                    Text(text = "Swipe up to search", style = MaterialTheme.typography.bodyMedium)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = bottomInternalPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = {
                        indoorViewModel.decrFloor()
                    }) {
                        Text(text = "Prev.\n Floor", textAlign = TextAlign.Center)
                    }

                    if (from == NO_PATH && to == NO_PATH) {
                        Text(text = "Building Name", style = MaterialTheme.typography.titleLarge)
                    } else {
                        Text(text = "$from to $to", style = MaterialTheme.typography.titleLarge)
                    }

                    Button(onClick = {
                        indoorViewModel.incrFloor()
                    }) {
                        Text(text = "Next\n Floor", textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

// draws the map and the side buttons
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
fun IndoorBox(navController: NavController, indoorViewModel: IndoorViewModel) {
    val from = indoorViewModel.from
    val to = indoorViewModel.to
    val buildingId = indoorViewModel.buildingId

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
            IndoorMap(indoorViewModel)
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)  // This adds even spacing between buttons
        ){
            val buttonSize: Dp = 50.dp

            FloatingActionButton(
                onClick = {
                    // 导航到信息页面
                    navController.navigate(route = InfoScreenRoute) // 正确：使用路由对象
                },
                shape = CircleShape,
                modifier = Modifier
                    .size(width = buttonSize, height = buttonSize)
            ) {
                Icon(Icons.Outlined.Person, contentDescription = "查看账户信息")
            }

            FloatingActionButton(
                onClick = {navController.navigate(
                    route = SettingsScreen
                )},
                shape = CircleShape,
                modifier = Modifier
                    .size(width = buttonSize, height = buttonSize)
            ) {
                Icon(Icons.Outlined.Settings, contentDescription = "")
            }

            // Modified compass button to toggle rotation
            val compassEnabled = indoorViewModel.compassEnabled.observeAsState(initial = true)
            FloatingActionButton(
                onClick = { indoorViewModel.toggleCompass() },  // Toggle compass mode
                shape = CircleShape,
                // Change color based on toggle state
                containerColor = if (compassEnabled.value)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .size(width = buttonSize, height = buttonSize)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.navigation),
                    contentDescription = "Toggle Compass Mode",
                    // Change icon color based on toggle state
                    tint = if (compassEnabled.value)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
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

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
fun IndoorMap(indoorViewModel: IndoorViewModel
              ) {
    val from = indoorViewModel.from
    val to = indoorViewModel.to
    val buildingId = indoorViewModel.buildingId
    val compassEnabled = indoorViewModel.compassEnabled.observeAsState(initial = true)

    // from https://developer.android.com/develop/ui/compose/touch-input/pointer-input/multi-touch
    // Get context for sensor access
    val context = LocalContext.current

    // State variables for map transformations
    var scale by remember { mutableStateOf(1f) }
    var mapRotation by remember { mutableStateOf(0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Debug text state
    var sensorStatus by remember { mutableStateOf("Compass mode: OFF") }

    // Reset rotation when toggling compass mode off
    LaunchedEffect(compassEnabled.value) {
        if (!compassEnabled.value) {
            // Reset rotation to 0 when compass mode is turned off
            mapRotation = 0f
        }
    }

    // Transformable state for zoom, pan, and manual rotation (if compass disabled)
    val state = rememberTransformableState { zoomChange, offsetChange, rotationChange ->
        scale *= zoomChange
        // Only allow manual rotation when compass is disabled
        if (!compassEnabled.value) {
            mapRotation += rotationChange
        }
        offset += offsetChange
    }

    // Set up sensor for rotation only when compass is enabled
    DisposableEffect(compassEnabled.value) {
        // Only set up sensors if compass is enabled
        if (compassEnabled.value) {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

            // Try to get rotation vector sensor (most accurate)
            val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

            // Fallback sensors
            val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

            // Arrays for sensor readings
            val accelerometerReading = FloatArray(3)
            val magnetometerReading = FloatArray(3)
            val rotationMatrix = FloatArray(9)
            val orientationAngles = FloatArray(3)

            // Create sensor listener
            val sensorListener = object : SensorEventListener {
                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                    // Not needed for this implementation
                }

                override fun onSensorChanged(event: SensorEvent) {
                    when (event.sensor.type) {
                        Sensor.TYPE_ROTATION_VECTOR -> {
                            // Process rotation vector data
                            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                            SensorManager.getOrientation(rotationMatrix, orientationAngles)

                            // Convert radians to degrees (azimuth is orientationAngles[0])
                            val azimuthInDegrees = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                            val normalizedAzimuth = (azimuthInDegrees + 360) % 360

                            // Update map rotation (negate to rotate map correctly)
                            mapRotation = -normalizedAzimuth
                            sensorStatus = "Compass: ${normalizedAzimuth.toInt()}°"
                        }
                        Sensor.TYPE_ACCELEROMETER -> {
                            // Store accelerometer data
                            System.arraycopy(event.values, 0, accelerometerReading, 0, 3)
                        }
                        Sensor.TYPE_MAGNETIC_FIELD -> {
                            // Store magnetic field data
                            System.arraycopy(event.values, 0, magnetometerReading, 0, 3)

                            // Check if we have both sensor readings
                            if (accelerometerReading[0] != 0f || accelerometerReading[1] != 0f ||
                                accelerometerReading[2] != 0f) {

                                // Calculate rotation matrix
                                val success = SensorManager.getRotationMatrix(
                                    rotationMatrix, null, accelerometerReading, magnetometerReading
                                )

                                if (success) {
                                    // Get orientation
                                    SensorManager.getOrientation(rotationMatrix, orientationAngles)

                                    // Convert to degrees
                                    val azimuthInDegrees = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                                    val normalizedAzimuth = (azimuthInDegrees + 360) % 360

                                    // Update map rotation
                                    mapRotation = -normalizedAzimuth
                                    sensorStatus = "Compass: ${normalizedAzimuth.toInt()}°"
                                }
                            }
                        }
                    }
                }
            }

            // Register sensors
            if (rotationVectorSensor != null) {
                sensorManager.registerListener(
                    sensorListener,
                    rotationVectorSensor,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
                sensorStatus = "Compass mode: ON"
            } else if (accelerometer != null && magnetometer != null) {
                // Register accelerometer
                sensorManager.registerListener(
                    sensorListener,
                    accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL
                )

                // Register magnetometer
                sensorManager.registerListener(
                    sensorListener,
                    magnetometer,
                    SensorManager.SENSOR_DELAY_NORMAL
                )

                sensorStatus = "Compass mode: ON"
            } else {
                sensorStatus = "Compass mode: NO SENSORS"
            }

            // Cleanup when component is disposed or compass is disabled
            onDispose {
                sensorManager.unregisterListener(sensorListener)
            }
        } else {
            // Update status when compass is disabled
            sensorStatus = "Compass mode: OFF"

            // No cleanup needed when compass is disabled
            onDispose { }
        }
    }

    var markedPlan = MapRepository.getMarkedPlan(buildingId, from, to)
    if (from != to) {
        markedPlan = MapRepository.getMarkedPlan(buildingId, from, buildingId, to)
    }
    indoorViewModel.updatePath(newMaxFloor = markedPlan.size - 1)
    val floor = indoorViewModel.liveFloor.observeAsState(initial = 0)

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
//        painter = painterResource(id = buildingId),
            bitmap = markedPlan[floor.value]!!.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    rotationZ = mapRotation,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .transformable(state = state)
                .fillMaxSize()
        )
    }

}