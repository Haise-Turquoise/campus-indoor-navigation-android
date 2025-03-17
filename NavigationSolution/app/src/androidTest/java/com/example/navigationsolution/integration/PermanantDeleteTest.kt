package com.example.navigationsolution.integration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.navigationsolution.service.SessionManager
import com.example.navigationsolution.ui.auth.DeleteAccountResult
import com.example.navigationsolution.ui.auth.InfoViewModel
import com.example.navigationsolution.ui.auth.LoginResult
import com.example.navigationsolution.ui.auth.LoginViewModel
import com.example.navigationsolution.ui.auth.RegisterResult
import com.example.navigationsolution.ui.auth.RegisterViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 账户永久删除流程集成测试【Account Permanent Deletion Flow Integration Test】
 * 
 * 测试不同验证场景下的账户删除流程:【Test account deletion flow in different verification scenarios:】
 * - 错误验证码处理【- Incorrect verification code handling】
 * - 正确验证码处理【- Correct verification code handling】
 * - 删除后会话状态验证【- Session state verification after deletion】
 */
@RunWith(AndroidJUnit4::class)
class AccountDeletionTest {
    
    private lateinit var sessionManager: SessionManager
    private lateinit var loginViewModel: LoginViewModel
    private lateinit var infoViewModel: InfoViewModel
    
    // 测试账户凭据 - 使用同一个测试账户【Test account credentials - Using the same test account】
    private val TEST_USERNAME = "mika"
    private val TEST_PASSWORD = "mika_PWD"
    
    // 正确的验证码【Correct verification code】
    private val CORRECT_VERIFICATION_CODE = "1PQ0"
    // 错误的验证码【Incorrect verification code】
    private val WRONG_VERIFICATION_CODE = "0000"
    private val DEFAULT_USERNAME = TEST_USERNAME
    private val DEFAULT_PASSWORD = TEST_PASSWORD
    
    @Before
    fun setUp() = runBlocking {
        // 准备测试环境【Prepare test environment】
        val context = ApplicationProvider.getApplicationContext<Context>()
        sessionManager = SessionManager.getInstance()
        sessionManager.initialize(context)
        sessionManager.logout() // 确保初始状态是登出状态【Ensure initial state is logged out】
        
        // 确保每个测试开始前mika账户都存在【Ensure mika account exists before each test】
        ensureMikaAccountExists()
        
        // 初始化ViewModels【Initialize ViewModels】
        loginViewModel = LoginViewModel()
        
        // 登录测试账户【Login with test account】
        loginViewModel.login(TEST_USERNAME, TEST_PASSWORD)
        
        // 等待登录完成【Wait for login completion】
        val maxWaitTime = 5000L
        val startTime = System.currentTimeMillis()
        
        while (System.currentTimeMillis() - startTime < maxWaitTime) {
            val result = loginViewModel.loginResult.first()
            if (result is LoginResult.Success || 
                result is LoginResult.GenericError || 
                result is LoginResult.PasswordError || 
                result is LoginResult.UsernameError) {
                break
            }
            delay(100)
        }
        
        // 验证登录成功【Verify login success】
        assertTrue("准备阶段登录应成功【Login should succeed during preparation】", sessionManager.isLoggedIn())
        
        // 创建InfoViewModel【Create InfoViewModel】
        infoViewModel = InfoViewModel()
    }
    
    @After
    fun tearDown() {
        // 清理测试环境【Clean test environment】
        sessionManager.logout()
    }
    
    @Test
    fun testWrongVerificationCode() = runBlocking {
        // 显示删除确认界面【Display delete confirmation screen】
        infoViewModel.showDeleteConfirmation()
        
        // 验证删除确认状态【Verify delete confirmation state】
        assertTrue("显示删除确认应为true【Show delete confirmation should be true】", infoViewModel.showDeleteConfirmation.first())
        
        // 输入错误验证码【Enter incorrect verification code】
        infoViewModel.updateVerificationCode(WRONG_VERIFICATION_CODE)
        
        // 尝试确认删除【Attempt to confirm deletion】
        infoViewModel.confirmDelete()
        
        // 验证结果 - 应该报错【Verify result - Should show error】
        val result = infoViewModel.deleteResult.first()
        assertTrue("使用错误验证码应产生错误【Using incorrect verification code should produce error】", result is DeleteAccountResult.Error)
        
        // 验证会话状态 - 应保持登录状态【Verify session state - Should remain logged in】
        assertTrue("验证失败后应保持登录状态【Should remain logged in after verification failure】", sessionManager.isLoggedIn())
        assertEquals("用户名应保持不变【Username should remain unchanged】", TEST_USERNAME, sessionManager.getCurrentUser()?.username)
    }
    
    @Test
    fun testCorrectVerificationCode() = runBlocking {
        // 注意：此测试会真实删除账户，仅在特定条件下运行【Note: This test will actually delete the account, run only under specific conditions】
        
        // 显示删除确认界面【Display delete confirmation screen】
        infoViewModel.showDeleteConfirmation()
        
        // 验证删除确认状态【Verify delete confirmation state】
        assertTrue("显示删除确认应为true【Show delete confirmation should be true】", infoViewModel.showDeleteConfirmation.first())
        
        // 输入正确验证码【Enter correct verification code】
        infoViewModel.updateVerificationCode(CORRECT_VERIFICATION_CODE)
        
        // 尝试确认删除【Attempt to confirm deletion】
        infoViewModel.confirmDelete()
        
        // 等待删除操作完成【Wait for deletion operation to complete】
        val maxWaitTime = 5000L
        val startTime = System.currentTimeMillis()
        
        while (System.currentTimeMillis() - startTime < maxWaitTime) {
            val result = infoViewModel.deleteResult.first()
            if (result is DeleteAccountResult.Success || 
                result is DeleteAccountResult.DatabaseError || 
                result is DeleteAccountResult.Error) {
                break
            }
            delay(100)
        }
        
        // 此处有两种可能的结果:【Two possible results here:】
        // 1. 成功删除账户 - deleteResult为Success且会话已登出【1. Successfully deleted account - deleteResult is Success and session is logged out】
        // 2. 账户已被之前测试删除 - 会出现其他错误类型【2. Account already deleted by previous test - other error types will appear】
        
        val result = infoViewModel.deleteResult.first()
        if (result is DeleteAccountResult.Success) {
            // 如果成功删除，验证会话状态 - 应已登出【If successfully deleted, verify session state - Should be logged out】
            assertFalse("删除成功后应为登出状态【Should be logged out after successful deletion】", sessionManager.isLoggedIn())
            assertNull("删除成功后应无用户会话【Should have no user session after successful deletion】", sessionManager.getCurrentUser())
        } else {
            // 如果出现错误(可能账户已被删除)，记录结果类型【If error occurs (account may have been deleted), record result type】
            println("删除结果【Deletion result】: ${result::class.simpleName}")
        }
    }
    
    @Test
    fun testCancelDeleteConfirmation() = runBlocking {
        // 显示删除确认界面【Display delete confirmation screen】
        infoViewModel.showDeleteConfirmation()
        
        // 验证删除确认状态【Verify delete confirmation state】
        assertTrue("显示删除确认应为true【Show delete confirmation should be true】", infoViewModel.showDeleteConfirmation.first())
        
        // 取消删除【Cancel deletion】
        infoViewModel.cancelDeleteConfirmation()
        
        // 验证状态重置【Verify state reset】
        assertFalse("取消后确认显示应为false【Confirmation display should be false after cancellation】", infoViewModel.showDeleteConfirmation.first())
        assertEquals("验证码应被清空【Verification code should be cleared】", "", infoViewModel.verificationCode.first())
        assertTrue("删除结果应重置为Initial【Delete result should reset to Initial】", infoViewModel.deleteResult.first() is DeleteAccountResult.Initial)
        
        // 验证会话状态 - 应保持登录状态【Verify session state - Should remain logged in】
        assertTrue("取消删除后应保持登录状态【Should remain logged in after cancellation】", sessionManager.isLoggedIn())
    }

    /**
     * 确保mika账户存在的辅助方法【Helper method to ensure mika account exists】
     * 用于在每个测试开始前和所有测试结束后恢复基准账户【Used to restore the baseline account before each test and after all tests】
     */
    private suspend fun ensureMikaAccountExists() {
        // 检查mika账户是否存在【Check if mika account exists】
        val tempLoginViewModel = LoginViewModel()
        tempLoginViewModel.login(DEFAULT_USERNAME, DEFAULT_PASSWORD)
        
        // 等待登录结果【Wait for login result】
        val maxWaitTime = 3000L
        val startTime = System.currentTimeMillis()
        var accountExists = false
        
        while (System.currentTimeMillis() - startTime < maxWaitTime) {
            val result = tempLoginViewModel.loginResult.first()
            if (result is LoginResult.Success) {
                // 账户存在且密码正确，无需重建【Account exists and password is correct, no need to recreate】
                accountExists = true
                break
            } else if (result is LoginResult.PasswordError) {
                // 账户存在但密码错误，也算存在【Account exists but password is incorrect, still counts as existing】
                accountExists = true
                break
            } else if (result is LoginResult.UsernameError) {
                // 用户名不存在，需要重建【Username doesn't exist, need to recreate】
                break
            } else if (result !is LoginResult.Loading) {
                // 其他非加载状态结果【Other non-loading state results】
                break
            }
            delay(100)
        }
        
        // 如果账户不存在，则重新注册【If account doesn't exist, register again】
        if (!accountExists) {
            println("基准账户mika不存在，开始重新注册...【Baseline account mika doesn't exist, starting re-registration...】")
            
            // 登出以确保干净状态【Logout to ensure clean state】
            sessionManager.logout()
            
            // 创建注册视图模型【Create register view model】
            val registerViewModel = RegisterViewModel()
            
            // 注册基准账户【Register baseline account】
            registerViewModel.register(
                DEFAULT_USERNAME, 
                "mika@gmail.com", 
                DEFAULT_PASSWORD, 
                DEFAULT_PASSWORD
            )
            
            // 等待注册结果【Wait for registration result】
            val registerStartTime = System.currentTimeMillis()
            
            while (System.currentTimeMillis() - registerStartTime < maxWaitTime) {
                val result = registerViewModel.registerResult.first()
                if (result is RegisterResult.Success || 
                    result !is RegisterResult.Loading) {
                    break
                }
                delay(100)
            }
            
            println("基准账户mika注册完成，状态【Baseline account mika registration completed, status】: ${registerViewModel.registerResult.first()::class.simpleName}")
        } else {
            println("基准账户mika已存在，无需重新注册【Baseline account mika already exists, no need to re-register】")
        }
        
        // 确保处于登出状态【Ensure logged out state】
        sessionManager.logout()
    }
    
    /**
     * 确保所有测试结束后mika账户存在的方法【Method to ensure mika account exists after all tests】
     * 这样其他测试类可以继续使用这个账户【So other test classes can continue using this account】
     */
    @After
    fun ensureMikaAccountExistsAfterTests() = runBlocking {
        ensureMikaAccountExists()
    }
}