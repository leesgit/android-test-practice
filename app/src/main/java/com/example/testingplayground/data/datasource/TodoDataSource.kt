package com.example.testingplayground.data.datasource

import com.example.testingplayground.domain.model.Todo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TodoDataSource @Inject constructor() {
    private val todos = MutableStateFlow<Map<Long, Todo>>(emptyMap())
    private var nextId = 1L

    fun getTodos(): Flow<List<Todo>> = todos.map { it.values.toList() }

    fun getTodoById(id: Long): Flow<Todo?> = todos.map { it[id] }

    suspend fun addTodo(todo: Todo): Long {
        val id = nextId++
        val newTodo = todo.copy(id = id)
        todos.update { it + (id to newTodo) }
        return id
    }

    suspend fun updateTodo(todo: Todo) {
        todos.update { it + (todo.id to todo) }
    }

    suspend fun deleteTodo(id: Long) {
        todos.update { it - id }
    }
}
