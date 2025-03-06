package com.example.navigationsolution

import MapRepository
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.compose.runtime.livedata.observeAsState
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
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.navigationsolution.ui.theme.AppTheme
import com.example.navigationsolution.viewmodels.IndoorViewModel
import com.example.navigationsolution.viewmodels.SettingsViewModel
import kotlinx.serialization.Serializable


import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.navigationsolution.R
import com.example.navigationsolution.service.SupabaseService
import com.example.navigationsolution.ui.theme.AppTheme
import kotlinx.coroutines.launch
import com.example.navigationsolution.ui.mode.ModeScreen
import com.example.navigationsolution.ui.auth.LoginScreen


@Serializable
object OpeningScreen

@Serializable
object ModeSelectionScreen

@Serializable
object LoginScreenRoute

@Serializable
object IndoorMapScreen

@Serializable
object IndoorSearchScreen

@Serializable
object SettingsScreen

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MapRepository.initialize(this.applicationContext)

        // 连通性测试：调用 SupabaseService.fetchAllUsers() 并在 Logcat 输出结果【Connectivity test: Call SupabaseService.fetchAllUsers() and output results in Logcat】
        lifecycleScope.launch {
            try {
                val users = SupabaseService.fetchAllUsers()
                Log.d("SupabaseTest", "Users Fetched: $users")
            } catch (e: Exception) {
                Log.e("SupabaseTest", "Error fetching users: ${e.message}", e)
            }
        }


        setContent {
            val settingsModel: SettingsViewModel by viewModels()
            val altColours = settingsModel.altColours.observeAsState(initial = false)
            val textScale = settingsModel.textScale.observeAsState(initial = 1f)

            val indoorModel: IndoorViewModel by viewModels()
            AppTheme(darkTheme = altColours.value, textScale = textScale.value) {
                Surface() {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = ModeSelectionScreen
                    ){
                        composable<ModeSelectionScreen> {
                            ModeScreen(navController)
                        }

                        composable<LoginScreenRoute> {
                            LoginScreen(navController)
                        }

                        composable<OpeningScreen> {
                            LoadingScreen(navController)
                        }

                        composable<IndoorMapScreen> { backStackEntry ->
                            val dir: IndoorMapScreen = backStackEntry.toRoute()
                            IndoorBars(
                                navController,
                                indoorModel
                            )
                        }

                        composable<IndoorSearchScreen> { backStackEntry ->
                            val dir: IndoorSearchScreen = backStackEntry.toRoute()
                            IndoorSearch(
                                navController,
                                indoorModel
                            )
                        }

                        composable<SettingsScreen> { backStackEntry ->
                            val dir: SettingsScreen = backStackEntry.toRoute()
                            Settings(
                                navController,
                                settingsModel
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
        Button(onClick = { navController.navigate(route = IndoorMapScreen) }) {
            Text(text = buttonText.value)
        }
    }


}