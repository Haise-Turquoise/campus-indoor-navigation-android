package com.example.navigationsolution.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * SessionManager基础功能测试【SessionManager Basic Functionality Test】
 * - 测试初始状态【- Test initial state】
 * - 测试访客模式设置【- Test visitor mode setting】
 * - 测试状态流一致性【- Test state flow consistency】
 */
@RunWith(AndroidJUnit4::class)
class SessionManagerTest {
    
    private lateinit var sessionManager: SessionManager
    
    @Before
    fun setUp() {
        // 测试环境准备【Test environment preparation】
        val context = ApplicationProvider.getApplicationContext<Context>()
        sessionManager = SessionManager.getInstance()
        sessionManager.initialize(context)
        sessionManager.logout()
    }
    
    @Test
    fun testInitialState() {
        // 验证初始状态 - 未登录【Verify initial state - Not logged in】
        assertFalse("初始状态应为未登录【Initial state should be logged out】", sessionManager.isLoggedIn())
        assertNull("初始状态应无用户数据【Initial state should have no user data】", sessionManager.getCurrentUser())
        
        // 验证状态流初始值【Verify initial state flow values】
        runBlocking {
            assertFalse("状态流应显示为未登录【State flow should show logged out】", sessionManager.isLoggedIn.first())
            assertNull("状态流应无用户数据【State flow should have no user data】", sessionManager.currentUser.first())
        }
    }
    
    @Test
    fun testVisitorMode() {
        // 设置访客模式【Set visitor mode】
        sessionManager.setUserSession("visitor", "visitor")
        
        // 验证常规API返回值【Verify regular API return values】
        assertTrue("访客模式应显示为已登录【Visitor mode should show as logged in】", sessionManager.isLoggedIn())
        val user = sessionManager.getCurrentUser()
        assertEquals("用户名应为visitor【Username should be visitor】", "visitor", user?.username)
        assertEquals("邮箱应为visitor【Email should be visitor】", "visitor", user?.email)
        
        // 验证状态流值一致性【Verify state flow value consistency】
        runBlocking {
            assertTrue("状态流应显示为已登录【State flow should show logged in】", sessionManager.isLoggedIn.first())
            val flowUser = sessionManager.currentUser.first()
            assertEquals("状态流用户名应为visitor【State flow username should be visitor】", "visitor", flowUser?.username)
            assertEquals("状态流邮箱应为visitor【State flow email should be visitor】", "visitor", flowUser?.email)
        }
    }
    
    @Test
    fun testPersistence() {
        // 设置访客模式【Set visitor mode】
        sessionManager.setUserSession("visitor", "visitor")
        
        // 重新初始化SessionManager模拟应用重启【Reinitialize SessionManager to simulate app restart】
        val context = ApplicationProvider.getApplicationContext<Context>()
        sessionManager.initialize(context)
        
        // 验证持久化【Verify persistence】
        val user = sessionManager.getCurrentUser()
        assertEquals("重新初始化后用户名应保持为visitor【Username should remain visitor after reinitialization】", "visitor", user?.username)
        assertEquals("重新初始化后邮箱应保持为visitor【Email should remain visitor after reinitialization】", "visitor", user?.email)
    }
}