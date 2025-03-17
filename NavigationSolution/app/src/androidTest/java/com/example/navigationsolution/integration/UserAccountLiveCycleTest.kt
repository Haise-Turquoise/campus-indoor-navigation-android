package com.example.navigationsolution.integration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.navigationsolution.service.SessionManager
import com.example.navigationsolution.ui.auth.InfoViewModel
import com.example.navigationsolution.ui.auth.LoginResult
import com.example.navigationsolution.ui.auth.LoginViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 用户账户生命周期集成测试【User Account Lifecycle Integration Test】
 * 
 * 测试用户处理分配流程的关键路径:【Test the key path of user assignment flow:】
 * - 登录 → 查看信息 → 登出【- Login → View Info → Logout】
 * 
 * 使用真实账户：mika/mika_PWD【Using real account: mika/mika_PWD】
 */
@RunWith(AndroidJUnit4::class)
class UserAccountLiveCycleTest {
    
    private lateinit var sessionManager: SessionManager
    private lateinit var loginViewModel: LoginViewModel
    private lateinit var infoViewModel: InfoViewModel
    
    // 测试账户凭据【Test account credentials】
    private val TEST_USERNAME = "mika"
    private val TEST_PASSWORD = "mika_PWD"
    
    @Before
    fun setUp() {
        // 准备测试环境【Prepare test environment】
        val context = ApplicationProvider.getApplicationContext<Context>()
        sessionManager = SessionManager.getInstance()
        sessionManager.initialize(context)
        sessionManager.logout() // 确保初始状态是登出状态【Ensure initial state is logged out】
        
        // 初始化ViewModels【Initialize ViewModels】
        loginViewModel = LoginViewModel()
    }
    
    @After
    fun tearDown() {
        // 清理测试环境 - 确保登出【Clean test environment - Ensure logout】
        sessionManager.logout()
    }
    
    @Test
    fun testLoginViewInfoLogout() = runBlocking {
        // 验证初始状态 - 未登录【Verify initial state - Not logged in】
        assertFalse("测试开始前应处于未登录状态【Should be in logged out state before test】", sessionManager.isLoggedIn())
        
        // 1. 登录过程【1. Login process】
        loginViewModel.login(TEST_USERNAME, TEST_PASSWORD)
        
        // 等待登录过程（最多等待5秒）【Wait for login process (maximum 5 seconds)】
        val maxWaitTime = 5000L
        val startTime = System.currentTimeMillis()
        var loginSuccess = false
        
        while (System.currentTimeMillis() - startTime < maxWaitTime) {
            val result = loginViewModel.loginResult.first()
            if (result is LoginResult.Success) {
                loginSuccess = true
                break
            } else if (result is LoginResult.GenericError || 
                      result is LoginResult.PasswordError || 
                      result is LoginResult.UsernameError) {
                fail("登录失败【Login failed】: ${result::class.simpleName}")
                break
            }
            delay(100) // 短暂延迟，避免紧密循环【Brief delay to avoid tight loop】
        }
        
        // 验证登录成功【Verify login success】
        assertTrue("登录应该成功【Login should succeed】", loginSuccess)
        assertTrue("SessionManager应显示已登录【SessionManager should show logged in】", sessionManager.isLoggedIn())
        
        val user = sessionManager.getCurrentUser()
        assertNotNull("应有用户会话【Should have user session】", user)
        assertEquals("用户名应为测试账户【Username should be the test account】", TEST_USERNAME, user?.username)
        
        // 2. 查看用户信息（通过InfoViewModel）【2. View user info (via InfoViewModel)】
        infoViewModel = InfoViewModel() // 创建InfoViewModel将自动从SessionManager获取状态【Creating InfoViewModel will automatically get state from SessionManager】
        
        // 验证InfoViewModel状态【Verify InfoViewModel state】
        assertEquals("InfoViewModel应显示正确用户名【InfoViewModel should display correct username】", TEST_USERNAME, infoViewModel.username.first())
        assertFalse("应处于普通用户模式【Should be in regular user mode】", infoViewModel.isVisitorMode.first())
        
        // 3. 登出【3. Logout】
        infoViewModel.logout()
        
        // 验证登出后状态【Verify state after logout】
        assertFalse("登出后应为未登录状态【Should be in logged out state after logout】", sessionManager.isLoggedIn())
        assertNull("登出后应无用户会话【Should have no user session after logout】", sessionManager.getCurrentUser())
    }
}
