package com.example.testingplayground.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.testingplayground.domain.usecase.AddTodoUseCase
import com.example.testingplayground.domain.usecase.DeleteTodoUseCase
import com.example.testingplayground.domain.usecase.GetTodosUseCase
import com.example.testingplayground.domain.usecase.ToggleTodoUseCase
import com.example.testingplayground.presentation.contract.TodoEffect
import com.example.testingplayground.presentation.contract.TodoEvent
import com.example.testingplayground.presentation.contract.TodoState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TodoViewModel @Inject constructor(
    private val getTodosUseCase: GetTodosUseCase,
    private val addTodoUseCase: AddTodoUseCase,
    private val toggleTodoUseCase: ToggleTodoUseCase,
    private val deleteTodoUseCase: DeleteTodoUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(TodoState())
    val state: StateFlow<TodoState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<TodoEffect>()
    val effect: SharedFlow<TodoEffect> = _effect.asSharedFlow()

    init {
        loadTodos()
    }

    fun onEvent(event: TodoEvent) {
        when (event) {
            is TodoEvent.OnTitleChanged -> updateTitle(event.title)
            is TodoEvent.OnDescriptionChanged -> updateDescription(event.description)
            is TodoEvent.OnAddTodo -> addTodo()
            is TodoEvent.OnToggleTodo -> toggleTodo(event.id)
            is TodoEvent.OnDeleteTodo -> deleteTodo(event.id)
            is TodoEvent.OnToggleFilter -> toggleFilter()
        }
    }

    private fun loadTodos() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            getTodosUseCase(_state.value.showOnlyIncomplete).collect { todos ->
                _state.update { it.copy(todos = todos, isLoading = false) }
            }
        }
    }

    private fun updateTitle(title: String) {
        _state.update { it.copy(titleInput = title) }
    }

    private fun updateDescription(description: String) {
        _state.update { it.copy(descriptionInput = description) }
    }

    private fun addTodo() {
        viewModelScope.launch {
            val title = _state.value.titleInput
            val description = _state.value.descriptionInput

            addTodoUseCase(title, description)
                .onSuccess {
                    _state.update { it.copy(titleInput = "", descriptionInput = "") }
                    _effect.emit(TodoEffect.TodoAdded)
                }
                .onFailure { error ->
                    _effect.emit(TodoEffect.ShowError(error.message ?: "추가 실패"))
                }
        }
    }

    private fun toggleTodo(id: Long) {
        viewModelScope.launch {
            toggleTodoUseCase(id).onFailure { error ->
                _effect.emit(TodoEffect.ShowError(error.message ?: "토글 실패"))
            }
        }
    }

    private fun deleteTodo(id: Long) {
        viewModelScope.launch {
            deleteTodoUseCase(id).onFailure { error ->
                _effect.emit(TodoEffect.ShowError(error.message ?: "삭제 실패"))
            }
        }
    }

    private fun toggleFilter() {
        _state.update { it.copy(showOnlyIncomplete = !it.showOnlyIncomplete) }
        loadTodos()
    }
}
