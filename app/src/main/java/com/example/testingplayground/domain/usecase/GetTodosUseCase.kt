package com.example.testingplayground.domain.usecase

import com.example.testingplayground.domain.model.Todo
import com.example.testingplayground.domain.repository.TodoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetTodosUseCase @Inject constructor(
    private val repository: TodoRepository
) {
    operator fun invoke(showOnlyIncomplete: Boolean = false): Flow<List<Todo>> {
        return repository.getTodos().map { todos ->
            val filtered = if (showOnlyIncomplete) {
                todos.filter { !it.isCompleted }
            } else {
                todos
            }
            filtered.sortedByDescending { it.createdAt }
        }
    }
}
