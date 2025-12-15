package com.example.testingplayground.di

import com.example.testingplayground.domain.repository.TodoRepository
import com.example.testingplayground.fake.FakeTodoRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * =====================================================================
 * TestModule - 테스트용 의존성 모듈
 * =====================================================================
 *
 * 【@TestInstallIn 설명】
 *
 * - 프로덕션 모듈(AppModule)을 테스트용 모듈로 교체
 * - replaces = [AppModule::class]: AppModule 대신 이 모듈 사용
 * - 테스트 시 FakeRepository가 주입됨
 *
 * 【동작 원리】
 *
 * 프로덕션:
 *   AppModule → TodoRepositoryImpl 바인딩
 *
 * 테스트:
 *   TestModule → FakeTodoRepository 바인딩 (AppModule 무시)
 *
 * 【장점】
 * - 테스트 코드에서 별도 설정 없이 자동으로 Fake 주입
 * - UseCase, ViewModel 코드 변경 없이 테스트 가능
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [AppModule::class]  // 프로덕션 모듈 교체
)
object TestModule {

    /**
     * FakeTodoRepository를 TodoRepository로 제공
     *
     * 【@Singleton】
     * - 테스트 내에서 동일한 인스턴스 공유
     * - 여러 UseCase가 같은 데이터 접근
     */
    @Provides
    @Singleton
    fun provideTodoRepository(): TodoRepository {
        return FakeTodoRepository()
    }

    /**
     * FakeTodoRepository 직접 접근용
     *
     * 【용도】
     * - 테스트에서 헬퍼 메서드 사용 (clear, addAll 등)
     * - 테스트 데이터 직접 조작
     */
    @Provides
    @Singleton
    fun provideFakeTodoRepository(repository: TodoRepository): FakeTodoRepository {
        return repository as FakeTodoRepository
    }
}
