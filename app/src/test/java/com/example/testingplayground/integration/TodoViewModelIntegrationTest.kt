package com.example.testingplayground.integration

import app.cash.turbine.test
import com.example.testingplayground.domain.model.Todo
import com.example.testingplayground.domain.usecase.AddTodoUseCase
import com.example.testingplayground.domain.usecase.DeleteTodoUseCase
import com.example.testingplayground.domain.usecase.GetTodosUseCase
import com.example.testingplayground.domain.usecase.ToggleTodoUseCase
import com.example.testingplayground.fake.FakeTodoRepository
import com.example.testingplayground.presentation.contract.TodoEffect
import com.example.testingplayground.presentation.contract.TodoEvent
import com.example.testingplayground.presentation.viewmodel.TodoViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/*
=====================================================================
TodoViewModel 통합 테스트 - Fake Repository + 실제 UseCase
=====================================================================

【Mock vs Fake ViewModel 테스트 비교】

┌─────────────────────────────────────────────────────────────────┐
│  Mock 방식 (기존)                                                │
├─────────────────────────────────────────────────────────────────┤
│  ViewModel ← Mock UseCase (동작 정의 필요)                       │
│                                                                 │
│  장점: UseCase와 완전히 격리된 테스트                            │
│  단점: 매번 stubbing 필요, 실제 로직 검증 불가                   │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  Fake 방식 (이 테스트)                                           │
├─────────────────────────────────────────────────────────────────┤
│  ViewModel ← 실제 UseCase ← FakeRepository                      │
│                                                                 │
│  장점: 실제 동작 검증, stubbing 불필요                           │
│  단점: 더 넓은 범위 테스트 (버그 원인 특정 어려울 수 있음)         │
└─────────────────────────────────────────────────────────────────┘

【이 테스트의 구조 - Hilt 시뮬레이션】

프로덕션 (Hilt 사용):
  @HiltViewModel
  class TodoViewModel @Inject constructor(
      getTodosUseCase: GetTodosUseCase,  ← Hilt가 주입
      addTodoUseCase: AddTodoUseCase,    ← Hilt가 주입
      ...
  )

테스트 (수동 주입):
  val repository = FakeTodoRepository()
  val getTodosUseCase = GetTodosUseCase(repository)  ← 직접 생성
  val viewModel = TodoViewModel(getTodosUseCase, ...)  ← 직접 주입
*/
@OptIn(ExperimentalCoroutinesApi::class)
class TodoViewModelIntegrationTest {

    // ═══════════════════════════════════════════════════════════════
    // 테스트 환경 설정
    // ═══════════════════════════════════════════════════════════════

    private val testDispatcher = StandardTestDispatcher()

    /*
    의존성 체인 (Hilt가 생성하는 것을 수동으로 구현)

    FakeRepository → UseCase들 → ViewModel
    */
    private lateinit var fakeRepository: FakeTodoRepository
    private lateinit var getTodosUseCase: GetTodosUseCase
    private lateinit var addTodoUseCase: AddTodoUseCase
    private lateinit var toggleTodoUseCase: ToggleTodoUseCase
    private lateinit var deleteTodoUseCase: DeleteTodoUseCase
    private lateinit var viewModel: TodoViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // 의존성 체인 구성 (Hilt의 역할)
        fakeRepository = FakeTodoRepository()
        getTodosUseCase = GetTodosUseCase(fakeRepository)
        addTodoUseCase = AddTodoUseCase(fakeRepository)
        toggleTodoUseCase = ToggleTodoUseCase(fakeRepository)
        deleteTodoUseCase = DeleteTodoUseCase(fakeRepository)

        viewModel = TodoViewModel(
            getTodosUseCase,
            addTodoUseCase,
            toggleTodoUseCase,
            deleteTodoUseCase
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ═══════════════════════════════════════════════════════════════
    // 초기 상태 테스트
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("초기 상태")
    inner class InitialStateTest {

        @Test
        @DisplayName("초기 상태는 빈 목록이다")
        fun `initial state has empty todos`() = runTest {
            advanceUntilIdle()

            assertTrue(viewModel.state.value.todos.isEmpty())
            assertFalse(viewModel.state.value.isLoading)
        }

        @Test
        @DisplayName("초기 데이터가 있으면 로드된다")
        fun `loads initial data from repository`() = runTest {
            // Given: Repository에 데이터 미리 추가
            fakeRepository.addAll(
                listOf(
                    Todo(title = "미리 있던 할일1"),
                    Todo(title = "미리 있던 할일2")
                )
            )

            // 새 ViewModel 생성 (init에서 로드)
            val vm = TodoViewModel(
                getTodosUseCase,
                addTodoUseCase,
                toggleTodoUseCase,
                deleteTodoUseCase
            )

            // When
            advanceUntilIdle()

            // Then
            assertEquals(2, vm.state.value.todos.size)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Todo 추가 테스트
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Todo 추가")
    inner class AddTodoTest {

        @Test
        @DisplayName("Todo 추가 시 목록에 반영된다")
        fun `adds todo and updates list`() = runTest {
            advanceUntilIdle()

            // When
            viewModel.onEvent(TodoEvent.OnTitleChanged("새 할일"))
            viewModel.onEvent(TodoEvent.OnAddTodo)
            advanceUntilIdle()

            // Then: 목록에 추가됨
            assertEquals(1, viewModel.state.value.todos.size)
            assertEquals("새 할일", viewModel.state.value.todos[0].title)

            // 입력 필드 초기화됨
            assertEquals("", viewModel.state.value.titleInput)
        }

        @Test
        @DisplayName("Todo 추가 성공 시 Effect가 발생한다")
        fun `emits TodoAdded effect on success`() = runTest {
            advanceUntilIdle()

            viewModel.onEvent(TodoEvent.OnTitleChanged("새 할일"))

            viewModel.effect.test {
                viewModel.onEvent(TodoEvent.OnAddTodo)
                advanceUntilIdle()

                assertEquals(TodoEffect.TodoAdded, awaitItem())
            }
        }

        @Test
        @DisplayName("잘못된 제목으로 추가 시 에러 Effect가 발생한다")
        fun `emits error effect on validation failure`() = runTest {
            advanceUntilIdle()

            viewModel.onEvent(TodoEvent.OnTitleChanged("가"))  // 1글자 (최소 2글자)

            viewModel.effect.test {
                viewModel.onEvent(TodoEvent.OnAddTodo)
                advanceUntilIdle()

                val effect = awaitItem()
                assertTrue(effect is TodoEffect.ShowError)
            }

            // 목록은 비어있어야 함
            assertTrue(viewModel.state.value.todos.isEmpty())
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Todo 토글 테스트
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Todo 토글")
    inner class ToggleTodoTest {

        @Test
        @DisplayName("Todo 토글 시 완료 상태가 변경된다")
        fun `toggles todo completion status`() = runTest {
            // Given: Todo 추가
            advanceUntilIdle()
            viewModel.onEvent(TodoEvent.OnTitleChanged("토글 테스트"))
            viewModel.onEvent(TodoEvent.OnAddTodo)
            advanceUntilIdle()

            val todoId = viewModel.state.value.todos[0].id
            assertFalse(viewModel.state.value.todos[0].isCompleted)

            // When: 토글
            viewModel.onEvent(TodoEvent.OnToggleTodo(todoId))
            advanceUntilIdle()

            // Then: 완료 상태 변경
            assertTrue(viewModel.state.value.todos[0].isCompleted)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Todo 삭제 테스트
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Todo 삭제")
    inner class DeleteTodoTest {

        @Test
        @DisplayName("Todo 삭제 시 목록에서 제거된다")
        fun `deletes todo from list`() = runTest {
            // Given: Todo 추가
            advanceUntilIdle()
            viewModel.onEvent(TodoEvent.OnTitleChanged("삭제할 항목"))
            viewModel.onEvent(TodoEvent.OnAddTodo)
            advanceUntilIdle()

            assertEquals(1, viewModel.state.value.todos.size)
            val todoId = viewModel.state.value.todos[0].id

            // When: 삭제
            viewModel.onEvent(TodoEvent.OnDeleteTodo(todoId))
            advanceUntilIdle()

            // Then: 목록에서 제거됨
            assertTrue(viewModel.state.value.todos.isEmpty())
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 필터링 테스트
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("필터링")
    inner class FilterTest {

        @Test
        @DisplayName("미완료 필터 토글이 정상 동작한다")
        fun `toggles incomplete filter`() = runTest {
            // Given: 완료/미완료 Todo 추가
            advanceUntilIdle()

            viewModel.onEvent(TodoEvent.OnTitleChanged("할일1"))
            viewModel.onEvent(TodoEvent.OnAddTodo)
            advanceUntilIdle()

            viewModel.onEvent(TodoEvent.OnTitleChanged("할일2"))
            viewModel.onEvent(TodoEvent.OnAddTodo)
            advanceUntilIdle()

            // 첫 번째 Todo 완료 처리
            val firstTodoId = viewModel.state.value.todos[0].id
            viewModel.onEvent(TodoEvent.OnToggleTodo(firstTodoId))
            advanceUntilIdle()

            assertEquals(2, viewModel.state.value.todos.size)

            // When: 필터 토글 (미완료만 보기)
            viewModel.onEvent(TodoEvent.OnToggleFilter)
            advanceUntilIdle()

            // Then: 미완료 항목만 표시
            assertEquals(1, viewModel.state.value.todos.size)
            assertFalse(viewModel.state.value.todos[0].isCompleted)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 전체 시나리오 테스트
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("통합 시나리오")
    inner class IntegrationScenarioTest {

        @Test
        @DisplayName("전체 사용자 시나리오가 정상 동작한다")
        fun `full user scenario works correctly`() = runTest {
            advanceUntilIdle()

            // 1. Todo 추가
            viewModel.onEvent(TodoEvent.OnTitleChanged("장보기"))
            viewModel.onEvent(TodoEvent.OnDescriptionChanged("우유, 빵"))
            viewModel.onEvent(TodoEvent.OnAddTodo)
            advanceUntilIdle()

            viewModel.onEvent(TodoEvent.OnTitleChanged("운동하기"))
            viewModel.onEvent(TodoEvent.OnAddTodo)
            advanceUntilIdle()

            assertEquals(2, viewModel.state.value.todos.size)

            // 2. 첫 번째 Todo 완료 처리
            val shoppingId = viewModel.state.value.todos
                .find { it.title == "장보기" }!!.id
            viewModel.onEvent(TodoEvent.OnToggleTodo(shoppingId))
            advanceUntilIdle()

            // 3. 미완료만 필터링
            viewModel.onEvent(TodoEvent.OnToggleFilter)
            advanceUntilIdle()

            assertEquals(1, viewModel.state.value.todos.size)
            assertEquals("운동하기", viewModel.state.value.todos[0].title)

            // 4. 필터 해제
            viewModel.onEvent(TodoEvent.OnToggleFilter)
            advanceUntilIdle()

            assertEquals(2, viewModel.state.value.todos.size)

            // 5. 완료된 Todo 삭제
            viewModel.onEvent(TodoEvent.OnDeleteTodo(shoppingId))
            advanceUntilIdle()

            assertEquals(1, viewModel.state.value.todos.size)
            assertEquals("운동하기", viewModel.state.value.todos[0].title)
        }
    }
}

/*
=====================================================================
【Mock vs Fake 선택 가이드】
=====================================================================

Mock 사용 (단위 테스트):
- 특정 클래스의 로직만 격리해서 테스트할 때
- 의존성의 특정 동작(예외 발생 등)을 시뮬레이션할 때
- 빠른 테스트가 필요할 때

Fake 사용 (통합 테스트):
- 여러 클래스가 협력하는 시나리오 테스트할 때
- 실제 데이터 흐름을 검증할 때
- End-to-End에 가까운 테스트가 필요할 때

【권장 조합】
- UseCase 단위 테스트: Mock Repository
- UseCase 통합 테스트: Fake Repository
- ViewModel 단위 테스트: Mock UseCase
- ViewModel 통합 테스트: Fake Repository + 실제 UseCase
*/
