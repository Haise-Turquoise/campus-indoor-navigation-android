package com.example.navigationsolution.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.navigationsolution.service.SupabaseService
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.from




// 验证结果的密封类【Sealed class for validation results】
sealed class LoginResult {
    object Initial : LoginResult()
    object Loading : LoginResult()
    object Success : LoginResult()
    data class UsernameError(val message: String = "Username does not exist") : LoginResult()
    data class PasswordError(val message: String = "Password is incorrect") : LoginResult()
    data class GenericError(val message: String = "Login failed. Please try again.") : LoginResult()
}

class LoginViewModel : ViewModel() {
    
    private val TAG = "LoginViewModel" // 日志标签【Log tag】
    
    // 验证结果状态【Validation result state】
    private val _loginResult = MutableStateFlow<LoginResult>(LoginResult.Initial)
    val loginResult: StateFlow<LoginResult> = _loginResult.asStateFlow()
    
    // 验证用户凭据【Validate user credentials】
    fun login(username: String, password: String) {
        // 输入验证【Input validation】
        if (username.isBlank() || password.isBlank()) {
            _loginResult.value = LoginResult.GenericError("Please fill in all fields")
            Log.d(TAG, "Login attempt failed: Empty username or password")
            return
        }
        
        // 设置加载状态【Set loading state】
        _loginResult.value = LoginResult.Loading
        Log.d(TAG, "Login attempt started for username: $username")
        
        // 在协程中进行验证【Perform validation in coroutine】
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting username validation...")
                
                // 检查用户名是否存在【Check if username exists】
                val usersWithUsername = SupabaseService.client
                    .from("users")
                    .select() {
                        filter {
                            eq("username", username)
                        }
                    }

                Log.d(TAG, "Username query executed, examining results...")
                Log.d(TAG, "Username query result data: ${usersWithUsername.data}")

                
                // 如果用户名不存在【If username doesn't exist】
                if (usersWithUsername.data.toString() == "[]") {
                    Log.d(TAG, "Username not found: $username")
                    _loginResult.value = LoginResult.UsernameError()
                    return@launch
                }
                
                Log.d(TAG, "Username found, now checking password...")

                // 检查密码是否匹配【Check if password matches】
                val usersWithCredentials = SupabaseService.client
                    .from("users")
                    .select() {
                        filter {
                            eq("username", username)
                            eq("pwd", password)
                        }
                    }
                
                Log.d(TAG, "Password query executed, examining results...")
                Log.d(TAG, "Password query result data: ${usersWithCredentials.data}")

                
                // 根据查询结果设置状态【Set state based on query results】
                if (usersWithCredentials.data.toString() == "[]") {
                    // 如果列表为空，表示密码验证失败【If the list is empty, password validation failed】
                    Log.d(TAG, "Password incorrect for user: $username - Empty credentials data")
                    _loginResult.value = LoginResult.PasswordError()
                } else {
                    // 列表非空，表示找到了匹配的用户名和密码【List not empty, found matching username and password】
                    Log.d(TAG, "Login successful for user: $username")
                    _loginResult.value = LoginResult.Success
                }
            } catch (e: Exception) {
                Log.e(TAG, "Login error: ${e.message}", e)
                _loginResult.value = LoginResult.GenericError(e.message ?: "Unknown error")
            }
        }
    }
    
    // 重置登录状态【Reset login state】
    fun resetLoginState() {
        _loginResult.value = LoginResult.Initial
        Log.d(TAG, "Login state reset")
    }
}