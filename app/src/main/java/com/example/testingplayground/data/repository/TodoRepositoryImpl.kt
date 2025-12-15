package com.example.testingplayground.data.repository

import com.example.testingplayground.data.datasource.TodoDataSource
import com.example.testingplayground.domain.model.Todo
import com.example.testingplayground.domain.repository.TodoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TodoRepositoryImpl @Inject constructor(
    private val dataSource: TodoDataSource
) : TodoRepository {
    override fun getTodos(): Flow<List<Todo>> = dataSource.getTodos()
    override fun getTodoById(id: Long): Flow<Todo?> = dataSource.getTodoById(id)
    override suspend fun addTodo(todo: Todo): Long = dataSource.addTodo(todo)
    override suspend fun updateTodo(todo: Todo) = dataSource.updateTodo(todo)
    override suspend fun deleteTodo(id: Long) = dataSource.deleteTodo(id)
}
