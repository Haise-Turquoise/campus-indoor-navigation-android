package com.example.navigationsolution.ui.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.navigationsolution.service.SessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


/**
 * InfoViewModel单一功能点测试【InfoViewModel Single Functionality Point Test】
 * - 测试访客模式检测逻辑【- Test visitor mode detection logic】
 * - 测试状态流正确性【- Test state flow correctness】
 * - 测试基本UI状态控制【- Test basic UI state control】
 */
@RunWith(AndroidJUnit4::class)
class InfoViewModelTest {
    
    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: InfoViewModel
    
    @Before
    fun setUp() {
        // 准备环境【Prepare environment】
        val context = ApplicationProvider.getApplicationContext<Context>()
        sessionManager = SessionManager.getInstance()
        sessionManager.initialize(context)
        sessionManager.logout() // 确保初始状态是登出状态【Ensure initial state is logged out】
    }
    
    @After
    fun tearDown() {
        // 清理会话数据【Clean session data】
        sessionManager.logout()
    }
    
    @Test
    fun testRegularUserMode() {
        // 设置普通用户会话【Set regular user session】
        sessionManager.setUserSession("testUser", "test@example.com")
        
        // 创建ViewModel - 这会触发它从SessionManager读取状态【Create ViewModel - This will trigger it to read state from SessionManager】
        viewModel = InfoViewModel()
        
        // 验证ViewModel状态【Verify ViewModel state】
        runBlocking {
            // 检查用户名和邮箱状态流【Check username and email state flows】
            assertEquals("用户名应与会话一致【Username should match session】", "testUser", viewModel.username.first())
            assertEquals("邮箱应与会话一致【Email should match session】", "test@example.com", viewModel.email.first())
            
            // 检查访客模式状态流 - 应为false【Check visitor mode state flow - Should be false】
            assertFalse("普通用户不应处于访客模式【Regular user should not be in visitor mode】", viewModel.isVisitorMode.first())
        }
    }
    
    @Test
    fun testVisitorMode() {
        // 设置访客会话【Set visitor session】
        sessionManager.setUserSession("visitor", "visitor")
        
        // 创建ViewModel【Create ViewModel】
        viewModel = InfoViewModel()
        
        // 验证ViewModel状态【Verify ViewModel state】
        runBlocking {
            // 检查用户名和邮箱状态流【Check username and email state flows】
            assertEquals("用户名应为visitor【Username should be visitor】", "visitor", viewModel.username.first())
            assertEquals("邮箱应为visitor【Email should be visitor】", "visitor", viewModel.email.first())
            
            // 检查访客模式状态流 - 应为true【Check visitor mode state flow - Should be true】
            assertTrue("应检测到访客模式【Should detect visitor mode】", viewModel.isVisitorMode.first())
        }
    }
    
    @Test
    fun testDeleteConfirmationState() {
        // 设置普通用户【Set regular user】
        sessionManager.setUserSession("testUser", "test@example.com")
        viewModel = InfoViewModel()
        
        // 初始状态应为不显示确认删除UI【Initial state should not display delete confirmation UI】
        runBlocking {
            assertFalse("初始不应显示删除确认【Initially should not show delete confirmation】", viewModel.showDeleteConfirmation.first())
        }
        
        // 请求显示删除确认【Request to show delete confirmation】
        viewModel.showDeleteConfirmation()
        
        // 验证状态变化【Verify state change】
        runBlocking {
            assertTrue("应显示删除确认【Should show delete confirmation】", viewModel.showDeleteConfirmation.first())
        }
        
        // 取消删除确认【Cancel delete confirmation】
        viewModel.cancelDeleteConfirmation()
        
        // 验证状态恢复【Verify state restoration】
        runBlocking {
            assertFalse("取消后不应显示删除确认【Should not show delete confirmation after cancellation】", viewModel.showDeleteConfirmation.first())
        }
    }
    
    @Test
    fun testVerificationCodeHandling() {
        // 设置用户并准备模型【Set user and prepare model】
        sessionManager.setUserSession("testUser", "test@example.com")
        viewModel = InfoViewModel()
        viewModel.showDeleteConfirmation()
        
        // 验证初始验证码为空【Verify initial verification code is empty】
        runBlocking {
            assertEquals("初始验证码应为空【Initial verification code should be empty】", "", viewModel.verificationCode.first())
        }
        
        // 更新验证码【Update verification code】
        viewModel.updateVerificationCode("ABC")
        
        // 验证更新【Verify update】
        runBlocking {
            assertEquals("验证码应被更新【Verification code should be updated】", "ABC", viewModel.verificationCode.first())
        }
        
        // 错误情况 - 验证码不匹配【Error case - Verification code mismatch】
        viewModel.confirmDelete()
        
        // 验证错误状态【Verify error state】
        runBlocking {
            assertTrue("错误验证码应产生错误状态【Incorrect verification code should produce error state】", viewModel.deleteResult.first() is DeleteAccountResult.Error)
        }
        
        // 更新为正确的验证码【Update to correct verification code】
        viewModel.updateVerificationCode("1PQ0")
        
        // 验证错误状态被重置【Verify error state is reset】
        runBlocking {
            // 输入正确验证码后，应重置错误状态【After entering correct verification code, error state should be reset】
            assertFalse("正确验证码应重置错误状态【Correct verification code should reset error state】", viewModel.deleteResult.first() is DeleteAccountResult.Error)
        }
    }
}


