package com.example.testingplayground.presentation.ui

/**
 * =====================================================================
 * TestTag - Compose UI 테스트를 위한 상수 정의
 * =====================================================================
 *
 * 【왜 필요한가?】
 *
 * Compose UI 테스트에서 요소를 찾을 때 testTag를 사용한다.
 * 문자열 리터럴 대신 상수를 사용하면:
 * - 오타 방지
 * - IDE 자동완성 지원
 * - 변경 시 한 곳만 수정
 *
 * =====================================================================
 * 【사용법】
 * =====================================================================
 *
 * 프로덕션 코드:
 * ```kotlin
 * OutlinedTextField(
 *     modifier = Modifier.testTag(TestTag.TITLE_INPUT),
 *     ...
 * )
 * ```
 *
 * 테스트 코드:
 * ```kotlin
 * composeTestRule
 *     .onNodeWithTag(TestTag.TITLE_INPUT)
 *     .performTextInput("새 할일")
 * ```
 *
 * =====================================================================
 * 【네이밍 컨벤션】
 * =====================================================================
 *
 * - SCREAMING_SNAKE_CASE 사용
 * - 컴포넌트 타입 포함 (INPUT, BUTTON, LIST 등)
 * - 화면별로 그룹화 가능 (TODO_TITLE_INPUT, SEARCH_INPUT 등)
 */
object TestTag {

    // ═══════════════════════════════════════════════════════════════
    // TodoScreen
    // ═══════════════════════════════════════════════════════════════

    /** 제목 입력 필드 */
    const val TITLE_INPUT = "title_input"

    /** 설명 입력 필드 */
    const val DESCRIPTION_INPUT = "description_input"

    /** 추가 버튼 */
    const val ADD_BUTTON = "add_button"

    /** Todo 목록 */
    const val TODO_LIST = "todo_list"

    /** 빈 상태 텍스트 */
    const val EMPTY_STATE = "empty_state"

    /** 미완료만 보기 필터 칩 */
    const val FILTER_CHIP = "filter_chip"

    // ═══════════════════════════════════════════════════════════════
    // TodoItem
    // ═══════════════════════════════════════════════════════════════

    /** Todo 아이템 (index 파라미터로 구분) */
    fun todoItem(id: Long) = "todo_item_$id"

    /** 체크박스 (id 파라미터로 구분) */
    fun todoCheckbox(id: Long) = "todo_checkbox_$id"

    /** 삭제 버튼 (id 파라미터로 구분) */
    fun todoDeleteButton(id: Long) = "todo_delete_$id"

    // ═══════════════════════════════════════════════════════════════
    // 통계 영역
    // ═══════════════════════════════════════════════════════════════

    /** 전체 개수 텍스트 */
    const val TOTAL_COUNT = "total_count"

    /** 완료 개수 텍스트 */
    const val COMPLETED_COUNT = "completed_count"

    /** 미완료 개수 텍스트 */
    const val INCOMPLETE_COUNT = "incomplete_count"
}

/**
 * =====================================================================
 * 【테스트에서의 활용 예시】
 * =====================================================================
 *
 * @Test
 * fun `Todo 추가 후 목록에 표시된다`() {
 *     // Given: 제목 입력
 *     composeTestRule
 *         .onNodeWithTag(TestTag.TITLE_INPUT)
 *         .performTextInput("새 할일")
 *
 *     // When: 추가 버튼 클릭
 *     composeTestRule
 *         .onNodeWithTag(TestTag.ADD_BUTTON)
 *         .performClick()
 *
 *     // Then: 목록에 표시됨
 *     composeTestRule
 *         .onNodeWithText("새 할일")
 *         .assertIsDisplayed()
 * }
 *
 * @Test
 * fun `체크박스 클릭 시 완료 상태로 변경된다`() {
 *     // Given: Todo가 있는 상태
 *     ...
 *
 *     // When: 체크박스 클릭
 *     composeTestRule
 *         .onNodeWithTag(TestTag.todoCheckbox(1))
 *         .performClick()
 *
 *     // Then: 체크 상태 확인
 *     composeTestRule
 *         .onNodeWithTag(TestTag.todoCheckbox(1))
 *         .assertIsOn()
 * }
 */
