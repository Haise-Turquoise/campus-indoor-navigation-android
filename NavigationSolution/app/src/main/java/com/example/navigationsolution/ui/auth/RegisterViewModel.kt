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

// 注册结果的密封类【Sealed class for registration results】
sealed class RegisterResult {
    object Initial : RegisterResult()
    object Loading : RegisterResult()
    object Success : RegisterResult()
    data class UsernameError(val message: String = "Username already exists") : RegisterResult()
    data class EmailError(val message: String = "Email already exists") : RegisterResult()
    data class PasswordError(val message: String = "Passwords do not match") : RegisterResult()
    data class FieldsError(val message: String = "Please fill in all fields") : RegisterResult()
    data class GenericError(val message: String = "Registration failed. Please try again.") : RegisterResult()
}

class RegisterViewModel : ViewModel() {
    
    private val TAG = "RegisterViewModel" // 日志标签【Log tag】
    
    // 注册结果状态【Registration result state】
    private val _registerResult = MutableStateFlow<RegisterResult>(RegisterResult.Initial)
    val registerResult: StateFlow<RegisterResult> = _registerResult.asStateFlow()
    
    // 注册新用户【Register new user】
    fun register(username: String, email: String, password: String, confirmPassword: String) {
        Log.d(TAG, "Starting registration process for username: $username, email: $email")
        
        // 重置为初始状态防止之前的错误影响【Reset to initial state to prevent previous errors from affecting the current registration】
        _registerResult.value = RegisterResult.Initial
        
        // 1- 检查username和email是否完全输入【Check if username and email are fully entered】
        if (username.isBlank() || email.isBlank()) {
            _registerResult.value = RegisterResult.FieldsError("Username and email need to be entered")
            Log.d(TAG, "Registration failed: Username or email is blank")
            return
        }
        
        // 2- 检查password和confirm password是否完全输入【Check if password and confirm password are fully entered】
        if (password.isBlank() || confirmPassword.isBlank()) {
            _registerResult.value = RegisterResult.FieldsError("Password and confirm password need to be entered")
            Log.d(TAG, "Registration failed: Password or confirm password is blank")
            return
        }
        
        // 设置加载状态【Set loading state】
        _registerResult.value = RegisterResult.Loading
        
        // 在协程中进行验证和注册【Perform validation and registration in coroutine】
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting database validation for username: $username and email: $email")
                
                // 3- 检查username和email是否已存在于数据库【Check if username and email already exist in the database】
                val existingUsernames = SupabaseService.client
                    .from("users")
                    .select() {
                        filter {
                            eq("username", username)
                        }
                    }
                
                Log.d(TAG, "Username query result: ${existingUsernames.data}")
                
                // 检查用户名是否已存在【Check if username already exists】
                if (existingUsernames.data.toString() != "[]") {
                    Log.d(TAG, "Username already exists: $username")
                    _registerResult.value = RegisterResult.UsernameError("Username exists, please change")
                    return@launch
                }
                
                // 检查邮箱是否已存在【Check if email already exists】
                val existingEmails = SupabaseService.client
                    .from("users")
                    .select() {
                        filter {
                            eq("email", email)
                        }
                    }
                
                Log.d(TAG, "Email query result: ${existingEmails.data}")
                
                if (existingEmails.data.toString() != "[]") {
                    Log.d(TAG, "Email already exists: $email")
                    _registerResult.value = RegisterResult.EmailError("Email exists, please change")
                    return@launch
                }
                
                // 4- 检查密码长度和一致性【Check password length and consistency】
                if (password != confirmPassword || password.length < 6) {
                    Log.d(TAG, "Password validation failed: Either passwords don't match or length < 6")
                    _registerResult.value = RegisterResult.PasswordError("Password and confirm password need to be same and size over 6 characters")
                    return@launch
                }
                
                // 5- 数据库写入操作【Database write operation】
                // 创建用户数据对象【Create user data object】
                val userData = mapOf(
                    "username" to username,
                    "email" to email,
                    "pwd" to password
                )
                
                Log.d(TAG, "Attempting to insert new user: $username")
                
                // 插入数据到users表【Insert data into users table】
                val insertResult = SupabaseService.client
                    .from("users")
                    .insert(userData) {
                        select()
                    }
                
                Log.d(TAG, "Insert result: ${insertResult.data}")
                
                // 检查插入是否成功【Check if insertion was successful】
                if (insertResult.data.toString() != "[]") {
                    Log.d(TAG, "Registration successful for user: $username")
                    Log.d(TAG, "User registered successfully, navigating back to login screen")
                    _registerResult.value = RegisterResult.Success
                } else {
                    Log.d(TAG, "Registration failed: Empty insert result")
                    _registerResult.value = RegisterResult.GenericError("Failed to create account. Please try again.")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Registration error: ${e.message}", e)
                _registerResult.value = RegisterResult.GenericError(e.message ?: "Unknown error")
            }
        }
    }
    
    // 重置注册状态【Reset registration state】
    fun resetRegisterState() {
        _registerResult.value = RegisterResult.Initial
        Log.d(TAG, "Registration state reset")
    }
} 