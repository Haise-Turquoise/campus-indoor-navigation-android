package com.example.navigationsolution.ui.auth

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Card
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.navigationsolution.ModeSelectionScreen
import com.example.navigationsolution.ui.auth.ErrorOrange



@Composable
fun InfoScreen(
    navController: NavController,
    viewModel: InfoViewModel
) {
    // 收集状态【Collect states】
    val username by viewModel.username.collectAsState()
    val email by viewModel.email.collectAsState()
    val isVisitorMode by viewModel.isVisitorMode.collectAsState()
    val showDeleteConfirmation by viewModel.showDeleteConfirmation.collectAsState()
    val verificationCode by viewModel.verificationCode.collectAsState()
    val deleteResult by viewModel.deleteResult.collectAsState()
    
    // 处理删除成功【Handle successful deletion】
    LaunchedEffect(deleteResult) {
        if (deleteResult is DeleteAccountResult.Success) {
            // 显示删除成功的Toast提示【Show toast for successful deletion】
            Toast.makeText(
                navController.context,
                "Account successfully deleted",
                Toast.LENGTH_SHORT
            ).show()
            
            // 导航回到模式选择界面【Navigate back to mode selection screen】
            navController.navigate(route = ModeSelectionScreen) {
                // 清除返回栈【Clear back stack】
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
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 顶部标题区域【Top title area】
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp)
            ) {
                // 应用标题【App title】
                Text(
                    text = "Pathfinder",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 副标题【Subtitle】
                Text(
                    text = "Account Information",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            // 信息展示区域【Information display area】
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .offset(y = (-100).dp)
            ) {
                // 用户信息卡片【User information card】
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .padding(vertical = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 用户名信息【Username information】
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Username:",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = username,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        
                        // 邮箱信息【Email information】
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Email:",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = email,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        
                        // 密码信息【Password information】
//                        Row(
//                            modifier = Modifier.fillMaxWidth(),
//                            horizontalArrangement = Arrangement.SpaceBetween
//                        ) {
//                            Text(
//                                text = "Password:",
//                                style = MaterialTheme.typography.bodyLarge,
//                                fontWeight = FontWeight.Bold
//                            )
//                            Text(
//                                text = password,
//                                style = MaterialTheme.typography.bodyLarge
//                            )
//                        }
                    }
                }
            }
            
            // 使用固定位置的Column来放置按钮区域【Using fixed position Column to place button area】
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (160).dp)
                    .fillMaxWidth()
            ) {
                // 主按钮区域 - 这部分位置保持固定【Main button area - This part maintains fixed position】
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 登出按钮【Logout button】
                    Button(
                        onClick = { 
                            viewModel.logout()
                            navController.navigate(route = ModeSelectionScreen) {
                                popUpTo(navController.graph.startDestinationId) {
                                    inclusive = true
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "Log Out",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 删除账户按钮 - 只有在非访客模式下显示【Delete account button - Only displayed in non-visitor mode】
                    if (!isVisitorMode) {
                        OutlinedButton(
                            onClick = { 
                                if (showDeleteConfirmation) {
                                    viewModel.confirmDelete()
                                } else {
                                    viewModel.showDeleteConfirmation()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(
                                text = if (showDeleteConfirmation) "Confirm Delete" else "Permanent Delete",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                // 确认删除区域 - 这部分仅在确认时显示，向下生长【Confirmation deletion area - This part is only displayed during confirmation, grows downward】
                if (showDeleteConfirmation && !isVisitorMode) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 取消按钮【Cancel button】
                        OutlinedButton(
                            onClick = { viewModel.cancelDeleteConfirmation() },
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = "Cancel",
                                fontSize = 16.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 验证码输入框【Verification code input field】
                        OutlinedTextField(
                            value = verificationCode,
                            onValueChange = { viewModel.updateVerificationCode(it) },
                            label = { Text("Verification Code") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // 提示文本以橙色显示【Hint text displayed in orange】
                        Text(
                            text = "Enter 1PQ0 to confirm",
                            color = ErrorOrange,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth(0.7f)
                        )
                        
                        // 错误信息【Error message】
                        if (deleteResult is DeleteAccountResult.Error) {
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = (deleteResult as DeleteAccountResult.Error).message,
                                color = ErrorOrange,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.fillMaxWidth(0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}