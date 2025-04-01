package com.example.navigationsolution.viewmodels

import MapRepository.roomExists
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.navigationsolution.NO_PATH
import com.example.navigationsolution.service.SessionManager
import com.example.navigationsolution.service.SupabaseService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import io.github.jan.supabase.postgrest.from

class IndoorViewModel: ViewModel() {
    // 日志标签【Log tag】
    private val TAG = "IndoorViewModel"
    
    // SessionManager实例【SessionManager instance】
    private val sessionManager = SessionManager.getInstance()
    
    var from: Int = NO_PATH
    var to: Int = NO_PATH
    var buildingFrom: Int = 1
    var buildingTo: Int = 1
    var floor = MutableLiveData(0)
    var maxFloor: Int = 0
    var compassEnabled = MutableLiveData(true)
    var markedPlan: List<Bitmap> = listOf()

    // 搜索历史相关状态【Search history related states】
    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()
    
    // 最大历史记录数【Maximum history records】
    private val MAX_HISTORY_SIZE = 10

    var liveFloor: LiveData<Int> = floor
    var liveCompassEnabled: LiveData<Boolean> = compassEnabled

    private val _altColours = MutableLiveData(false)
    val altColours: LiveData<Boolean> = _altColours

    init {
        // 初始化时加载搜索历史【Load search history on initialization】
        loadSearchHistory()
        
        // 监听用户登录状态变化【Monitor user login status changes】
        viewModelScope.launch {
            sessionManager.isLoggedIn.collectLatest { isLoggedIn ->
                Log.d(TAG, "用户登录状态变化：$isLoggedIn【User login status change: $isLoggedIn】")
                if (isLoggedIn) {
                    // 用户登录，加载搜索历史【User logged in, load search history】
                    loadSearchHistory()
                } else {
                    // 用户登出，清除搜索历史【User logged out, clear search history】
                    _searchHistory.value = emptyList()
                    Log.d(TAG, "用户已登出，清除搜索历史【User logged out, clearing search history】")
                }
            }
        }
        
        // 监听用户数据变化【Monitor user data changes】
        viewModelScope.launch {
            sessionManager.currentUser.collectLatest { user ->
                Log.d(TAG, "用户数据变化：${user?.username ?: "null"}【User data change: ${user?.username ?: "null"}】")
                if (user != null) {
                    // 用户数据更新，重新加载搜索历史【User data updated, reload search history】
                    loadSearchHistory()
                }
            }
        }
    }

    fun swapColours() {
        _altColours.value = !_altColours.value!!
    }

    private val _textScale = MutableLiveData(1f)
    val textScale: LiveData<Float> = _textScale

    fun updateTextScale(scale: Float) {
        _textScale.value = scale
    }

    // 加载搜索历史【Load search history】
    private fun loadSearchHistory() {
        Log.d(TAG, "加载搜索历史【Loading search history】")
        val user = sessionManager.getCurrentUser()
        
        if (user != null && sessionManager.isLoggedIn()) {
            // 先清除当前历史记录，防止跨账号污染【First clear current history to prevent cross-account contamination】
            _searchHistory.value = emptyList()
            
            // 加载当前用户的搜索历史【Load current user's search history】
            _searchHistory.value = user.building_search_history
            Log.d(TAG, "已从本地加载 ${_searchHistory.value.size} 条搜索历史【Loaded ${_searchHistory.value.size} search history entries from local】")
            
            // 然后从数据库获取最新数据【Then get latest data from database】
            viewModelScope.launch {
                try {
                    val username = user.username
                    Log.d(TAG, "从数据库加载用户 $username 的搜索历史【Loading search history for user $username from database】")
                    
                    // 从数据库查询最新用户数据【Query latest user data from database】
                    val userData = SupabaseService.client
                        .from("users")
                        .select() {
                            filter {
                                eq("username", username)
                            }
                        }
                        .decodeList<com.example.navigationsolution.data.UserData>()
                        .firstOrNull()
                    
                    if (userData != null) {
                        // 确保当前登录用户与加载的用户一致【Ensure current logged-in user matches loaded user】
                        val currentUser = sessionManager.getCurrentUser()
                        if (currentUser != null && currentUser.username == username) {
                            // 更新搜索历史状态【Update search history state】
                            _searchHistory.value = userData.building_search_history
                            
                            // 同步到SessionManager【Sync to SessionManager】
                            sessionManager.setUserSession(
                                username = user.username,
                                email = user.email,
                                userId = user.userId,
                                buildingSearchHistory = userData.building_search_history
                            )
                            
                            Log.d(TAG, "已从数据库加载 ${_searchHistory.value.size} 条搜索历史【Loaded ${_searchHistory.value.size} search history entries from database】")
                        } else {
                            Log.d(TAG, "用户已切换，取消加载搜索历史【User has switched, canceling history loading】")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "从数据库加载搜索历史失败【Failed to load search history from database】: ${e.message}", e)
                }
            }
        } else {
            _searchHistory.value = emptyList()
            Log.d(TAG, "用户未登录，无搜索历史【User not logged in, no search history】")
        }
    }
    
    // 添加搜索记录【Add search record】
    fun addSearchRecord(buildingCode: String, roomNumber: String) {
        Log.d(TAG, "添加搜索记录【Adding search record】: $buildingCode-$roomNumber")
        if (!sessionManager.isLoggedIn()) {
            Log.d(TAG, "用户未登录，不保存搜索历史【User not logged in, not saving search history】")
            return
        }
        
        // 仅保存房间号，去掉建筑物代码前缀【Only save room number, without building code prefix】
        val searchEntry = roomNumber
        
        // 获取当前历史记录并添加新记录【Get current history and add new record】
        val currentHistory = _searchHistory.value.toMutableList()
        
        // 如果已存在相同记录，先移除【If the same record exists, remove it first】
        currentHistory.remove(searchEntry)
        
        // 添加到列表开头【Add to the beginning of the list】
        currentHistory.add(0, searchEntry)
        
        // 如果超过最大限制，删除最旧的记录【If exceeds the limit, remove the oldest record】
        if (currentHistory.size > MAX_HISTORY_SIZE) {
            currentHistory.removeAt(currentHistory.size - 1)
        }
        
        // 更新状态【Update state】
        _searchHistory.value = currentHistory
        
        // 更新会话管理器【Update session manager】
        saveSearchHistoryToSession()
    }
    
    // 清除搜索历史【Clear search history】
    fun clearSearchHistory() {
        Log.d(TAG, "清除搜索历史【Clearing search history】")
        _searchHistory.value = emptyList()
        saveSearchHistoryToSession()
    }
    
    // 保存搜索历史到会话【Save search history to session】
    private fun saveSearchHistoryToSession() {
        Log.d(TAG, "保存搜索历史到会话【Saving search history to session】")
        val currentUser = sessionManager.getCurrentUser()
        if (currentUser != null) {
            // 本地保存【Local saving】
            sessionManager.setUserSession(
                username = currentUser.username,
                email = currentUser.email,
                userId = currentUser.userId,
                buildingSearchHistory = _searchHistory.value
            )
            
            // 数据库同步【Database synchronization】
            viewModelScope.launch {
                try {
                    // 获取用户名【Get username】
                    val username = currentUser.username
                    
                    // 直接使用List<String>作为更新数据，让Supabase SDK处理JSON转换
                    // Directly use List<String> as update data, let Supabase SDK handle JSON conversion
                    SupabaseService.client
                        .from("users")
                        .update(mapOf("building_search_history" to _searchHistory.value)) {
                            filter {
                                eq("username", username)
                            }
                        }
                    Log.d(TAG, "搜索历史已同步到数据库【Search history synchronized to database】: ${_searchHistory.value}")
                } catch (e: Exception) {
                    Log.e(TAG, "同步搜索历史到数据库失败【Failed to sync search history to database】: ${e.message}", e)
                }
            }
        } else {
            Log.d(TAG, "用户未登录，无法保存搜索历史【User not logged in, cannot save search history】")
        }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun updatePath(newFrom: Int = from,
                   newTo: Int = to,
                   newBuildingFrom: Int = buildingFrom,
                   newBuildingTo: Int = buildingTo,
                   newMaxFloor: Int = maxFloor) {
        from = newFrom
        to = newTo
        buildingFrom = newBuildingFrom
        buildingTo = newBuildingTo
        maxFloor = newMaxFloor

        val inv: Boolean = altColours.value == true

        if (from != to || buildingFrom != buildingTo) {
            markedPlan = MapRepository.getMarkedPlan(buildingFrom, from, buildingTo, to, inv)
        } else {
            markedPlan = MapRepository.getMarkedPlan(buildingFrom, from, to, inv)
        }

        maxFloor = markedPlan.size - 1
        
        // 如果是有效房间号，保存到搜索历史【If it's a valid room number, save to search history】
        if (from != NO_PATH && to != NO_PATH) {
            // 保存起点位置到搜索历史【Save starting point to search history】
            addSearchRecord("", from.toString())
            
            // 保存目标位置到搜索历史【Save destination to search history】
            addSearchRecord("", to.toString())
        }
    }

    fun incrFloor() {
        floor.value = min(maxFloor, floor.value?.plus(1) as Int)
    }

    fun decrFloor() {
        floor.value = max(0, floor.value?.minus(1) as Int)
    }

    fun setFloor(newFloor: Int) {
        floor.value = newFloor
    }

    fun toggleCompass() {
        compassEnabled.value = !compassEnabled.value!!
    }

    fun getBuildingID(name: String): Int {
        if (name == "E7") {
            return 1
        } else if (name == "E6") {
            return 2
        } else {
            return 1
        }
    }
    
    // 根据建筑物ID获取建筑物名称【Get building name based on building ID】
    fun getBuildingName(buildingId: Int): String {
        return when (buildingId) {
            1 -> "E7"
            2 -> "E6"
            else -> "E7"
        }
    }

    /*fun updateBuilding(newBuildingCode: String) {
        from = NO_PATH
        to = NO_PATH
        buildingId = getBuildingResourceId(newBuildingCode)
        setFloor(0)
    }

    private fun getBuildingResourceId(buildingCode: String): Int {
        return when (buildingCode) {
            "MC" -> R.drawable.mc_map // we can add actual buildings when we get
            "DC" -> R.drawable.dc_map
            "SLC" -> R.drawable.slc_map
            "E5" -> R.drawable.e5_map
            "E7" -> R.drawable.e7f1.png
            "QNC" -> R.drawable.qnc_map
            else -> R.drawable.default_map
        }
    }*/
}