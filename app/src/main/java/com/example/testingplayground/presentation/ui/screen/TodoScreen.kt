package com.example.testingplayground.presentation.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.testingplayground.presentation.ui.TestTag
import com.example.testingplayground.presentation.contract.TodoEffect
import com.example.testingplayground.presentation.contract.TodoEvent
import com.example.testingplayground.presentation.ui.component.TodoItem
import com.example.testingplayground.presentation.viewmodel.TodoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(viewModel: TodoViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is TodoEffect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is TodoEffect.TodoAdded -> {
                    Toast.makeText(context, "할 일이 추가되었습니다", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Testing Playground") },
                actions = {
                    FilterChip(
                        selected = state.showOnlyIncomplete,
                        onClick = { viewModel.onEvent(TodoEvent.OnToggleFilter) },
                        label = { Text("미완료만") },
                        leadingIcon = { Icon(Icons.Default.FilterList, null) },
                        modifier = Modifier.testTag(TestTag.FILTER_CHIP)
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // 입력 영역
            OutlinedTextField(
                value = state.titleInput,
                onValueChange = { viewModel.onEvent(TodoEvent.OnTitleChanged(it)) },
                label = { Text("할 일 (2글자 이상)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTag.TITLE_INPUT),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = state.descriptionInput,
                onValueChange = { viewModel.onEvent(TodoEvent.OnDescriptionChanged(it)) },
                label = { Text("설명 (선택)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTag.DESCRIPTION_INPUT)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.onEvent(TodoEvent.OnAddTodo) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTag.ADD_BUTTON),
                enabled = state.titleInput.length >= 2
            ) {
                Text("추가")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 통계
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "전체: ${state.totalCount}",
                    modifier = Modifier.testTag(TestTag.TOTAL_COUNT)
                )
                Text(
                    text = "완료: ${state.completedCount}",
                    modifier = Modifier.testTag(TestTag.COMPLETED_COUNT)
                )
                Text(
                    text = "미완료: ${state.incompleteCount}",
                    modifier = Modifier.testTag(TestTag.INCOMPLETE_COUNT)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 목록
            if (state.todos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(TestTag.EMPTY_STATE),
                    contentAlignment = Alignment.Center
                ) {
                    Text("할 일을 추가해보세요!")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.testTag(TestTag.TODO_LIST),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.todos, key = { it.id }) { todo ->
                        TodoItem(
                            todo = todo,
                            onToggle = { viewModel.onEvent(TodoEvent.OnToggleTodo(todo.id)) },
                            onDelete = { viewModel.onEvent(TodoEvent.OnDeleteTodo(todo.id)) }
                        )
                    }
                }
            }
        }
    }
}
