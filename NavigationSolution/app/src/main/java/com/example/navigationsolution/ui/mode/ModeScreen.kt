package com.example.navigationsolution.ui.mode

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.navigationsolution.IndoorMapScreen
import com.example.navigationsolution.LoginScreenRoute
import com.example.navigationsolution.OpeningScreen
import com.example.navigationsolution.R
import com.example.navigationsolution.service.SessionManager
import android.util.Log

@Composable
fun ModeScreen(navController: NavController) {
    // 记录日志标签
    val TAG = "ModeScreen"
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 顶部标题区域 - 使用Box作为父容器，并将标题区域放在顶部【Top Title Area - Using Box as parent container and placing the title area at the top】
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp)
            ) {
                // 应用Logo【App Logo】
                Image(
                    painter = painterResource(id = R.drawable.uwlogo),
                    contentDescription = "应用Logo【App Logo】",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .height(120.dp)
                        .fillMaxWidth(0.8f)
                        .clip(shape = RoundedCornerShape(8.dp))
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
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
                    text = "Please Select Login Mode",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            // 模式选择按钮区域 - 放置在屏幕下方40%的位置【Mode Selection Button Area - Placed at 40% from the bottom of the screen】
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 40.dp)
                    .wrapContentHeight(Alignment.Bottom)
                    .offset(y = (-180).dp) // 向上偏移，位于屏幕约下方40%位置【Offset upward, positioned at about 40% from the bottom of the screen】
            ) {
                // 用户模式按钮【User Mode Button】
                Button(
                    onClick = { 
                        // 导航到登录页面【Navigate to login page】
                        navController.navigate(route = LoginScreenRoute)
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(60.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "User Mode",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 访客模式按钮【Visitor Mode Button】
                OutlinedButton(
                    onClick = { 
                        // 设置访客模式会话数据
                        Log.d(TAG, "设置visitor用户会话数据")
                        SessionManager.getInstance().setUserSession("visitor", "visitor")
                        
                        // 直接导航到地图页面【Navigate directly to map page】
                        navController.navigate(route = IndoorMapScreen)
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(60.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Visitor Mode",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
