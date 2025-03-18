package com.example.navigationsolution.ui.auth

import android.util.Log
import android.widget.Toast
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
import androidx.compose.foundation.layout.size
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
import com.example.navigationsolution.LoginScreenRoute
import com.example.navigationsolution.RegisterScreenRoute
import com.example.navigationsolution.InfoScreenRoute

@Composable
fun RegisterScreen(
    navController: NavController,
    viewModel: RegisterViewModel = viewModel()
) {
    // 状态变量【State variables】
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    // 收集注册结果状态【Collect registration result state】
    val registerResult by viewModel.registerResult.collectAsState()
    
    // 处理注册成功【Handle successful registration】
    LaunchedEffect(registerResult) {
        if (registerResult is RegisterResult.Success) {
            // 显示注册成功的Toast提示【Show toast for successful registration】
            Toast.makeText(
                navController.context,
                "Sign up Success, Please Sign in",
                Toast.LENGTH_SHORT
            ).show()
            
            navController.navigate(route = LoginScreenRoute) {
                // 清除返回栈上的注册页面【Clear registration page from back stack】
                popUpTo(RegisterScreenRoute) {
                    inclusive = true
                }
            }
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 顶部标题区域【Top Title Area】
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 35.dp)  // 调整顶部内边距，将标题放在页面顶部与第一个输入框顶部之间的一半位置【Adjust top padding to place title halfway between page top and first input field】
            ) {
                // 删除应用Logo【App Logo removed】
                
                // 应用标题【App Title】
                Text(
                    text = "Pathfinder",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 副标题【Subtitle】
                Text(
                    text = "Create an account",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            // 输入表单区域 - 放置在顶部和按钮区域之间【Input Form Area - Placed between top and button areas】
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .offset(y = (-140).dp) // 进一步上移输入区域位置，确保与按钮之间有足够空间【Move input area further up to ensure enough space between it and buttons】
            ) {
                // 用户名输入框【Username Input Field】
                OutlinedTextField(
                    value = username,
                    onValueChange = { 
                        username = it
                        // 重置错误状态【Reset error state】
                        if (registerResult !is RegisterResult.Initial) {
                            viewModel.resetRegisterState()
                        }
                    },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .padding(vertical = 6.dp)
                )
                
                // 邮箱输入框【Email Input Field】
                OutlinedTextField(
                    value = email,
                    onValueChange = { 
                        email = it
                        // 重置错误状态【Reset error state】
                        if (registerResult !is RegisterResult.Initial) {
                            viewModel.resetRegisterState()
                        }
                    },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .padding(vertical = 6.dp)
                )
                
                // 密码输入框【Password Input Field】
                OutlinedTextField(
                    value = password,
                    onValueChange = { 
                        password = it
                        // 重置错误状态【Reset error state】
                        if (registerResult !is RegisterResult.Initial) {
                            viewModel.resetRegisterState()
                        }
                    },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .padding(vertical = 6.dp)
                )
                
                // 确认密码输入框【Confirm Password Input Field】
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { 
                        confirmPassword = it
                        // 重置错误状态【Reset error state】
                        if (registerResult !is RegisterResult.Initial) {
                            viewModel.resetRegisterState()
                        }
                    },
                    label = { Text("Confirm Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .padding(vertical = 6.dp)
                )
                
                // 错误信息显示区域【Error Message Display Area】
                if (registerResult is RegisterResult.UsernameError || 
                    registerResult is RegisterResult.PasswordError || 
                    registerResult is RegisterResult.EmailError ||
                    registerResult is RegisterResult.FieldsError ||
                    registerResult is RegisterResult.GenericError) {
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = when (registerResult) {
                            is RegisterResult.UsernameError -> (registerResult as RegisterResult.UsernameError).message
                            is RegisterResult.PasswordError -> (registerResult as RegisterResult.PasswordError).message
                            is RegisterResult.EmailError -> (registerResult as RegisterResult.EmailError).message
                            is RegisterResult.FieldsError -> (registerResult as RegisterResult.FieldsError).message
                            is RegisterResult.GenericError -> (registerResult as RegisterResult.GenericError).message
                            else -> ""
                        },
                        color = com.example.navigationsolution.ui.auth.ErrorOrange,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth(0.7f)
                    )
                }
                
                // 添加底部空间确保表单不会被按钮覆盖【Add bottom space to ensure form is not covered by buttons】
                Spacer(modifier = Modifier.height(50.dp)) // 增加底部间距【Increase bottom spacing】
            }
            
            // 按钮区域 - 与LoginScreen保持一致的比例位置【Button Area - Maintains the same proportional position as LoginScreen】
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 40.dp)
                    .wrapContentHeight(Alignment.Bottom)
                    .offset(y = (-252).dp) // 向上移动约一个按钮高度【Moved up by about one button height】
            ) {
                // 注册按钮【Sign Up Button】
                Button(
                    onClick = { 
                        Log.d("RegisterScreen", "Sign Up button clicked with username: $username, email: $email")
                        viewModel.register(username, email, password, confirmPassword)
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    enabled = registerResult !is RegisterResult.Loading
                ) {
                    if (registerResult is RegisterResult.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = "Sign Up",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 返回登录按钮【Return to Login Button】
                OutlinedButton(
                    onClick = { 
                        navController.navigate(route = LoginScreenRoute)
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    enabled = registerResult !is RegisterResult.Loading
                ) {
                    Text(
                        text = "Return to Login",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
} 