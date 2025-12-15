package com.example.testingplayground.integration

import app.cash.turbine.test
import com.example.testingplayground.domain.model.Todo
import com.example.testingplayground.domain.usecase.AddTodoUseCase
import com.example.testingplayground.domain.usecase.DeleteTodoUseCase
import com.example.testingplayground.domain.usecase.GetTodosUseCase
import com.example.testingplayground.domain.usecase.ToggleTodoUseCase
import com.example.testingplayground.fake.FakeTodoRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/*
=====================================================================
UseCase 통합 테스트 - Fake Repository 사용
=====================================================================

【Mock vs Fake 테스트 비교】

┌─────────────────────────────────────────────────────────────────┐
│  Mock 테스트 (단위 테스트)                                        │
├─────────────────────────────────────────────────────────────────┤
│  - mockk<Repository>()로 가짜 객체 생성                          │
│  - every { } returns로 동작 정의                                 │
│  - 각 테스트마다 예상 동작 설정 필요                              │
│  - UseCase 로직만 격리해서 테스트                                │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  Fake 테스트 (통합 테스트)                                        │
├─────────────────────────────────────────────────────────────────┤
│  - FakeRepository로 실제 동작하는 구현체 사용                    │
│  - 인메모리 저장소로 실제 CRUD 동작                              │
│  - UseCase + Repository 함께 테스트                             │
│  - 실제 데이터 흐름 검증                                        │
└─────────────────────────────────────────────────────────────────┘

【Hilt와의 관계】

프로덕션 코드:
  Hilt가 TodoRepositoryImpl 주입 → UseCase 사용

테스트 코드:
  FakeTodoRepository 직접 주입 → UseCase 사용
  (Hilt의 @TestInstallIn으로 자동 교체되는 것과 동일한 효과)

【장점】
- 실제 UseCase 간 협력 테스트 가능
- Mock 설정 없이 자연스러운 테스트
- 데이터 일관성 검증
*/
class TodoUseCaseIntegrationTest {

    // ═══════════════════════════════════════════════════════════════
    // 의존성 설정 - Hilt가 하는 것을 수동으로 구현
    // ═══════════════════════════════════════════════════════════════

    /*
    FakeTodoRepository - 실제 동작하는 테스트용 Repository

    【Hilt 동작 시뮬레이션】
    프로덕션: @Inject constructor → Hilt가 Repository 주입
    테스트: 직접 Fake 인스턴스 생성 후 주입
    */
    private lateinit var fakeRepository: FakeTodoRepository

    /*
    UseCase들 - FakeRepository를 주입받음

    【실제 Hilt 동작과 동일】
    - UseCase는 Repository 인터페이스에 의존
    - 테스트에서 Fake 구현체 주입
    - UseCase 코드 변경 없이 테스트
    */
    private lateinit var getTodosUseCase: GetTodosUseCase
    private lateinit var addTodoUseCase: AddTodoUseCase
    private lateinit var toggleTodoUseCase: ToggleTodoUseCase
    private lateinit var deleteTodoUseCase: DeleteTodoUseCase

    /*
    @BeforeEach - 매 테스트 전 의존성 초기화

    【Hilt의 역할을 수동으로 구현】
    1. FakeRepository 생성 (Singleton)
    2. 각 UseCase에 Repository 주입
    3. 테스트 간 격리를 위해 매번 새로 생성
    */
    @BeforeEach
    fun setUp() {
        // 1. Repository 생성 (Hilt의 @Provides 역할)
        fakeRepository = FakeTodoRepository()

        // 2. UseCase에 의존성 주입 (Hilt의 @Inject constructor 역할)
        getTodosUseCase = GetTodosUseCase(fakeRepository)
        addTodoUseCase = AddTodoUseCase(fakeRepository)
        toggleTodoUseCase = ToggleTodoUseCase(fakeRepository)
        deleteTodoUseCase = DeleteTodoUseCase(fakeRepository)
    }

    // ═══════════════════════════════════════════════════════════════
    // GetTodosUseCase 통합 테스트
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GetTodosUseCase 통합 테스트")
    inner class GetTodosUseCaseTest {

        @Test
        @DisplayName("빈 저장소에서 빈 리스트를 반환한다")
        fun `returns empty list when repository is empty`() = runTest {
            // When & Then
            getTodosUseCase(showOnlyIncomplete = false).test {
                val result = awaitItem()
                assertTrue(result.isEmpty())
                // Flow가 계속 살아있으므로 cancelAndIgnoreRemainingEvents 사용
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("저장된 Todo를 최신순으로 정렬하여 반환한다")
        fun `returns todos sorted by createdAt descending`() = runTest {
            // Given: 데이터 추가
            fakeRepository.addAll(
                listOf(
                    Todo(title = "첫번째", createdAt = 1000),
                    Todo(title = "두번째", createdAt = 2000),
                    Todo(title = "세번째", createdAt = 3000)
                )
            )

            // When & Then
            getTodosUseCase(showOnlyIncomplete = false).test {
                val result = awaitItem()

                assertEquals(3, result.size)
                assertEquals("세번째", result[0].title)  // 가장 최신
                assertEquals("두번째", result[1].title)
                assertEquals("첫번째", result[2].title)  // 가장 오래됨

                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("미완료 필터링이 정상 동작한다")
        fun `filters incomplete todos when showOnlyIncomplete is true`() = runTest {
            // Given
            fakeRepository.addAll(
                listOf(
                    Todo(title = "완료됨", isCompleted = true),
                    Todo(title = "미완료1", isCompleted = false),
                    Todo(title = "미완료2", isCompleted = false)
                )
            )

            // When & Then
            getTodosUseCase(showOnlyIncomplete = true).test {
                val result = awaitItem()

                assertEquals(2, result.size)
                assertTrue(result.all { !it.isCompleted })

                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // AddTodoUseCase 통합 테스트
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("AddTodoUseCase 통합 테스트")
    inner class AddTodoUseCaseTest {

        @Test
        @DisplayName("Todo 추가 후 저장소에 실제로 저장된다")
        fun `todo is actually stored in repository after add`() = runTest {
            // When
            val result = addTodoUseCase("새 할일", "설명입니다")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(1, fakeRepository.count())

            // 저장된 데이터 확인
            getTodosUseCase(false).test {
                val todos = awaitItem()
                assertEquals(1, todos.size)
                assertEquals("새 할일", todos[0].title)
                assertEquals("설명입니다", todos[0].description)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("여러 Todo 추가 시 모두 저장된다")
        fun `multiple todos are stored correctly`() = runTest {
            // When
            addTodoUseCase("할일1", "")
            addTodoUseCase("할일2", "")
            addTodoUseCase("할일3", "")

            // Then
            assertEquals(3, fakeRepository.count())
        }

        @Test
        @DisplayName("유효성 검증 실패 시 저장되지 않는다")
        fun `invalid todo is not stored`() = runTest {
            // When: 1글자 제목 (최소 2글자)
            val result = addTodoUseCase("가", "")

            // Then
            assertTrue(result.isFailure)
            assertEquals(0, fakeRepository.count())
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ToggleTodoUseCase 통합 테스트
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("ToggleTodoUseCase 통합 테스트")
    inner class ToggleTodoUseCaseTest {

        @Test
        @DisplayName("Todo 토글 후 상태가 변경된다")
        fun `toggle changes todo completion status`() = runTest {
            // Given
            val id = addTodoUseCase("테스트", "").getOrThrow()

            // When: 토글 (false → true)
            val result = toggleTodoUseCase(id)

            // Then
            assertTrue(result.isSuccess)
            assertTrue(result.getOrThrow())

            getTodosUseCase(false).test {
                val todos = awaitItem()
                assertTrue(todos.first().isCompleted)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("두 번 토글하면 원래 상태로 돌아온다")
        fun `double toggle returns to original state`() = runTest {
            // Given
            val id = addTodoUseCase("테스트", "").getOrThrow()

            // When: 두 번 토글
            toggleTodoUseCase(id)
            toggleTodoUseCase(id)

            // Then: 원래 상태 (false)
            getTodosUseCase(false).test {
                val todos = awaitItem()
                assertFalse(todos.first().isCompleted)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("존재하지 않는 Todo 토글 시 실패한다")
        fun `toggle non-existent todo fails`() = runTest {
            // When
            val result = toggleTodoUseCase(999L)

            // Then
            assertTrue(result.isFailure)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // DeleteTodoUseCase 통합 테스트
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DeleteTodoUseCase 통합 테스트")
    inner class DeleteTodoUseCaseTest {

        @Test
        @DisplayName("Todo 삭제 후 저장소에서 제거된다")
        fun `deleted todo is removed from repository`() = runTest {
            // Given
            val id = addTodoUseCase("삭제할 항목", "").getOrThrow()
            assertEquals(1, fakeRepository.count())

            // When
            val result = deleteTodoUseCase(id)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(0, fakeRepository.count())
        }

        @Test
        @DisplayName("잘못된 ID로 삭제 시 실패한다")
        fun `delete with invalid id fails`() = runTest {
            // When
            val result = deleteTodoUseCase(-1)

            // Then
            assertTrue(result.isFailure)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // UseCase 협력 통합 테스트
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("UseCase 협력 테스트")
    inner class UseCaseCollaborationTest {

        @Test
        @DisplayName("전체 CRUD 시나리오가 정상 동작한다")
        fun `full CRUD scenario works correctly`() = runTest {
            // Create
            val id1 = addTodoUseCase("할일1", "설명1").getOrThrow()
            val id2 = addTodoUseCase("할일2", "설명2").getOrThrow()

            // Read
            getTodosUseCase(false).test {
                assertEquals(2, awaitItem().size)
                cancelAndIgnoreRemainingEvents()
            }

            // Update (Toggle)
            toggleTodoUseCase(id1)

            getTodosUseCase(showOnlyIncomplete = true).test {
                val incomplete = awaitItem()
                assertEquals(1, incomplete.size)
                assertEquals("할일2", incomplete[0].title)
                cancelAndIgnoreRemainingEvents()
            }

            // Delete
            deleteTodoUseCase(id2)

            getTodosUseCase(false).test {
                val remaining = awaitItem()
                assertEquals(1, remaining.size)
                assertEquals("할일1", remaining[0].title)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}

/*
=====================================================================
【테스트 실행 결과 예시】
=====================================================================

✅ TodoUseCaseIntegrationTest
   ✅ GetTodosUseCase 통합 테스트
      ✅ 빈 저장소에서 빈 리스트를 반환한다
      ✅ 저장된 Todo를 최신순으로 정렬하여 반환한다
      ✅ 미완료 필터링이 정상 동작한다
   ✅ AddTodoUseCase 통합 테스트
      ✅ Todo 추가 후 저장소에 실제로 저장된다
      ✅ 여러 Todo 추가 시 모두 저장된다
      ✅ 유효성 검증 실패 시 저장되지 않는다
   ✅ ToggleTodoUseCase 통합 테스트
      ...

=====================================================================
【핵심 포인트】
=====================================================================

1. Fake는 실제 동작하는 테스트용 구현체
2. Mock과 달리 stubbing 없이 자연스러운 테스트
3. 여러 UseCase가 협력하는 시나리오 테스트 가능
4. Hilt의 의존성 주입을 수동으로 시뮬레이션
*/
