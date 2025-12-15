package com.example.testingplayground.util

import com.example.testingplayground.domain.model.Todo

/**
 * =====================================================================
 * TestData - 테스트 데이터 빌더 및 Fixture
 * =====================================================================
 *
 * 【왜 필요한가?】
 *
 * 테스트에서 반복되는 데이터 생성 코드를 줄이고,
 * 일관된 테스트 데이터를 제공하기 위해 사용한다.
 *
 * ❌ 반복적인 데이터 생성
 * ```kotlin
 * val todo1 = Todo(id = 1, title = "테스트1", description = "", isCompleted = false)
 * val todo2 = Todo(id = 2, title = "테스트2", description = "", isCompleted = true)
 * ```
 *
 * ✅ 빌더 패턴 사용
 * ```kotlin
 * val todo1 = TestData.todo()
 * val todo2 = TestData.todo(isCompleted = true)
 * val todos = TestData.todoList(3)
 * ```
 *
 * =====================================================================
 * 【빌더 패턴의 장점】
 * =====================================================================
 *
 * 1. 코드 간결성: 필요한 필드만 지정한다.
 * 2. 기본값 제공: 테스트마다 동일한 기본 데이터를 사용한다.
 * 3. 유지보수성: 데이터 구조 변경 시 한 곳만 수정하면 된다.
 * 4. 가독성: 테스트 의도가 명확해진다.
 */
object TestData {

    // ═══════════════════════════════════════════════════════════════
    // Todo 빌더
    // ═══════════════════════════════════════════════════════════════

    /**
     * 기본 Todo를 생성한다.
     *
     * 【사용 예시】
     * ```kotlin
     * val todo = TestData.todo()                          // 기본값
     * val completed = TestData.todo(isCompleted = true)   // 완료된 Todo
     * val custom = TestData.todo(title = "커스텀 제목")    // 제목 지정
     * ```
     */
    fun todo(
        id: Long = 1L,
        title: String = "테스트 할일",
        description: String = "테스트 설명",
        isCompleted: Boolean = false,
        createdAt: Long = System.currentTimeMillis()
    ) = Todo(
        id = id,
        title = title,
        description = description,
        isCompleted = isCompleted,
        createdAt = createdAt
    )

    /**
     * 완료된 Todo를 생성한다. (편의 메서드)
     */
    fun completedTodo(
        id: Long = 1L,
        title: String = "완료된 할일"
    ) = todo(
        id = id,
        title = title,
        isCompleted = true
    )

    /**
     * 미완료 Todo를 생성한다. (편의 메서드)
     */
    fun incompleteTodo(
        id: Long = 1L,
        title: String = "미완료 할일"
    ) = todo(
        id = id,
        title = title,
        isCompleted = false
    )

    // ═══════════════════════════════════════════════════════════════
    // Todo 리스트 빌더
    // ═══════════════════════════════════════════════════════════════

    /**
     * Todo 리스트를 생성한다.
     *
     * 【사용 예시】
     * ```kotlin
     * val todos = TestData.todoList(3)  // 3개의 Todo 생성
     * // 결과: [Todo(id=1, title="할일 1"), Todo(id=2, title="할일 2"), Todo(id=3, title="할일 3")]
     * ```
     */
    fun todoList(
        count: Int,
        titlePrefix: String = "할일"
    ): List<Todo> = (1..count).map { index ->
        todo(
            id = index.toLong(),
            title = "$titlePrefix $index",
            createdAt = System.currentTimeMillis() + index // 순서 보장
        )
    }

    /**
     * 완료/미완료 섞인 리스트를 생성한다.
     *
     * 【사용 예시】
     * ```kotlin
     * val mixed = TestData.mixedTodoList()
     * // 완료 2개 + 미완료 3개 = 5개
     * ```
     */
    fun mixedTodoList(
        completedCount: Int = 2,
        incompleteCount: Int = 3
    ): List<Todo> {
        val completed = (1..completedCount).map { index ->
            completedTodo(id = index.toLong(), title = "완료 $index")
        }
        val incomplete = (1..incompleteCount).map { index ->
            incompleteTodo(id = (completedCount + index).toLong(), title = "미완료 $index")
        }
        return completed + incomplete
    }

    /**
     * 정렬 테스트용 리스트를 생성한다. (createdAt이 다름)
     *
     * 【사용 예시】
     * ```kotlin
     * val sortedTodos = TestData.sortedTodoList()
     * // createdAt이 다른 3개의 Todo
     * ```
     */
    fun sortedTodoList(): List<Todo> = listOf(
        todo(id = 1, title = "오래된 할일", createdAt = 1000L),
        todo(id = 2, title = "중간 할일", createdAt = 2000L),
        todo(id = 3, title = "최신 할일", createdAt = 3000L)
    )

    // ═══════════════════════════════════════════════════════════════
    // 에러 시나리오용 데이터
    // ═══════════════════════════════════════════════════════════════

    /**
     * 유효하지 않은 제목 (너무 짧음)
     */
    const val INVALID_SHORT_TITLE = "가"

    /**
     * 유효하지 않은 제목 (너무 김)
     */
    val INVALID_LONG_TITLE = "가".repeat(101)

    /**
     * 존재하지 않는 ID
     */
    const val NON_EXISTENT_ID = 999L
}

/**
 * =====================================================================
 * 【테스트에서의 사용 예시】
 * =====================================================================
 *
 * @Test
 * fun `Todo 목록을 정렬하여 반환한다`() = runTest {
 *     // Given
 *     val todos = TestData.sortedTodoList()
 *     every { repository.getTodos() } returns flowOf(todos)
 *
 *     // When
 *     useCase(showOnlyIncomplete = false).test {
 *         val result = awaitItem()
 *
 *         // Then
 *         assertEquals("최신 할일", result[0].title)  // 최신순 정렬
 *     }
 * }
 *
 * @Test
 * fun `완료된 Todo만 필터링한다`() = runTest {
 *     // Given
 *     val todos = TestData.mixedTodoList(completedCount = 2, incompleteCount = 3)
 *     every { repository.getTodos() } returns flowOf(todos)
 *
 *     // When & Then
 *     useCase(showOnlyIncomplete = true).test {
 *         val result = awaitItem()
 *         assertEquals(3, result.size)  // 미완료만
 *     }
 * }
 *
 * @Test
 * fun `짧은 제목으로 추가 시 실패한다`() = runTest {
 *     // When
 *     val result = useCase(TestData.INVALID_SHORT_TITLE, "설명")
 *
 *     // Then
 *     assertTrue(result.isFailure)
 * }
 */
