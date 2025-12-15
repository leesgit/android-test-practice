package com.example.testingplayground.domain.usecase

import com.example.testingplayground.domain.repository.TodoRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ToggleTodoUseCase @Inject constructor(
    private val repository: TodoRepository
) {
    suspend operator fun invoke(id: Long): Result<Boolean> {
        return runCatching {
            val todo = repository.getTodoById(id).first()
                ?: throw NoSuchElementException("Todo not found: $id")

            val toggled = todo.copy(isCompleted = !todo.isCompleted)
            repository.updateTodo(toggled)
            toggled.isCompleted
        }
    }
}
