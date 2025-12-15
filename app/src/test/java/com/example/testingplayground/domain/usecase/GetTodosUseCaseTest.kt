package com.example.testingplayground.domain.usecase

import app.cash.turbine.test
import com.example.testingplayground.domain.model.Todo
import com.example.testingplayground.domain.repository.TodoRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/*
=====================================================================
GetTodosUseCase 단위 테스트
=====================================================================

【사용 라이브러리 설명】

▸ JUnit 5 (Jupiter) - org.junit.jupiter:junit-jupiter:5.10.0
  - Java/Kotlin 표준 테스트 프레임워크
  - @Test: 테스트 메서드 표시
  - @BeforeEach: 각 테스트 전 실행되는 설정 메서드
  - @DisplayName: 테스트 이름을 한글 등 가독성 좋게 지정
  - Assertions.assertEquals(): 예상값과 실제값 비교

▸ MockK - io.mockk:mockk:1.13.8
  - Kotlin 전용 Mocking 라이브러리 (Mockito보다 Kotlin 친화적)
  - mockk<T>(): Mock 객체 생성 (인터페이스/클래스 가짜 구현체)
  - every { }: 일반 함수의 동작(return값) 정의
  - returns: 호출시 반환할 값 지정

▸ Coroutines Test - kotlinx-coroutines-test:1.7.3
  - 코루틴 테스트 유틸리티
  - runTest { }: 테스트용 코루틴 스코프 제공, suspend 함수 테스트 가능
  - flowOf(): 테스트용 간단한 Flow 생성

▸ Turbine - app.cash.turbine:turbine:1.0.0
  - Flow 테스트 전용 라이브러리 (Cash App 제작)
  - test { }: Flow를 구독하고 emit된 값들을 순차적으로 검증
  - awaitItem(): 다음 emit된 아이템을 대기하고 반환
  - awaitComplete(): Flow 완료를 대기
  - awaitError(): 에러 발생을 대기

【테스트 구조: Given-When-Then 패턴】
  - Given: 테스트 조건/데이터 준비
  - When: 테스트 대상 실행
  - Then: 결과 검증
*/
class GetTodosUseCaseTest {

    // ═══════════════════════════════════════════════════════════════
    // 테스트 대상과 의존성 선언
    // ═══════════════════════════════════════════════════════════════

    /*
    Mock 객체: 실제 Repository를 대체하는 가짜 객체
    - 실제 DB 연결 없이 원하는 값을 반환하도록 설정 가능
    - 테스트를 빠르고 독립적으로 만듦
    */
    private lateinit var repository: TodoRepository

    /*
    테스트 대상 (SUT: System Under Test)
    - UseCase는 비즈니스 로직을 담당
    - Repository에 의존하므로 Mock 주입 필요
    */
    private lateinit var useCase: GetTodosUseCase

    // ═══════════════════════════════════════════════════════════════
    // 테스트 설정
    // ═══════════════════════════════════════════════════════════════

    /*
    @BeforeEach - 각 @Test 메서드 실행 전에 호출됨

    【동작 과정】
    1. mockk<TodoRepository>() 호출
       → TodoRepository 인터페이스의 가짜 구현체 생성
    2. GetTodosUseCase에 Mock 주입
       → UseCase가 Mock Repository를 사용하게 됨

    【왜 매번 새로 생성하나?】
    - 테스트 간 격리: 이전 테스트의 설정이 다음 테스트에 영향 X
    - Mock 상태 초기화: every { } 설정도 초기화됨
    */
    @BeforeEach
    fun setUp() {
        repository = mockk()  // Mock 객체 생성
        useCase = GetTodosUseCase(repository)  // 의존성 주입
    }

    // ═══════════════════════════════════════════════════════════════
    // 테스트 케이스 1: 전체 Todo 목록 반환
    // ═══════════════════════════════════════════════════════════════

    /*
    테스트: 전체 Todo 목록을 최신순으로 정렬해서 반환

    【runTest { } 설명】
    - 테스트용 코루틴 스코프를 제공
    - 내부에서 suspend 함수 호출 가능
    - 가상 시간 제어로 delay() 등을 즉시 실행

    【테스트 시나리오】
    1. Repository가 2개의 Todo를 반환하도록 설정
    2. UseCase 실행 (showOnlyIncomplete = false)
    3. 결과: 2개 모두 반환, 최신순 정렬 확인
    */
    @Test
    @DisplayName("전체 Todo 목록을 반환한다")
    fun `returns all todos`() = runTest {
        // ─────────────────────────────────────────────────────────────
        // Given: 테스트 데이터 및 Mock 동작 정의
        // ─────────────────────────────────────────────────────────────
        val todos = listOf(
            Todo(1, "할일1", createdAt = 1000),  // 이전에 생성됨
            Todo(2, "할일2", createdAt = 2000)   // 나중에 생성됨
        )

        /*
        every { repository.getTodos() } returns flowOf(todos)

        【분해 설명】
        - every { ... }: MockK의 stubbing 문법
        - repository.getTodos(): 이 메서드가 호출되면
        - returns: 다음 값을 반환하라
        - flowOf(todos): todos 리스트를 emit하는 Flow 생성

        【결과】
        repository.getTodos() 호출시
        → Flow<List<Todo>>가 반환됨
        → collect하면 todos 리스트가 emit됨
        */
        every { repository.getTodos() } returns flowOf(todos)

        // ─────────────────────────────────────────────────────────────
        // When & Then: UseCase 실행 및 결과 검증
        // ─────────────────────────────────────────────────────────────

        /*
        useCase(showOnlyIncomplete = false).test { }

        【Turbine test { } 설명】
        1. UseCase가 반환하는 Flow를 구독 시작
        2. Flow에서 값이 emit될 때까지 대기
        3. emit된 값을 순서대로 검증
        4. Flow가 완료되는지 확인

        【왜 Turbine을 쓰나?】
        - Flow 테스트는 비동기라 타이밍 이슈 발생 가능
        - Turbine이 대기/타임아웃/검증을 깔끔하게 처리
        - first(), toList() 대신 세밀한 제어 가능
        */
        useCase(showOnlyIncomplete = false).test {
            /*
            awaitItem(): 다음 emit을 기다리고 값을 반환

            - Flow가 값을 emit할 때까지 suspend
            - 타임아웃 시 테스트 실패 (기본 1초)
            - 반환된 값으로 assertions 수행
            */
            val result = awaitItem()

            // 검증: 2개 모두 반환됨
            assertEquals(2, result.size)

            // 검증: 최신순 정렬 (createdAt이 큰 게 먼저)
            // result[0]은 "할일2" (createdAt = 2000)
            assertEquals("할일2", result[0].title)

            /*
            awaitComplete(): Flow 완료를 기다림

            - flowOf()는 값 emit 후 자동 완료
            - 완료되지 않으면 테스트 실패
            - Hot Flow(StateFlow 등)는 완료 안 되므로 주의
            */
            awaitComplete()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 테스트 케이스 2: 미완료 Todo만 필터링
    // ═══════════════════════════════════════════════════════════════

    /*
    테스트: showOnlyIncomplete = true일 때 미완료 항목만 반환

    【테스트 시나리오】
    1. 완료된 Todo 1개 + 미완료 Todo 1개 준비
    2. UseCase에 showOnlyIncomplete = true 전달
    3. 결과: 미완료 Todo 1개만 반환

    【이 테스트의 목적】
    - UseCase 내부의 필터링 로직 검증
    - isCompleted 플래그에 따른 분기 확인
    */
    @Test
    @DisplayName("미완료 Todo만 필터링한다")
    fun `filters incomplete todos only`() = runTest {
        // Given: 완료/미완료 섞인 데이터
        val todos = listOf(
            Todo(1, "완료됨", isCompleted = true),   // 필터링되어야 함
            Todo(2, "미완료", isCompleted = false)   // 결과에 포함되어야 함
        )
        every { repository.getTodos() } returns flowOf(todos)

        // When & Then
        useCase(showOnlyIncomplete = true).test {
            val result = awaitItem()

            // 검증: 1개만 반환 (미완료 것만)
            assertEquals(1, result.size)
            assertEquals("미완료", result[0].title)

            awaitComplete()
        }
    }
}

/*
=====================================================================
【테스트 실행 결과 예시】
=====================================================================

✅ GetTodosUseCaseTest
   ✅ 전체 Todo 목록을 반환한다 (23ms)
   ✅ 미완료 Todo만 필터링한다 (5ms)

Tests passed: 2 of 2

=====================================================================
【핵심 학습 포인트 요약】
=====================================================================

1. Mock 객체 (mockk)
   - 의존성을 가짜로 대체하여 격리된 단위 테스트 가능
   - every { } 로 반환값 정의

2. Flow 테스트 (Turbine)
   - test { } 블록으로 Flow 구독 및 검증
   - awaitItem()으로 값 수신 대기
   - awaitComplete()로 완료 확인

3. 코루틴 테스트 (runTest)
   - suspend 함수를 테스트에서 호출 가능하게 함
   - 가상 시간으로 delay 즉시 처리

4. Given-When-Then 패턴
   - 가독성 높은 테스트 구조
   - 준비 → 실행 → 검증 명확히 분리
*/
