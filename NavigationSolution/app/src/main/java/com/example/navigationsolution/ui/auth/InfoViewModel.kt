package com.example.navigationsolution.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.navigationsolution.service.SessionManager
import com.example.navigationsolution.service.SupabaseService
import kotlinx.coroutines.launch
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.from

// 删除账户结果的密封类【Sealed class for delete account results】
sealed class DeleteAccountResult {
    object Initial : DeleteAccountResult()
    object Loading : DeleteAccountResult()
    object Success : DeleteAccountResult()
    object DatabaseError : DeleteAccountResult()
    object VerificationFailed : DeleteAccountResult()
    data class Error(val message: String = "Verification code incorrect") : DeleteAccountResult()
}

class InfoViewModel : ViewModel() {
    
    private val TAG = "InfoViewModel" // 日志标签【Log tag】
    
    // 从SessionManager获取用户信息【Get user information from SessionManager】
    private val sessionManager = SessionManager.getInstance()
    private val currentUser = sessionManager.getCurrentUser()
    
    // 账户信息 - 从SessionManager获取【Account information - Retrieved from SessionManager】
    private val _username = MutableStateFlow(currentUser?.username ?: "unknown")
    val username: StateFlow<String> = _username.asStateFlow()
    
    private val _email = MutableStateFlow(currentUser?.email ?: "unknown")
    val email: StateFlow<String> = _email.asStateFlow()
    
    // 判断是否为访客模式【Determine if in visitor mode】
    private val _isVisitorMode = MutableStateFlow(_username.value == "visitor" && _email.value == "visitor")
    val isVisitorMode: StateFlow<Boolean> = _isVisitorMode.asStateFlow()
    
    // 删除状态【Deletion state】
    private val _showDeleteConfirmation = MutableStateFlow(false)
    val showDeleteConfirmation: StateFlow<Boolean> = _showDeleteConfirmation.asStateFlow()
    
    // 删除验证码【Deletion verification code】
    private val _verificationCode = MutableStateFlow("")
    val verificationCode: StateFlow<String> = _verificationCode.asStateFlow()
    
    // 删除结果状态【Deletion result state】
    private val _deleteResult = MutableStateFlow<DeleteAccountResult>(DeleteAccountResult.Initial)
    val deleteResult: StateFlow<DeleteAccountResult> = _deleteResult.asStateFlow()
    
    init {
        Log.d(TAG, "InfoViewModel初始化: 用户名=${_username.value}, 邮箱=${_email.value}, 访客模式=${_isVisitorMode.value}【InfoViewModel initialization: username=${_username.value}, email=${_email.value}, visitor mode=${_isVisitorMode.value}】")
    }
    
    // 显示删除确认【Show delete confirmation】
    fun showDeleteConfirmation() {
        _showDeleteConfirmation.value = true
        _deleteResult.value = DeleteAccountResult.Initial
        Log.d(TAG, "Delete confirmation shown")
    }
    
    // 更新验证码【Update verification code】
    fun updateVerificationCode(code: String) {
        _verificationCode.value = code
        // 如果有错误状态，重置为初始状态【If there is an error state, reset to initial state】
        if (_deleteResult.value is DeleteAccountResult.Error) {
            _deleteResult.value = DeleteAccountResult.Initial
        }
    }
    
    // 验证删除【Verify deletion】
    fun confirmDelete() {
        if (_verificationCode.value == "1PQ0") {
            // 验证成功 - 开始数据库删除操作【Verification successful - Start database deletion operation】
            Log.d(TAG, "Delete verification successful, starting database operation")
            _deleteResult.value = DeleteAccountResult.Loading
            
            // 使用协程执行数据库操作【Use coroutine to execute database operation】
            viewModelScope.launch {
                try {
                    // 事务安全保障 - 第一步：确保用户仍存在【Transaction safety guarantee - Step 1: Ensure user still exists】
                    val username = _username.value
                    
                    if (username == "unknown" || username == "visitor") {
                        Log.e(TAG, "Cannot delete account: Invalid username $username")
                        _deleteResult.value = DeleteAccountResult.Error("Invalid account state")
                        return@launch
                    }
                    
                    // 执行数据库删除操作【Execute database deletion operation】
                    Log.d(TAG, "Executing database delete for user: $username")
                    val deleteResult = SupabaseService.client
                        .from("users")
                        .delete {
                            filter {
                                eq("username", username)
                            }
                        }
                    
                    Log.d(TAG, "Deletion response: $deleteResult")
                    
                    // 事务安全保障 - 第二步：验证用户是否已被删除【Transaction safety guarantee - Step 2: Verify user has been deleted】
                    val verifyDeletion1 = SupabaseService.client
                        .from("users")
                        .select() {
                            filter { 
                                eq("username", username) 
                            }
                        }
                    
                    // 再次验证，确保用户确实被删除（双重检查）【Verify again, ensure user is indeed deleted (double check)】
                    val verifyDeletion2 = SupabaseService.client
                        .from("users")
                        .select() {
                            filter { 
                                eq("email", _email.value) 
                            }
                        }
                    
                    // 检查验证结果【Check verification results】
                    if (verifyDeletion1.data.toString() != "[]" || verifyDeletion2.data.toString() != "[]") {
                        // 用户未被完全删除【User has not been completely deleted】
                        Log.e(TAG, "Deletion verification failed: User still exists in database")
                        _deleteResult.value = DeleteAccountResult.DatabaseError
                    } else {
                        // 用户已成功删除【User has been successfully deleted】
                        Log.d(TAG, "User successfully deleted from database")
                        
                        // 事务安全保障 - 第三步：成功删除后清理本地会话【Transaction safety guarantee - Step 3: Clean local session after successful deletion】
                        _deleteResult.value = DeleteAccountResult.Success
                        logout() // 清理本地会话【Clean local session】
                    }
                } catch (e: Exception) {
                    // 处理异常【Handle exception】
                    Log.e(TAG, "Error during account deletion: ${e.message}", e)
                    _deleteResult.value = DeleteAccountResult.Error("Database error: ${e.message}")
                }
            }
        } else {
            // 验证码不匹配【Verification code does not match】
            Log.d(TAG, "Delete verification failed: ${_verificationCode.value}")
            _deleteResult.value = DeleteAccountResult.Error()
        }
    }
    
    // 取消删除确认【Cancel delete confirmation】
    fun cancelDeleteConfirmation() {
        _showDeleteConfirmation.value = false
        _verificationCode.value = ""
        _deleteResult.value = DeleteAccountResult.Initial
        Log.d(TAG, "Delete confirmation canceled")
    }
    
    // 登出 - 调用SessionManager.logout()【Logout - Call SessionManager.logout()】
    fun logout() {
        Log.d(TAG, "User logged out")
        sessionManager.logout()
    }
}