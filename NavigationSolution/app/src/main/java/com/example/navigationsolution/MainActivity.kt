package com.example.navigationsolution

import MapRepository
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.navigationsolution.ui.theme.AppTheme
import kotlinx.serialization.Serializable

@Serializable
object OpeningScreen

@Serializable
data class IndoorMapScreen(
    val from: Int = NO_PATH,
    val to: Int = NO_PATH,
    val buildingId: Int = -1
    )

@Serializable
data class IndoorSearchScreen(
    val from: Int = NO_PATH,
    val to: Int = NO_PATH,
    val buildingId: Int = -1
)

@Serializable
data class SettingsScreen(
    val from: Int = NO_PATH,
    val to: Int = NO_PATH,
    val buildingId: Int = -1
)

var altColours = false
class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MapRepository.initialize(this.applicationContext)

        setContent {
            // var altColours by remember { mutableStateOf(false) }
            AppTheme(altColours) {
                Surface() {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = OpeningScreen
                    ){
                        composable<OpeningScreen> {
                            LoadingScreen(navController)
                        }

                        composable<IndoorMapScreen> { backStackEntry ->
                            val dir: IndoorMapScreen = backStackEntry.toRoute()
                            IndoorBars(
                                navController,
                                dir.from,
                                dir.to,
                                dir.buildingId
                            )
                        }

                        composable<IndoorSearchScreen> { backStackEntry ->
                            val dir: IndoorSearchScreen = backStackEntry.toRoute()
                            IndoorSearch(
                                navController,
                                dir.from,
                                dir.to,
                                dir.buildingId
                            )
                        }

                        composable<SettingsScreen> { backStackEntry ->
                            val dir: SettingsScreen = backStackEntry.toRoute()
                            Settings(
                                navController,
                                dir.from,
                                dir.to,
                                dir.buildingId
                            )
                        }
                    }

                }
            }
        }
    }
}

@Composable
fun LoadingScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.uwlogo),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .height(150.dp)
                .fillMaxWidth()
                .clip(shape = RoundedCornerShape(10.dp))
        )

        Text("Pathfinder", style = MaterialTheme.typography.displayLarge)

        val buttonText = remember { mutableStateOf("Go") }
        Button(onClick = { navController.navigate(route = IndoorMapScreen()) }) {
            Text(text = buttonText.value)
        }
    }


}