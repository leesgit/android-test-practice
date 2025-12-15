package com.example.testingplayground.fake

import com.example.testingplayground.domain.model.Todo
import com.example.testingplayground.domain.repository.TodoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * =====================================================================
 * FakeTodoRepository - 테스트용 가짜 Repository
 * =====================================================================
 *
 * 【Fake vs Mock 차이】
 *
 * ▸ Mock (MockK)
 *   - 빈 껍데기 + 동작 정의 (every { } returns ...)
 *   - 호출 검증 가능 (verify { })
 *   - 실제 로직 없음
 *
 * ▸ Fake
 *   - 실제로 동작하는 간단한 구현체
 *   - 인메모리 저장소 사용
 *   - 실제 DB 없이 통합 테스트 가능
 *
 * 【사용 시나리오】
 * - UseCase 통합 테스트
 * - ViewModel 통합 테스트
 * - Hilt 테스트에서 실제 Repository 대체
 *
 * 【장점】
 * - 실제 로직 흐름 테스트 가능
 * - Mock 설정 없이 자연스러운 테스트
 * - 여러 UseCase가 협력하는 시나리오 테스트
 */
class FakeTodoRepository : TodoRepository {

    /**
     * 인메모리 저장소
     * - Map<Long, Todo>: ID를 키로 사용
     * - MutableStateFlow: 변경 시 구독자에게 자동 알림
     */
    private val todos = MutableStateFlow<Map<Long, Todo>>(emptyMap())
    private var nextId = 1L

    // ═══════════════════════════════════════════════════════════════
    // TodoRepository 구현
    // ═══════════════════════════════════════════════════════════════

    override fun getTodos(): Flow<List<Todo>> {
        return todos.map { it.values.toList() }
    }

    override fun getTodoById(id: Long): Flow<Todo?> {
        return todos.map { it[id] }
    }

    override suspend fun addTodo(todo: Todo): Long {
        val id = nextId++
        val newTodo = todo.copy(id = id)
        todos.update { it + (id to newTodo) }
        return id
    }

    override suspend fun updateTodo(todo: Todo) {
        todos.update { it + (todo.id to todo) }
    }

    override suspend fun deleteTodo(id: Long) {
        todos.update { it - id }
    }

    // ═══════════════════════════════════════════════════════════════
    // 테스트 헬퍼 메서드
    // ═══════════════════════════════════════════════════════════════

    /**
     * 저장소 초기화 - 테스트 간 격리를 위해 사용
     */
    fun clear() {
        todos.value = emptyMap()
        nextId = 1L
    }

    /**
     * 테스트 데이터 한번에 추가
     */
    fun addAll(todoList: List<Todo>) {
        val todosWithIds = todoList.mapIndexed { index, todo ->
            val id = nextId++
            id to todo.copy(id = id)
        }.toMap()
        todos.update { it + todosWithIds }
    }

    /**
     * 현재 저장된 Todo 개수
     */
    fun count(): Int = todos.value.size
}
