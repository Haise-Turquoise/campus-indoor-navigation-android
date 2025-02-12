package com.example.navigationsolution

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController

@Composable
fun IndoorSearch(navController: NavController, from: Int = NO_PATH, to: Int = NO_PATH,
                 imageID: Int = R.drawable.uwlogo) {
    Column (
        Modifier
            .background(Color(android.graphics.Color.WHITE))
            .fillMaxSize(),

        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(
            onClick = {navController.navigate(IndoorMapScreen(from, to, imageID))},
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack , contentDescription = "")
            Text(text = "Go Back")
        }

        var cur by remember { mutableStateOf("") }
        var dest by remember { mutableStateOf("") }
        Column (
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            OutlinedTextField(
                value = cur,
                onValueChange = { cur = it },
                label = { Text("Current/Nearby Room") }
            )

            OutlinedTextField(
                value = dest,
                onValueChange = { dest = it },
                label = { Text("Target Room") }
            )
        }

        Row (
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            FloatingActionButton (
                onClick = {
                    navController.navigate(IndoorMapScreen(cur.toInt(), dest.toInt(), imageID))
                          },
            ) {
                Icon(Icons.Outlined.Search , contentDescription = "")
            }
        }


    }
}