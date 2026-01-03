# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요
Kotlin + Spring Boot + MyBatis를 사용한 데모 애플리케이션으로, 관리자(Admin), 사용자(User), 공지(Notice) 도메인을 포함합니다.

## 주요 명령어

### 빌드 및 실행
- `./gradlew build` - 프로젝트 빌드
- `./gradlew bootRun` - 애플리케이션 실행 (포트 8000)
- `./gradlew spotlessApply` - 코드 포맷팅 자동 적용
- `./gradlew spotlessCheck` - 코드 포맷팅 검사

### Docker
- `docker build -t demo-app .` - Docker 이미지 빌드
- Docker 실행 시 포트 8000 노출

## 아키텍처 구조

### 패키지 구조
- `com.github.bestheroz.demo` - 비즈니스 로직 (Admin, User, Notice 도메인)
- `com.github.bestheroz.standard` - 공통 프레임워크 코드

### 핵심 구성요소
- **MyBatis 설정**: `MyBatisConfig.kt`에서 언더스코어-카멜케이스 변환 및 타입 핸들러 설정
- **보안 설정**: `SecurityConfig.kt`에서 JWT 인증, CORS 설정
- **데이터베이스**: MySQL 사용, Flyway 마이그레이션 파일은 `migration/` 디렉토리

### 인증/인가
- JWT 토큰 기반 인증 시스템
- Access Token 만료: 5분 (local: 1440분)
- Refresh Token 만료: 30분
- 공개 API 엔드포인트는 `SecurityConfig.kt`의 `GET_PUBLIC`, `POST_PUBLIC` 배열에서 관리

### 도메인별 패턴
각 도메인(Admin, User, Notice)은 동일한 구조를 따름:
- `Controller` - REST API 엔드포인트
- `Service` - 비즈니스 로직
- `Repository` - 데이터 접근 레이어 (MyBatis 매퍼)
- `dto/` - 요청/응답 DTO 클래스들
- `domain/` - 엔티티 클래스

### 공통 기능
- **로깅**: P6spy로 SQL 쿼리 로깅, Sentry 연동
- **API 문서**: SpringDoc OpenAPI (Swagger UI)
- **Coroutines**: Kotlin 코루틴 지원
- **Exception Handler**: `ApiExceptionHandler.kt`에서 전역 예외 처리

## 개발 환경 설정
- Java 25 사용
- Kotlin 2.3.0
- Spring Boot 4.0.1
- 프로필별 설정: local, sandbox, qa, prod

## 트랜잭션 경계 설정
- **Service 레이어**에 `@Transactional` 적용
  - 기본값: `@Transactional(readOnly = true)` (클래스 레벨)
  - 쓰기 작업: `@Transactional` (메서드 레벨)
- **Controller 레이어**는 트랜잭션 없음
- **Repository 레이어**는 데이터 접근만 담당 (트랜잭션 없음)

## 비동기 처리
- Kotlin Coroutines 사용 (`suspend` 함수)
- Service 메서드는 `suspend` 함수로 정의
- Controller에서 `runBlocking`으로 코루틴 실행
- I/O 작업은 `withContext(Dispatchers.IO)` 사용
- 병렬 처리가 필요한 경우 `async`/`await` 활용

## MyBatis Repository 패턴
- `io.github.bestheroz:mybatis-repository` 라이브러리 사용
- Repository 인터페이스는 `SimpleRepository<T>` 상속
- XML 매퍼 파일 없이 기본 CRUD 메서드 제공:
  - `getItemById()`, `getItemByMap()`, `getItemsByMapOrderByLimitOffset()`
  - `insert()`, `updateById()`, `deleteById()`
  - `countByMap()`
- 복잡한 쿼리는 XML 매퍼 파일로 확장 가능

## API 문서 접근
- Swagger UI: `http://localhost:8000/swagger-ui.html` (local 프로필에서만)
- OpenAPI Spec: `http://localhost:8000/v3/api-docs`