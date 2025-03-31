package com.example.navigationsolution.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.navigationsolution.IndoorMapScreen
import com.example.navigationsolution.R
import androidx.compose.foundation.layout.size
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.io.FileWriter

// 错误信息使用的橙色【Error message orange color】
val ErrorOrange = Color(0xFFF57C00)

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel
) {
    // 状态变量【State variables】
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    // 收集登录结果状态【Collect login result state】
    val loginResult by viewModel.loginResult.collectAsState()
    
    // 处理登录成功【Handle successful login】
    LaunchedEffect(loginResult) {
        if (loginResult is LoginResult.Success) {
            navController.navigate(route = IndoorMapScreen) {
                // 清除返回栈上的登录页面【Clear login page from back stack】
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
            }
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶部标题区域【Top Title Area】
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
//                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp)
            ) {
                // 应用Logo【App Logo】
                Image(
                    painter = painterResource(id = R.drawable.uwlogo),
                    contentDescription = "App Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .height(100.dp)
                        .fillMaxWidth(0.5f)
                        .clip(shape = RoundedCornerShape(8.dp))
                )
                
//                Spacer(modifier = Modifier.height(16.dp))
                
                // 应用标题【App Title】
                Text(
                    text = "Pathfinder",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
//                Spacer(modifier = Modifier.height(8.dp))
                
                // 副标题【Subtitle】
                Text(
                    text = "Login to your account",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            // 输入表单区域 - 放置在顶部和按钮区域之间【Input Form Area - Placed between top and button areas】
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
//                    .offset(y = (-100).dp) // 将原来的-40dp改为-100dp，使表单在上下元素之间更好地居中【Changed from -40dp to -100dp to better center the form between elements】
            ) {
                // 用户名输入框【Username Input Field】
                OutlinedTextField(
                    value = username,
                    onValueChange = { 
                        username = it
                        // 重置错误状态【Reset error state】
                        if (loginResult !is LoginResult.Initial) {
                            viewModel.resetLoginState()
                        }
                    },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .padding(vertical = 8.dp)
                )
                
//                Spacer(modifier = Modifier.height(16.dp))
                
                // 密码输入框【Password Input Field】
                OutlinedTextField(
                    value = password,
                    onValueChange = { 
                        password = it
                        // 重置错误状态【Reset error state】
                        if (loginResult !is LoginResult.Initial) {
                            viewModel.resetLoginState()
                        }
                    },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .padding(vertical = 8.dp)
                )
                
                // 错误信息显示区域【Error Message Display Area】
                if (loginResult is LoginResult.UsernameError || 
                    loginResult is LoginResult.PasswordError || 
                    loginResult is LoginResult.GenericError) {
                    
//                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = when (loginResult) {
                            is LoginResult.UsernameError -> (loginResult as LoginResult.UsernameError).message
                            is LoginResult.PasswordError -> (loginResult as LoginResult.PasswordError).message
                            is LoginResult.GenericError -> (loginResult as LoginResult.GenericError).message
                            else -> ""
                        },
                        color = ErrorOrange,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth(0.7f)
                    )
                }
            }
            
            // 按钮区域 - 与ModeScreen保持一致的比例位置【Button Area - Maintains the same proportional position as ModeScreen】
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.dp)
                    .wrapContentHeight(Alignment.Bottom)
//                    .offset(y = (-252).dp) // 将原来的-180dp改为-252dp，向上移动约一个按钮高度【Changed from -180dp to -252dp, moved up by about one button height】
            ) {
                // 登录按钮【Login Button】
                Button(
                    onClick = { 
                        viewModel.login(username, password)
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    enabled = loginResult !is LoginResult.Loading
                ) {
                    if (loginResult is LoginResult.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = "Login",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 注册按钮【Register Button】
                OutlinedButton(
                    onClick = { 
                        // 导航到注册页面【Navigate to register page】
                        navController.navigate(route = com.example.navigationsolution.RegisterScreenRoute)
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    enabled = loginResult !is LoginResult.Loading
                ) {
                    Text(
                        text = "Register",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}