package com.example.testingplayground.domain.usecase

import com.example.testingplayground.domain.model.Todo
import com.example.testingplayground.domain.repository.TodoRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/*
=====================================================================
AddTodoUseCase 단위 테스트
=====================================================================

【이 테스트에서 새로 배우는 MockK 기능】

▸ coEvery { } vs every { }
  - every { }: 일반 함수 stubbing
  //테스트 스텁(Test Stub)은 테스트 호출 중 테스트 스텁은 테스트 중에 만들어진 호출에 대해 미리 준비된 답변을 제공하는 것

  - coEvery { }: suspend 함수 stubbing (co = coroutine)
  - 사용법은 동일, suspend 여부만 다름

▸ coVerify { } vs verify { }
  - verify { }: 일반 함수가 호출되었는지 검증
  - coVerify { }: suspend 함수가 호출되었는지 검증
  - exactly = N: 정확히 N번 호출되었는지 확인

▸ slot<T>() - 인자 캡처
  - Mock 메서드에 전달된 인자를 캡처하여 검증
  - capture(slot): 인자를 slot에 저장
  - slot.captured: 캡처된 값 접근

▸ Result<T> 타입 테스트
  - Kotlin 표준 라이브러리의 성공/실패 래퍼
  - Result.success(value): 성공 결과
  - Result.failure(exception): 실패 결과
  - isSuccess, isFailure: 결과 확인
  - getOrNull(), exceptionOrNull(): 값/예외 추출

【테스트 대상】
- AddTodoUseCase: Todo 추가 비즈니스 로직
- 입력 검증 (제목 길이 2~100자)
- Repository 호출 및 결과 반환
*/
class AddTodoUseCaseTest {

    // ═══════════════════════════════════════════════════════════════
    // 테스트 대상과 의존성
    // ═══════════════════════════════════════════════════════════════

    private lateinit var repository: TodoRepository
    private lateinit var useCase: AddTodoUseCase

    /*
    @BeforeEach - 각 테스트 전 Mock과 UseCase 초기화

    【중요】
    - Mock은 매 테스트마다 새로 생성
    - 이전 테스트의 coEvery/coVerify 설정이 영향 주지 않음
    */
    @BeforeEach
    fun setUp() {
        repository = mockk()
        useCase = AddTodoUseCase(repository)
    }

    // ═══════════════════════════════════════════════════════════════
    // 테스트 케이스 1: 유효한 제목으로 Todo 추가 성공
    // ═══════════════════════════════════════════════════════════════

    /*
    테스트: 정상적인 제목으로 Todo 추가 시 성공

    【이 테스트의 핵심: slot을 이용한 인자 캡처】

    왜 slot이 필요한가?
    - UseCase 내부에서 Todo 객체를 생성하여 Repository에 전달
    - 그 Todo 객체의 내용을 검증하고 싶음
    - slot으로 캡처하면 전달된 객체를 꺼내서 검증 가능
    */
    @Test
    @DisplayName("유효한 제목으로 Todo를 추가한다")
    fun `adds todo with valid title`() = runTest {
        // ─────────────────────────────────────────────────────────────
        // Given: slot 생성 및 Mock 동작 정의
        // ─────────────────────────────────────────────────────────────

        /*
        slot<Todo>() - 인자 캡처 슬롯 생성

        【동작 원리】
        1. slot<T>()로 빈 슬롯 생성
        2. coEvery에서 capture(slot)으로 인자 캡처 설정
        3. Mock 메서드가 호출되면 인자가 슬롯에 저장됨
        4. slot.captured로 저장된 값 접근

        【사용 예시】
        val todoSlot = slot<Todo>()
        coEvery { repository.addTodo(capture(todoSlot)) } returns 1L
        // ... UseCase 호출 ...
        assertEquals("제목", todoSlot.captured.title)  // 캡처된 값 검증
        */
        val todoSlot = slot<Todo>()

        /*
        coEvery { repository.addTodo(capture(todoSlot)) } returns 1L

        【분해 설명】
        - coEvery { }: suspend 함수의 동작 정의
        - repository.addTodo(...): 이 메서드가 호출되면
        - capture(todoSlot): 전달된 Todo를 todoSlot에 저장
        - returns 1L: 메서드는 1L을 반환

        【결과】
        repository.addTodo(anyTodo) 호출 시
        → anyTodo가 todoSlot.captured에 저장됨
        → 반환값은 1L (새로 생성된 Todo의 ID)
        */
        coEvery { repository.addTodo(capture(todoSlot)) } returns 1L

        // ─────────────────────────────────────────────────────────────
        // When: UseCase 실행
        // ─────────────────────────────────────────────────────────────

        /*
        UseCase 호출
        - 내부에서 Todo 객체를 생성하고 repository.addTodo() 호출
        - Result<Long>을 반환 (성공 시 새 Todo의 ID)
        */
        val result = useCase("새 할일", "설명")

        // ─────────────────────────────────────────────────────────────
        // Then: 결과 및 캡처된 인자 검증
        // ─────────────────────────────────────────────────────────────

        /*
        Result 타입 검증

        【Result<T> 주요 메서드】
        - isSuccess: 성공 여부 (Boolean)
        - isFailure: 실패 여부 (Boolean)
        - getOrNull(): 성공시 값, 실패시 null
        - exceptionOrNull(): 실패시 예외, 성공시 null
        - getOrThrow(): 성공시 값, 실패시 예외 throw
        - getOrDefault(default): 성공시 값, 실패시 default
        */
        assertTrue(result.isSuccess)  // 성공했는지 확인
        assertEquals(1L, result.getOrNull())  // 반환된 ID 확인

        /*
        slot.captured - 캡처된 인자 검증

        【검증 내용】
        - UseCase가 생성한 Todo 객체의 필드값 확인
        - 입력으로 전달한 "새 할일"과 "설명"이 제대로 설정되었는지
        */
        assertEquals("새 할일", todoSlot.captured.title)
        assertEquals("설명", todoSlot.captured.description)
    }

    // ═══════════════════════════════════════════════════════════════
    // 테스트 케이스 2: 제목이 너무 짧으면 실패
    // ═══════════════════════════════════════════════════════════════

    /*
    테스트: 제목이 2글자 미만이면 유효성 검증 실패

    【이 테스트의 핵심: coVerify로 호출 여부 확인】

    유효성 검증 실패 시:
    - Repository.addTodo()가 호출되면 안 됨
    - coVerify(exactly = 0)으로 미호출 확인
    */
    @Test
    @DisplayName("제목이 2글자 미만이면 실패한다")
    fun `fails when title is too short`() = runTest {
        // ─────────────────────────────────────────────────────────────
        // When: 1글자 제목으로 UseCase 호출
        // ─────────────────────────────────────────────────────────────

        /*
        UseCase 호출 (유효하지 않은 입력)
        - "가"는 1글자로 최소 길이(2자) 미달
        - UseCase 내부에서 유효성 검증 후 실패 처리
        */
        val result = useCase("가", "설명")

        // ─────────────────────────────────────────────────────────────
        // Then: 실패 결과 및 Repository 미호출 검증
        // ─────────────────────────────────────────────────────────────

        /*
        Result.isFailure - 실패 결과인지 확인

        【성공/실패 판단】
        - UseCase가 Result.failure(exception)을 반환하면 isFailure = true
        - 유효성 검증 실패 시 IllegalArgumentException 포함
        */
        assertTrue(result.isFailure)

        /*
        result.exceptionOrNull() - 실패 원인 예외 추출

        【사용법】
        - 실패 시: 내부에 저장된 Exception 반환
        - 성공 시: null 반환

        【예외 타입 확인】
        - is 연산자로 예외 타입 검증
        - IllegalArgumentException = 잘못된 인자
        */
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)

        /*
        coVerify(exactly = 0) { repository.addTodo(any()) }

        【분해 설명】
        - coVerify { }: suspend 함수 호출 검증
        - exactly = 0: 정확히 0번 호출되었어야 함 (= 호출 안 됨)
        - any(): 어떤 인자든 상관없이

        【다른 옵션들】
        - exactly = 1: 정확히 1번
        - atLeast = 1: 최소 1번
        - atMost = 3: 최대 3번

        【이 검증의 의미】
        유효성 검증 실패 시 Repository까지 도달하면 안 됨
        → DB 접근 없이 빠르게 실패 응답
        */
        coVerify(exactly = 0) { repository.addTodo(any()) }
    }

    // ═══════════════════════════════════════════════════════════════
    // 테스트 케이스 3: 제목이 너무 길면 실패
    // ═══════════════════════════════════════════════════════════════

    /*
    테스트: 제목이 100글자 초과면 유효성 검증 실패

    【테스트 시나리오】
    - "가".repeat(101) = "가" 101개 = 101글자
    - 최대 길이(100자) 초과
    - Result.failure 반환, Repository 미호출
    */
    @Test
    @DisplayName("제목이 100글자 초과면 실패한다")
    fun `fails when title is too long`() = runTest {
        // When: 101글자 제목으로 호출
        val result = useCase("가".repeat(101), "설명")

        // Then: 실패 확인
        assertTrue(result.isFailure)

        /*
        Repository 미호출 검증

        【any() 매처 설명】
        - MockK의 인자 매처
        - 어떤 타입의 어떤 값이든 매칭
        - "해당 메서드가 어떤 인자로든 호출되었는지" 확인할 때 사용

        【다른 매처들】
        - any<Todo>(): 특정 타입의 any
        - eq(value): 특정 값과 같음
        - match { predicate }: 조건에 맞는 값
        */
        coVerify(exactly = 0) { repository.addTodo(any()) }
    }
}

/*
=====================================================================
【테스트 실행 결과 예시】
=====================================================================

✅ AddTodoUseCaseTest
   ✅ 유효한 제목으로 Todo를 추가한다 (15ms)
   ✅ 제목이 2글자 미만이면 실패한다 (3ms)
   ✅ 제목이 100글자 초과면 실패한다 (2ms)

Tests passed: 3 of 3

=====================================================================
【핵심 학습 포인트 요약】
=====================================================================

1. coEvery vs every
   - suspend 함수는 coEvery { }
   - 일반 함수는 every { }

2. slot<T>()로 인자 캡처
   - capture(slot)으로 캡처 설정
   - slot.captured로 값 접근
   - Mock 내부로 전달된 값 검증에 유용

3. coVerify로 호출 검증
   - exactly = 0: 호출 안 됨 확인
   - exactly = 1: 정확히 1번 호출 확인
   - 부작용 없이 빠른 실패 확인에 사용

4. Result<T> 타입
   - Kotlin 표준 성공/실패 래퍼
   - isSuccess, isFailure로 판단
   - getOrNull(), exceptionOrNull()로 값 추출

=====================================================================
【실제 UseCase 코드 (참고)】
=====================================================================

class AddTodoUseCase(private val repository: TodoRepository) {
    suspend operator fun invoke(title: String, description: String): Result<Long> {
        // 유효성 검증
        if (title.length < 2) {
            return Result.failure(IllegalArgumentException("제목은 2글자 이상"))
        }
        if (title.length > 100) {
            return Result.failure(IllegalArgumentException("제목은 100글자 이하"))
        }

        // Todo 생성 및 저장
        val todo = Todo(title = title, description = description)
        val id = repository.addTodo(todo)
        return Result.success(id)
    }
}
*/


/*

⏺ every vs verify - 핵심 차이

  ┌─────────────────────────────────────────────────────────────────┐
  │  every { }  →  "이렇게 호출되면 이걸 반환해라" (사전 설정)        │
  │  verify { } →  "이렇게 호출됐는지 확인해라" (사후 검증)          │
  └─────────────────────────────────────────────────────────────────┘

  ---
  every / coEvery 상세 설명

  역할: Mock의 동작 정의 (Stubbing)

  // Mock은 빈 껍데기 - 아무 동작도 없음
  val repository = mockk<TodoRepository>()

  // repository.addTodo() 호출하면? → 에러! (동작 정의 안 됨)
  repository.addTodo(todo)  // ❌ 크래시

  // every로 동작 정의
  coEvery { repository.addTodo(any()) } returns 1L

  // 이제 호출 가능
  repository.addTodo(todo)  // ✅ 1L 반환

  왜 1L을 반환?

  // 실제 Repository 코드를 보면:
  interface TodoRepository {
      suspend fun addTodo(todo: Todo): Long  // ← 새로 생성된 ID 반환
  }

  // 실제 구현체:
  class TodoRepositoryImpl : TodoRepository {
      override suspend fun addTodo(todo: Todo): Long {
          val id = database.insert(todo)  // DB에 저장하고
          return id                        // 새 ID 반환 (예: 1, 2, 3...)
      }
  }

  // 테스트에서는 DB가 없으니 가짜 응답 설정:
  coEvery { repository.addTodo(any()) } returns 1L
  //                                            ↑
  //                              "새 Todo의 ID가 1이라고 가정"

  ---
  every 문법 분해

  coEvery { repository.addTodo(capture(todoSlot)) } returns 1L
     │              │              │                    │
     │              │              │                    └── 반환값
     │              │              └── 인자 캡처 (선택사항)
     │              └── 이 메서드가 호출되면
     └── suspend 함수용 (일반 함수는 every)

  every vs coEvery

  // 일반 함수
  every { repository.getTodos() } returns flowOf(listOf())

  // suspend 함수 (co = coroutine)
  coEvery { repository.addTodo(todo) } returns 1L

  다양한 반환 설정

  // 단순 값 반환
  coEvery { repository.addTodo(any()) } returns 1L

  // 예외 발생
  coEvery { repository.addTodo(any()) } throws Exception("DB 에러")

  // 호출마다 다른 값
  coEvery { repository.addTodo(any()) } returnsMany listOf(1L, 2L, 3L)

  // 조건부 반환
  coEvery { repository.addTodo(match { it.title == "특별" }) } returns 999L
  coEvery { repository.addTodo(any()) } returns 1L  // 나머지

  ---
  verify / coVerify 상세 설명

  역할: Mock이 호출되었는지 검증 (사후 확인)

  // Given
  coEvery { repository.addTodo(any()) } returns 1L

  // When
  useCase("새 할일", "설명")  // 내부에서 repository.addTodo() 호출

  // Then - 호출 검증
  coVerify { repository.addTodo(any()) }  // ✅ 호출됨 확인

  verify 옵션들

  // 호출됨 (1번 이상)
  coVerify { repository.addTodo(any()) }

  // 정확히 N번
  coVerify(exactly = 1) { repository.addTodo(any()) }
  coVerify(exactly = 0) { repository.addTodo(any()) }  // 호출 안 됨

  // 최소/최대
  coVerify(atLeast = 1) { repository.addTodo(any()) }
  coVerify(atMost = 3) { repository.addTodo(any()) }

  // 순서 검증
  verifyOrder {
      repository.addTodo(any())
      repository.getTodos()
  }

  ---
  실제 테스트 흐름 예시

  @Test
  fun `Todo 추가 테스트`() = runTest {
      // ═══════════════════════════════════════════════
      // 1단계: STUBBING (every) - Mock 동작 정의
      // ═══════════════════════════════════════════════
      val todoSlot = slot<Todo>()
      coEvery { repository.addTodo(capture(todoSlot)) } returns 1L
      //        └─────────────────────────────────────────────────┘
      //        "addTodo가 호출되면 인자를 캡처하고 1L 반환해라"

      // ═══════════════════════════════════════════════
      // 2단계: 실행 - 테스트 대상 호출
      // ═══════════════════════════════════════════════
      val result = useCase("새 할일", "설명")
      //           └──────────────────────┘
      //           내부에서 repository.addTodo(todo) 호출됨
      //           → 1L 반환받음

      // ═══════════════════════════════════════════════
      // 3단계: 검증
      // ═══════════════════════════════════════════════

      // 결과 검증
      assertEquals(1L, result.getOrNull())

      // 캡처된 인자 검증
      assertEquals("새 할일", todoSlot.captured.title)

      // 호출 검증 (verify)
      coVerify(exactly = 1) { repository.addTodo(any()) }
      //       └───────────────────────────────────────┘
      //       "addTodo가 정확히 1번 호출되었는지 확인"
  }

  ---
  타임라인으로 보면

  시간 →

  [테스트 시작]
       │
       │  coEvery { ... } returns 1L     ← "나중에 이렇게 응답해"
       │         │
       │         ▼
       │  useCase("새 할일", "설명")
       │         │
       │         ▼
       │  [UseCase 내부]
       │    repository.addTodo(todo)     ← Mock 호출됨
       │         │
       │         ▼
       │    returns 1L                   ← 설정한 대로 반환
       │         │
       │         ▼
       │  coVerify { ... }               ← "진짜 호출됐나 확인"
       │
  [테스트 종료]

  ---
  요약

  | 기능     | 시점    | 목적    | 예시          |
  |--------|-------|-------|-------------|
  | every  | 테스트 전 | 동작 정의 | returns 1L  |
  | verify | 테스트 후 | 호출 확인 | exactly = 1 |


 */
