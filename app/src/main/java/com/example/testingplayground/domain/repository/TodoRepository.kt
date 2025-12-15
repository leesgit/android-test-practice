package com.example.testingplayground.domain.repository

import com.example.testingplayground.domain.model.Todo
import kotlinx.coroutines.flow.Flow

interface TodoRepository {
    fun getTodos(): Flow<List<Todo>>
    fun getTodoById(id: Long): Flow<Todo?>
    suspend fun addTodo(todo: Todo): Long
    suspend fun updateTodo(todo: Todo)
    suspend fun deleteTodo(id: Long)
}
