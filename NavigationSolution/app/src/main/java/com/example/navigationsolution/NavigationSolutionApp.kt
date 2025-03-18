package com.example.navigationsolution

import android.app.Application
import android.util.Log
import com.example.navigationsolution.service.SessionManager

class NavigationSolutionApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        Log.d("NavigationSolutionApp", "应用启动，初始化session manager【Application started, initializing session manager】")
        
        // 初始化SessionManager【Initialize SessionManager】
        SessionManager.getInstance().initialize(applicationContext)
    }
}