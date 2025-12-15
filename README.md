# Android Testing Playground

Android 테스트 전략을 학습하고 실습하기 위한 샘플 프로젝트다.

Todo 앱을 기반으로 Unit Test, Integration Test, UI Test를 단계별로 구현했다. Clean Architecture와 MVI 패턴을 적용하여 테스트 가능한 구조를 갖추었다.

---

## 기술 스택

| 구분 | 기술 |
|------|------|
| Language | Kotlin |
| UI | Jetpack Compose, Material3 |
| Architecture | Clean Architecture, MVI |
| DI | Hilt |
| Async | Coroutines, Flow |
| Test | JUnit5, MockK, Turbine |

---

## 프로젝트 구조

```
app/src/
├── main/
│   └── java/.../testingplayground/
│       ├── data/
│       │   ├── datasource/       # 데이터 소스
│       │   └── repository/       # Repository 구현체
│       ├── domain/
│       │   ├── model/            # 도메인 모델
│       │   ├── repository/       # Repository 인터페이스
│       │   └── usecase/          # UseCase
│       ├── presentation/
│       │   ├── contract/         # MVI Contract (State, Intent, Effect)
│       │   ├── ui/               # Compose UI
│       │   └── viewmodel/        # ViewModel
│       └── di/                   # Hilt 모듈
│
├── test/                         # Unit & Integration Test
│   ├── domain/usecase/           # UseCase 테스트
│   ├── presentation/viewmodel/   # ViewModel 테스트
│   ├── integration/              # 통합 테스트
│   ├── fake/                     # Fake 구현체
│   └── util/                     # 테스트 유틸
│
└── androidTest/                  # UI Test
    └── presentation/ui/          # Compose UI 테스트
```

---

## 테스트 종류

### 1. Unit Test

개별 클래스의 동작을 검증한다.

**UseCase 테스트**
- `GetTodosUseCaseTest`: Todo 목록 조회 로직 검증
- `AddTodoUseCaseTest`: Todo 추가 로직 검증

**ViewModel 테스트**
- `TodoViewModelTest`: 상태 변화 및 Intent 처리 검증

```kotlin
@Test
fun `Todo 추가 시 목록에 반영된다`() = runTest {
    // Given
    val newTodo = "새로운 할 일"

    // When
    viewModel.processIntent(TodoIntent.AddTodo(newTodo))

    // Then
    val state = viewModel.state.value
    assertThat(state.todos).anyMatch { it.title == newTodo }
}
```

### 2. Integration Test

여러 컴포넌트 간의 상호작용을 검증한다.

**UseCase + Repository 통합 테스트**
- `TodoUseCaseIntegrationTest`: UseCase와 Repository 연동 검증

**ViewModel + UseCase 통합 테스트**
- `TodoViewModelIntegrationTest`: 실제 UseCase를 주입한 ViewModel 테스트

### 3. UI Test

사용자 관점에서 화면 동작을 검증한다.

**Compose UI 테스트**
- `TodoScreenTest`: Todo 화면의 사용자 인터랙션 검증

```kotlin
@Test
fun Todo_추가_버튼_클릭_시_다이얼로그가_표시된다() {
    composeTestRule.onNodeWithContentDescription("Add Todo")
        .performClick()

    composeTestRule.onNodeWithText("할 일 추가")
        .assertIsDisplayed()
}
```

---

## 테스트 실행

```bash
# Unit Test + Integration Test
./gradlew test

# UI Test (에뮬레이터 필요)
./gradlew connectedAndroidTest

# 전체 테스트
./gradlew test connectedAndroidTest
```

---

## 테스트 전략

### Fake vs Mock

| 상황 | 선택 |
|------|------|
| Repository 테스트 | Fake 사용 (상태 유지 필요) |
| UseCase 단위 테스트 | Mock 사용 (빠른 검증) |
| Integration 테스트 | Fake 사용 (실제 동작 검증) |

### 테스트 피라미드

```
      /\
     /UI\        <- 적음, 느림, 높은 신뢰도
    /----\
   /Integ-\      <- 적당함
  / ration \
 /----------\
/  Unit Test \   <- 많음, 빠름, 낮은 비용
--------------
```

---

## 핵심 포인트

- **테스트 가능한 구조**: Clean Architecture로 레이어 분리, 의존성 주입으로 테스트 용이성 확보
- **MVI 패턴**: 단방향 데이터 흐름으로 상태 변화 추적 및 테스트 단순화
- **Fake Repository**: 테스트 시 실제 데이터 소스 없이 동작 검증 가능
- **Turbine**: Flow 테스트를 위한 라이브러리로 비동기 상태 검증

---

## 참고

- [Android Testing Guide](https://developer.android.com/training/testing)
- [Compose Testing](https://developer.android.com/jetpack/compose/testing)
- [MockK](https://mockk.io/)
- [Turbine](https://github.com/cashapp/turbine)
