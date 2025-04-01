package com.example.navigationsolution.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 用户数据模型类【User Data Model Class】
 */
data class UserSession(
    val username: String,
    val email: String,
    val userId: String? = null,
    val building_search_history: List<String> = emptyList() // 添加搜索历史字段【Add search history field】
)

/**
 * 会话处理分配器 - 单例模式【Session Manager - Singleton Pattern】
 * 负责用户会话数据的存储、检索和清除操作【Responsible for storing, retrieving, and clearing user session data】
 */
class SessionManager private constructor() {
    private val TAG = "SessionManager"
    
    // 会话状态流【Session state flow】
    private val _currentUser = MutableStateFlow<UserSession?>(null)
    val currentUser: StateFlow<UserSession?> = _currentUser.asStateFlow()
    
    // 登录状态流【Login state flow】
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
    
    // SharedPreferences 对象【SharedPreferences object】
    private lateinit var prefs: SharedPreferences
    
    // SharedPreferences 键【SharedPreferences keys】
    private val PREF_NAME = "navigation_solution_prefs"
    private val KEY_USERNAME = "username"
    private val KEY_EMAIL = "email"
    private val KEY_USER_ID = "user_id"
    private val KEY_IS_LOGGED_IN = "is_logged_in"
    private val KEY_BUILDING_HISTORY = "building_history"
    
    /**
     * 初始化 SessionManager，从SharedPreferences加载会话数据【Initialize SessionManager, load session data from SharedPreferences】
     */
    fun initialize(context: Context) {
        Log.d(TAG, "初始化会话处理分配器【Initializing Session Manager】")
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        
        // 检查并加载持久化的会话数据【Check and load persisted session data】
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        if (isLoggedIn) {
            loadUserSession()
        }
    }
    
    /**
     * 设置当前用户会话【Set current user session】
     */
    fun setUserSession(username: String, email: String, userId: String? = null, buildingSearchHistory: List<String> = emptyList()) {
        Log.d(TAG, "设置用户会话【Setting user session】: username=$username, email=$email")
        
        val userSession = UserSession(username, email, userId, buildingSearchHistory)
        _currentUser.value = userSession
        _isLoggedIn.value = true
        
        // 保存到SharedPreferences【Save to SharedPreferences】
        saveUserSession(userSession)
    }
    
    /**
     * 检查用户是否已登录【Check if user is logged in】
     */
    fun isLoggedIn(): Boolean {
        val loggedIn = _isLoggedIn.value
        Log.d(TAG, "检查用户登录状态【Checking user login status】: $loggedIn")
        return loggedIn
    }
    
    /**
     * 获取当前用户信息【Get current user information】
     */
    fun getCurrentUser(): UserSession? {
        val user = _currentUser.value
        Log.d(TAG, "获取当前用户【Getting current user】: ${user?.username ?: "未登录【Not logged in】"}")
        return user
    }
    
    /**
     * 退出登录，清除会话数据【Logout, clear session data】
     */
    fun logout() {
        Log.d(TAG, "用户退出登录【User logout】")
        
        // 记录旧用户信息用于日志【Record old user info for logging】
        val oldUsername = _currentUser.value?.username
        
        // 完全重置用户会话【Completely reset user session】
        _currentUser.value = null
        _isLoggedIn.value = false
        
        Log.d(TAG, "用户 $oldUsername 会话数据已清除【User $oldUsername session data cleared】")
        
        // 清除SharedPreferences中的会话数据【Clear session data in SharedPreferences】
        prefs.edit().apply {
            clear()
            apply()
        }
    }
    
    /**
     * 从SharedPreferences加载会话数据【Load session data from SharedPreferences】
     */
    private fun loadUserSession() {
        val username = prefs.getString(KEY_USERNAME, null)
        val email = prefs.getString(KEY_EMAIL, null)
        val userId = prefs.getString(KEY_USER_ID, null)
        val historyJson = prefs.getString(KEY_BUILDING_HISTORY, "[]")
        val buildingHistory = try {
            // 简单解析JSON数组【Simple JSON array parsing】
            historyJson?.trim()
                ?.removePrefix("[")
                ?.removeSuffix("]")
                ?.takeIf { it.isNotEmpty() }
                ?.split(",")
                ?.map { it.trim().removeSurrounding("\"") }
                ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "解析搜索历史出错【Error parsing search history】", e)
            emptyList()
        }
        
        if (username != null && email != null) {
            Log.d(TAG, "从持久化存储加载会话【Loading session from persistent storage】: username=$username, email=$email")
            _currentUser.value = UserSession(username, email, userId, buildingHistory)
            _isLoggedIn.value = true
        } else {
            Log.d(TAG, "无法从持久化存储加载有效会话【Unable to load valid session from persistent storage】")
            _isLoggedIn.value = false
        }
    }
    
    /**
     * 保存会话数据到SharedPreferences【Save session data to SharedPreferences】
     */
    private fun saveUserSession(userSession: UserSession) {
        Log.d(TAG, "保存会话到持久化存储【Saving session to persistent storage】")
        
        // 创建一个默认的空JSON数组字符串【Create a default empty JSON array string】
        val historyJson = if (userSession.building_search_history.isNotEmpty()) {
            userSession.building_search_history.joinToString(",", "[", "]") { "\"$it\"" }
        } else {
            "[]"  // 默认空数组【Default empty array】
        }
        
        prefs.edit().apply {
            putString(KEY_USERNAME, userSession.username)
            putString(KEY_BUILDING_HISTORY, historyJson)
            putString(KEY_EMAIL, userSession.email)
            userSession.userId?.let { putString(KEY_USER_ID, it) }
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }
    
    companion object {
        @Volatile
        private var instance: SessionManager? = null
        
        fun getInstance(): SessionManager {
            return instance ?: synchronized(this) {
                instance ?: SessionManager().also { instance = it }
            }
        }
    }
}