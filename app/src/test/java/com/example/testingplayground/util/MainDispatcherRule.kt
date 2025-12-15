package com.example.testingplayground.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * =====================================================================
 * MainDispatcherRule - ViewModel 테스트를 위한 JUnit5 Extension
 * =====================================================================
 *
 * 【왜 필요한가?】
 *
 * ViewModel은 내부적으로 Dispatchers.Main을 사용한다.
 * - viewModelScope.launch { } → Main Dispatcher 사용
 * - JUnit 테스트 환경에는 Main Dispatcher가 없다.
 * - 테스트 시 "Module with the Main dispatcher had failed to initialize" 에러가 발생한다.
 *
 * 해결책: 테스트 시작 전 Main Dispatcher를 TestDispatcher로 교체한다.
 *
 * =====================================================================
 * 【사용법】
 * =====================================================================
 *
 * ```kotlin
 * @ExtendWith(MainDispatcherRule::class)
 * class TodoViewModelTest {
 *     // @BeforeEach / @AfterEach에서 setMain/resetMain 불필요!
 *     // Extension이 자동으로 처리한다.
 *
 *     @Test
 *     fun `test something`() = runTest {
 *         val viewModel = createViewModel()
 *         advanceUntilIdle()
 *         // ...
 *     }
 * }
 * ```
 *
 * =====================================================================
 * 【JUnit4 Rule vs JUnit5 Extension】
 * =====================================================================
 *
 * JUnit4:
 * ```kotlin
 * @get:Rule
 * val mainDispatcherRule = MainDispatcherRule()
 * ```
 *
 * JUnit5:
 * ```kotlin
 * @ExtendWith(MainDispatcherRule::class)
 * class MyTest { }
 * ```
 *
 * =====================================================================
 * 【동작 원리】
 * =====================================================================
 *
 * 1. beforeEach(): 각 테스트 전에 Dispatchers.setMain(testDispatcher) 호출
 * 2. 테스트 실행: ViewModel의 viewModelScope가 testDispatcher를 사용한다.
 * 3. afterEach(): 각 테스트 후에 Dispatchers.resetMain()으로 복원한다.
 *
 * 【StandardTestDispatcher 특징】
 * - 코루틴을 즉시 실행하지 않고 대기열에 추가한다.
 * - advanceUntilIdle()로 명시적으로 실행한다.
 * - 테스트에서 코루틴 타이밍을 제어할 수 있다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : BeforeEachCallback, AfterEachCallback {

    /**
     * 각 테스트 시작 전 실행된다.
     * Main Dispatcher를 TestDispatcher로 교체한다.
     */
    override fun beforeEach(context: ExtensionContext?) {
        Dispatchers.setMain(testDispatcher)
    }

    /**
     * 각 테스트 종료 후 실행된다.
     * Main Dispatcher를 원래대로 복원한다.
     * 다른 테스트에 영향을 주지 않도록 정리한다.
     */
    override fun afterEach(context: ExtensionContext?) {
        Dispatchers.resetMain()
    }
}

/**
 * =====================================================================
 * 【Before & After 비교】
 * =====================================================================
 *
 * ❌ 수동 설정 (반복 코드)
 * ```kotlin
 * class TodoViewModelTest {
 *     private val testDispatcher = StandardTestDispatcher()
 *
 *     @BeforeEach
 *     fun setUp() {
 *         Dispatchers.setMain(testDispatcher)  // 매번 작성
 *     }
 *
 *     @AfterEach
 *     fun tearDown() {
 *         Dispatchers.resetMain()  // 매번 작성
 *     }
 * }
 * ```
 *
 * ✅ Extension 사용 (깔끔)
 * ```kotlin
 * @ExtendWith(MainDispatcherRule::class)
 * class TodoViewModelTest {
 *     // setUp/tearDown 불필요!
 * }
 * ```
 *
 * =====================================================================
 * 【다른 ViewModel 테스트에서도 재사용】
 * =====================================================================
 *
 * @ExtendWith(MainDispatcherRule::class)
 * class UserViewModelTest { ... }
 *
 * @ExtendWith(MainDispatcherRule::class)
 * class CartViewModelTest { ... }
 *
 * @ExtendWith(MainDispatcherRule::class)
 * class SearchViewModelTest { ... }
 */
