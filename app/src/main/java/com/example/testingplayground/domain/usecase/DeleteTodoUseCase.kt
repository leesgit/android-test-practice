package com.example.testingplayground.domain.usecase

import com.example.testingplayground.domain.repository.TodoRepository
import javax.inject.Inject

class DeleteTodoUseCase @Inject constructor(
    private val repository: TodoRepository
) {
    suspend operator fun invoke(id: Long): Result<Unit> {
        return runCatching {
            require(id > 0) { "유효하지 않은 ID입니다" }
            repository.deleteTodo(id)
        }
    }
}
