package com.example.testingplayground.domain.model

data class Todo(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

// 테스트용 헬퍼 함수
fun createTestTodo(
    id: Long = 1,
    title: String = "테스트 할일",
    description: String = "",
    isCompleted: Boolean = false,
    createdAt: Long = System.currentTimeMillis()
) = Todo(id, title, description, isCompleted, createdAt)
