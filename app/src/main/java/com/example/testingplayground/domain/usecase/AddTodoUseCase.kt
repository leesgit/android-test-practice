package com.example.testingplayground.domain.usecase

import com.example.testingplayground.domain.model.Todo
import com.example.testingplayground.domain.repository.TodoRepository
import javax.inject.Inject

class AddTodoUseCase @Inject constructor(
    private val repository: TodoRepository
) {
    suspend operator fun invoke(title: String, description: String = ""): Result<Long> {
        return runCatching {
            require(title.length >= 2) { "제목은 2글자 이상이어야 합니다" }
            require(title.length <= 100) { "제목은 100글자 이하여야 합니다" }

            val todo = Todo(
                title = title.trim(),
                description = description.trim()
            )
            repository.addTodo(todo)
        }
    }
}
