package com.example.testingplayground.presentation.viewmodel

import app.cash.turbine.test
import com.example.testingplayground.domain.usecase.AddTodoUseCase
import com.example.testingplayground.domain.usecase.DeleteTodoUseCase
import com.example.testingplayground.domain.usecase.GetTodosUseCase
import com.example.testingplayground.domain.usecase.ToggleTodoUseCase
import com.example.testingplayground.presentation.contract.TodoEffect
import com.example.testingplayground.presentation.contract.TodoEvent
import com.example.testingplayground.util.MainDispatcherRule
import com.example.testingplayground.util.TestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

/*
=====================================================================
TodoViewModel 단위 테스트
=====================================================================

【ViewModel 테스트의 특수성】

ViewModel은 Android 환경에서 동작하며, 내부적으로 Dispatchers.Main을 사용한다.
하지만 JUnit 테스트는 Android 환경이 아니므로 Main Dispatcher가 없다!
→ 테스트 실행 시 "Module with the Main dispatcher had failed to initialize" 에러 발생

해결책: Dispatchers.setMain()으로 테스트용 Dispatcher를 주입

=====================================================================
【이 테스트에서 배우는 핵심 개념】
=====================================================================

1. Dispatchers.setMain() / resetMain()
   - 테스트 환경에서 Main Dispatcher 교체
   - ViewModel의 viewModelScope.launch { }가 동작하게 함

2. StandardTestDispatcher
   - 테스트용 Dispatcher
   - advanceUntilIdle()로 코루틴 실행 제어

3. advanceUntilIdle()
   - 대기 중인 모든 코루틴이 완료될 때까지 진행
   - 비동기 작업 완료를 보장

4. StateFlow (state) 테스트
   - ViewModel의 상태를 나타내는 불변 데이터
   - state.value로 현재 상태 접근

5. SharedFlow (effect) 테스트
   - 일회성 이벤트 (Toast, Navigation 등)
   - Turbine test { }로 검증

【MVI 아키텍처 개념】
- Model: 상태 (TodoState)
- View: Composable UI
- Intent: 이벤트 (TodoEvent)
- Effect: 부수효과 (TodoEffect) - Snackbar, Navigation 등

【개선: MainDispatcherRule 사용】
- @ExtendWith로 Main Dispatcher 자동 교체
- @BeforeEach / @AfterEach 보일러플레이트 제거
- 여러 ViewModel 테스트에서 재사용 가능
*/
@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MainDispatcherRule::class)
class TodoViewModelTest {

    // ═══════════════════════════════════════════════════════════════
    // 테스트 환경 설정
    // ═══════════════════════════════════════════════════════════════

    /*
    MainDispatcherRule이 자동으로 처리:
    - Dispatchers.setMain(testDispatcher)
    - Dispatchers.resetMain()

    【Before】 수동 설정 필요
    private val testDispatcher = StandardTestDispatcher()
    @BeforeEach fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterEach fun tearDown() { Dispatchers.resetMain() }

    【After】 @ExtendWith만으로 충분
    @ExtendWith(MainDispatcherRule::class)
    */

    // ═══════════════════════════════════════════════════════════════
    // Mock 의존성
    // ═══════════════════════════════════════════════════════════════

    /*
    ViewModel이 의존하는 UseCase들
    - 모두 Mock으로 대체하여 격리된 테스트 환경 구성
    */
    private lateinit var getTodosUseCase: GetTodosUseCase
    private lateinit var addTodoUseCase: AddTodoUseCase
    private lateinit var toggleTodoUseCase: ToggleTodoUseCase
    private lateinit var deleteTodoUseCase: DeleteTodoUseCase

    // ═══════════════════════════════════════════════════════════════
    // 테스트 생명주기
    // ═══════════════════════════════════════════════════════════════

    /*
    @BeforeEach - 각 테스트 전 실행

    【개선 포인트】
    - Main Dispatcher 설정은 MainDispatcherRule이 자동 처리
    - Mock 객체 생성만 수행

    【Before】 setMain/resetMain 수동 관리
    【After】 @ExtendWith(MainDispatcherRule::class)로 자동화
    */
    @BeforeEach
    fun setUp() {
        // Mock 객체 생성
        getTodosUseCase = mockk()
        addTodoUseCase = mockk()
        toggleTodoUseCase = mockk()
        deleteTodoUseCase = mockk()
    }

    // @AfterEach 제거됨 - MainDispatcherRule이 resetMain() 자동 처리

    // ═══════════════════════════════════════════════════════════════
    // 헬퍼 함수
    // ═══════════════════════════════════════════════════════════════

    /*
    ViewModel 생성 헬퍼

    【왜 헬퍼 함수로?】
    - ViewModel 생성 시 init 블록에서 데이터 로드
    - 기본 동작(빈 리스트 반환)을 미리 설정
    - 테스트마다 반복 코드 제거

    【every { getTodosUseCase(any()) } returns flowOf(emptyList())】
    - ViewModel init에서 호출되는 UseCase의 기본 동작
    - 빈 Flow를 반환하여 초기화 완료되게 함
    */
    private fun createViewModel(): TodoViewModel {
        every { getTodosUseCase(any()) } returns flowOf(emptyList())
        return TodoViewModel(
            getTodosUseCase,
            addTodoUseCase,
            toggleTodoUseCase,
            deleteTodoUseCase
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // 테스트 케이스 1: 초기 상태 확인
    // ═══════════════════════════════════════════════════════════════

    /*
    테스트: ViewModel 초기 상태는 빈 목록

    【advanceUntilIdle() 핵심】

    【왜 필요한가?】
    - ViewModel 생성 시 init { } 블록에서 데이터 로드
    - 이 로드는 코루틴으로 비동기 실행
    - StandardTestDispatcher는 즉시 실행 안 함
    - advanceUntilIdle()로 모든 코루틴 완료 대기

    【동작 원리】
    1. ViewModel 생성 → init { loadTodos() } 호출
    2. loadTodos()는 viewModelScope.launch { } 실행
    3. 코루틴이 testDispatcher 대기열에 추가됨
    4. advanceUntilIdle() 호출
    5. 대기열의 모든 코루틴이 완료될 때까지 실행
    6. 이제 state를 검증해도 안전
    */
    @Test
    @DisplayName("초기 상태는 빈 목록이다")
    fun `initial state has empty todos`() = runTest {
        // Given: ViewModel 생성
        val viewModel = createViewModel()

        // When: 모든 초기화 코루틴 완료 대기
        /*
        advanceUntilIdle()

        【분해 설명】
        - testDispatcher에 예약된 모든 코루틴 실행
        - delay()도 가상 시간으로 즉시 진행
        - 모든 코루틴이 완료되면 반환

        【비교】
        - advanceUntilIdle(): 모든 작업 완료까지
        - advanceTimeBy(1000): 1초만 진행
        - runCurrent(): 현재 큐의 작업만 실행
        */
        advanceUntilIdle()

        // Then: 빈 목록 확인
        /*
        viewModel.state.value

        【StateFlow 접근】
        - state: StateFlow<TodoState>
        - value: 현재 상태 스냅샷
        - todos: 상태 내의 Todo 리스트
        */
        assertTrue(viewModel.state.value.todos.isEmpty())
    }

    // ═══════════════════════════════════════════════════════════════
    // 테스트 케이스 2: Todo 목록 로드
    // ═══════════════════════════════════════════════════════════════

    /*
    테스트: 초기화 시 Todo 목록을 로드

    【시나리오】
    1. UseCase가 Todo 1개를 반환하도록 설정
    2. ViewModel 생성 (init에서 자동 로드)
    3. advanceUntilIdle()로 로드 완료 대기
    4. state에 Todo가 있는지 확인
    */
    @Test
    @DisplayName("Todo 목록을 로드한다")
    fun `loads todos on init`() = runTest {
        // Given: UseCase가 Todo 목록을 반환 (TestData 빌더 사용)
        val todos = listOf(TestData.todo(id = 1, title = "테스트"))
        every { getTodosUseCase(any()) } returns flowOf(todos)

        /*
        직접 ViewModel 생성 (createViewModel 대신)

        【왜?】
        - createViewModel()은 빈 리스트 반환으로 설정됨
        - 이 테스트는 특정 데이터 로드를 테스트
        - Mock 설정 후 직접 생성 필요
        */
        val viewModel = TodoViewModel(
            getTodosUseCase,
            addTodoUseCase,
            toggleTodoUseCase,
            deleteTodoUseCase
        )

        // When: 초기화 코루틴 완료 대기
        advanceUntilIdle()

        // Then: 로드된 데이터 확인
        assertEquals(1, viewModel.state.value.todos.size)
        assertEquals("테스트", viewModel.state.value.todos[0].title)
    }

    // ═══════════════════════════════════════════════════════════════
    // 테스트 케이스 3: 제목 입력 이벤트
    // ═══════════════════════════════════════════════════════════════

    /*
    테스트: 제목 입력 시 상태 업데이트

    【MVI 이벤트 처리 테스트】

    MVI 패턴에서:
    - Event: 사용자 액션 (OnTitleChanged)
    - State: UI 상태 (titleInput)
    - ViewModel: Event를 받아 State 업데이트
    */
    @Test
    @DisplayName("제목 입력시 상태가 업데이트된다")
    fun `updates title input`() = runTest {
        // Given: ViewModel 초기화 완료
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When: 제목 입력 이벤트 발생
        /*
        viewModel.onEvent(TodoEvent.OnTitleChanged("새 제목"))

        【분해 설명】
        - onEvent(): ViewModel의 이벤트 핸들러
        - TodoEvent.OnTitleChanged: sealed class의 서브타입
        - "새 제목": 입력된 텍스트

        【내부 동작】
        ViewModel에서:
        fun onEvent(event: TodoEvent) {
            when (event) {
                is TodoEvent.OnTitleChanged -> {
                    _state.update { it.copy(titleInput = event.title) }
                }
                ...
            }
        }
        */
        viewModel.onEvent(TodoEvent.OnTitleChanged("새 제목"))

        // Then: 상태 업데이트 확인
        assertEquals("새 제목", viewModel.state.value.titleInput)
    }

    // ═══════════════════════════════════════════════════════════════
    // 테스트 케이스 4: Todo 추가 성공 시 Effect 발생
    // ═══════════════════════════════════════════════════════════════

    /*
    테스트: Todo 추가 성공 시 입력 초기화 + Effect 발생

    【SharedFlow Effect 테스트 - Turbine 활용】

    【Effect란?】
    - 일회성 이벤트 (Toast, Snackbar, Navigation)
    - StateFlow와 달리 한 번만 소비됨
    - 새 구독자에게 이전 값 전달 안 함

    【테스트 순서가 중요!】
    1. effect.test { } 로 구독 시작 (먼저!)
    2. 이벤트 발생 (onEvent)
    3. advanceUntilIdle()로 처리 완료
    4. awaitItem()으로 Effect 수신 확인
    */
    @Test
    @DisplayName("Todo 추가 성공시 입력이 초기화되고 Effect가 발생한다")
    fun `clears input and emits effect on successful add`() = runTest {
        // Given: ViewModel 초기화 및 Mock 설정
        val viewModel = createViewModel()
        advanceUntilIdle()

        /*
        UseCase 성공 응답 설정
        - Result.success(1L): ID 1로 저장 성공
        */
        coEvery { addTodoUseCase(any(), any()) } returns Result.success(1L)

        // 제목 입력 (추가할 내용)
        viewModel.onEvent(TodoEvent.OnTitleChanged("새 할일"))

        // When & Then: Effect 테스트
        /*
        viewModel.effect.test { }

        【분해 설명】
        - effect: SharedFlow<TodoEffect>
        - test { }: Turbine의 Flow 테스트 확장 함수

        【중요: 구독 타이밍】
        SharedFlow는 구독 전 emit된 값을 받을 수 없음
        → test { } 블록 안에서 이벤트 발생시켜야 함!

        【잘못된 예】
        viewModel.onEvent(TodoEvent.OnAddTodo)  // ← Effect 발생
        viewModel.effect.test {
            awaitItem()  // ← 이미 지나감, 타임아웃 발생!
        }

        【올바른 예】
        viewModel.effect.test {
            viewModel.onEvent(TodoEvent.OnAddTodo)  // ← 구독 중 발생
            awaitItem()  // ← 정상 수신
        }
        */
        viewModel.effect.test {
            // Effect 구독 중에 이벤트 발생
            viewModel.onEvent(TodoEvent.OnAddTodo)

            // 코루틴 완료 대기 (UseCase 호출 등)
            advanceUntilIdle()

            // Effect 수신 확인
            /*
            awaitItem()으로 Effect 수신

            【TodoEffect.TodoAdded】
            - sealed class의 object
            - 추가 성공을 나타내는 Effect
            - UI에서 Snackbar 표시 등에 사용
            */
            assertEquals(TodoEffect.TodoAdded, awaitItem())

            // 입력 초기화 확인
            assertEquals("", viewModel.state.value.titleInput)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 테스트 케이스 5: Todo 추가 실패 시 에러 Effect
    // ═══════════════════════════════════════════════════════════════

    /*
    테스트: Todo 추가 실패 시 에러 Effect 발생

    【실패 시나리오 테스트】
    - UseCase가 Result.failure 반환
    - ViewModel이 TodoEffect.ShowError 발생
    - UI에서 에러 메시지 표시
    */
    @Test
    @DisplayName("Todo 추가 실패시 에러 Effect가 발생한다")
    fun `emits error effect on failed add`() = runTest {
        // Given: 초기화
        val viewModel = createViewModel()
        advanceUntilIdle()

        /*
        UseCase 실패 응답 설정
        - Result.failure(Exception("에러")): 실패 + 메시지
        */
        coEvery { addTodoUseCase(any(), any()) } returns Result.failure(Exception("에러"))
        viewModel.onEvent(TodoEvent.OnTitleChanged("새 할일"))

        // When & Then
        viewModel.effect.test {
            viewModel.onEvent(TodoEvent.OnAddTodo)
            advanceUntilIdle()

            /*
            Effect 타입 및 내용 검증

            【TodoEffect.ShowError】
            - data class: 에러 메시지 포함
            - message: String 필드

            【검증 방법】
            1. is 연산자로 타입 확인
            2. 캐스팅 후 필드 검증
            */
            val effect = awaitItem()
            assertTrue(effect is TodoEffect.ShowError)
            assertEquals("에러", (effect as TodoEffect.ShowError).message)
        }
    }
}

/*
=====================================================================
【테스트 실행 결과 예시】
=====================================================================

✅ TodoViewModelTest
   ✅ 초기 상태는 빈 목록이다 (45ms)
   ✅ Todo 목록을 로드한다 (12ms)
   ✅ 제목 입력시 상태가 업데이트된다 (8ms)
   ✅ Todo 추가 성공시 입력이 초기화되고 Effect가 발생한다 (15ms)
   ✅ Todo 추가 실패시 에러 Effect가 발생한다 (10ms)

Tests passed: 5 of 5

=====================================================================
【핵심 학습 포인트 요약】
=====================================================================

1. Main Dispatcher 교체 (필수!)
   ```kotlin
   @BeforeEach
   fun setUp() {
       Dispatchers.setMain(testDispatcher)
   }

   @AfterEach
   fun tearDown() {
       Dispatchers.resetMain()
   }
   ```

2. advanceUntilIdle()
   - 모든 코루틴 완료 대기
   - ViewModel 초기화 후 반드시 호출
   - 비동기 작업 완료 보장

3. StateFlow 테스트
   - viewModel.state.value로 현재 상태 접근
   - 동기적으로 검증 가능

4. SharedFlow (Effect) 테스트
   - Turbine test { } 블록 사용
   - 구독 시작 후 이벤트 발생시켜야 함
   - awaitItem()으로 수신 확인

=====================================================================
【MVI 테스트 플로우】
=====================================================================

  [User Action]
       ↓
  TodoEvent.OnAddTodo  ←── onEvent() 호출
       ↓
  ViewModel 처리
       ↓
  ┌────────────────┐
  │  State 업데이트  │ ←── state.value로 검증
  └────────────────┘
       ↓
  ┌────────────────┐
  │  Effect 발생   │ ←── effect.test { } 로 검증
  └────────────────┘
       ↓
  [UI 반응]

=====================================================================
【Dispatcher 설정 대안: Rule 사용】
=====================================================================

JUnit 5 Extension으로 더 깔끔하게 처리 가능:

```kotlin
@ExtendWith(MainDispatcherExtension::class)
class TodoViewModelTest {
    // setUp/tearDown 필요 없음!
}

class MainDispatcherExtension : BeforeEachCallback, AfterEachCallback {
    private val testDispatcher = StandardTestDispatcher()

    override fun beforeEach(context: ExtensionContext?) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun afterEach(context: ExtensionContext?) {
        Dispatchers.resetMain()
    }
}
```
*/
