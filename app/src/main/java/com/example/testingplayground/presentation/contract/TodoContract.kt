package com.example.testingplayground.presentation.contract

import com.example.testingplayground.domain.model.Todo

data class TodoState(
    val todos: List<Todo> = emptyList(),
    val isLoading: Boolean = false,
    val showOnlyIncomplete: Boolean = false,
    val titleInput: String = "",
    val descriptionInput: String = ""
) {
    val totalCount: Int get() = todos.size
    val completedCount: Int get() = todos.count { it.isCompleted }
    val incompleteCount: Int get() = todos.count { !it.isCompleted }
}

sealed interface TodoEvent {
    data class OnTitleChanged(val title: String) : TodoEvent
    data class OnDescriptionChanged(val description: String) : TodoEvent
    data object OnAddTodo : TodoEvent
    data class OnToggleTodo(val id: Long) : TodoEvent
    data class OnDeleteTodo(val id: Long) : TodoEvent
    data object OnToggleFilter : TodoEvent
}

sealed interface TodoEffect {
    data class ShowError(val message: String) : TodoEffect
    data object TodoAdded : TodoEffect
}
