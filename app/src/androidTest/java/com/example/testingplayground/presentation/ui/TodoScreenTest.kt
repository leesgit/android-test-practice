package com.example.testingplayground.presentation.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.testingplayground.domain.model.Todo
import com.example.testingplayground.presentation.ui.component.TodoItem
import org.junit.Rule
import org.junit.Test

/*
=====================================================================
TodoScreen Compose UI 테스트
=====================================================================

Compose UI Test는 실제 디바이스 또는 에뮬레이터에서 실행되며,
사용자 관점의 플로우를 검증한다. 작성 비용이 크기 때문에,
가장 중요한 시나리오 위주로 선별해서 작성하는 것이 좋다.

【테스트 대상】
- TodoItem 컴포넌트: 체크박스, 삭제 버튼
- 입력 필드: 제목, 설명
- 버튼: 추가 버튼 활성화/비활성화
- 목록: 빈 상태, 아이템 표시
*/
class TodoScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ═══════════════════════════════════════════════════════════════
    // TodoItem 컴포넌트 테스트
    // ═══════════════════════════════════════════════════════════════

    // Todo 아이템이 제목을 표시한다.
    @Test
    fun todoItem_displaysTitle() {
        // Given
        val todo = Todo(id = 1, title = "테스트 할일", description = "설명")

        // When
        composeTestRule.setContent {
            TodoItem(todo = todo, onToggle = {}, onDelete = {})
        }

        // Then
        composeTestRule
            .onNodeWithText("테스트 할일")
            .assertIsDisplayed()
    }

    // 설명이 있으면 표시한다.
    @Test
    fun todoItem_displaysDescription() {
        // Given
        val todo = Todo(id = 1, title = "할일", description = "상세 설명이다")

        // When
        composeTestRule.setContent {
            TodoItem(todo = todo, onToggle = {}, onDelete = {})
        }

        // Then
        composeTestRule
            .onNodeWithText("상세 설명이다")
            .assertIsDisplayed()
    }

    // 체크박스 클릭 시 onToggle이 호출된다.
    @Test
    fun todoItem_checkboxClick_triggersOnToggle() {
        // Given
        var toggleCalled = false
        val todo = Todo(id = 1, title = "할일")

        composeTestRule.setContent {
            TodoItem(
                todo = todo,
                onToggle = { toggleCalled = true },
                onDelete = {}
            )
        }

        // When
        composeTestRule
            .onNodeWithTag(TestTag.todoCheckbox(1))
            .performClick()

        // Then
        assert(toggleCalled)
    }

    // 삭제 버튼 클릭 시 onDelete가 호출된다.
    @Test
    fun todoItem_deleteButtonClick_triggersOnDelete() {
        // Given
        var deleteCalled = false
        val todo = Todo(id = 1, title = "삭제할 항목")

        composeTestRule.setContent {
            TodoItem(
                todo = todo,
                onToggle = {},
                onDelete = { deleteCalled = true }
            )
        }

        // When
        composeTestRule
            .onNodeWithTag(TestTag.todoDeleteButton(1))
            .performClick()

        // Then
        assert(deleteCalled)
    }

    // 완료된 Todo는 체크박스가 선택 상태다.
    @Test
    fun todoItem_completedTodo_hasCheckedCheckbox() {
        // Given
        val completedTodo = Todo(id = 1, title = "완료된 할일", isCompleted = true)

        // When
        composeTestRule.setContent {
            TodoItem(todo = completedTodo, onToggle = {}, onDelete = {})
        }

        // Then
        composeTestRule
            .onNodeWithTag(TestTag.todoCheckbox(1))
            .assertIsOn()
    }

    // 미완료 Todo는 체크박스가 미선택 상태다.
    @Test
    fun todoItem_incompleteTodo_hasUncheckedCheckbox() {
        // Given
        val incompleteTodo = Todo(id = 1, title = "미완료 할일", isCompleted = false)

        // When
        composeTestRule.setContent {
            TodoItem(todo = incompleteTodo, onToggle = {}, onDelete = {})
        }

        // Then
        composeTestRule
            .onNodeWithTag(TestTag.todoCheckbox(1))
            .assertIsOff()
    }
}

/*
=====================================================================
【테스트 실행 방법】
=====================================================================

1. Android Studio에서 실행:
   - 테스트 클래스/메서드 옆 녹색 화살표 클릭
   - 또는 우클릭 → Run 'TodoScreenTest'

2. Gradle 명령어:
   ./gradlew connectedAndroidTest

3. 필요 조건:
   - 에뮬레이터 또는 실제 기기 연결
   - debugImplementation 'androidx.compose.ui:ui-test-manifest'

=====================================================================
【TestTag 사용의 장점】
=====================================================================

❌ 문자열 리터럴 (오타 위험)
```kotlin
onNodeWithTag("todo_checkbox_1").performClick()
onNodeWithTag("todo_checkbox_2").performClick()  // 오타 가능!
```

✅ TestTag 상수 (안전)
```kotlin
onNodeWithTag(TestTag.todoCheckbox(1)).performClick()
onNodeWithTag(TestTag.todoCheckbox(2)).performClick()
```
*/
