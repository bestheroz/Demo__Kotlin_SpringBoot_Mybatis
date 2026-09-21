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

### 의존성 갱신
- `./gradlew dependencyUpdates` - BOM 관리 의존성까지 포함한 업데이트 보고서 (사전 릴리스 포함)
- `./gradlew versionCatalogUpdate --interactive` - 올릴 후보를 `gradle/libs.versions.updates.toml` 에 쓴다
- `./gradlew versionCatalogApplyUpdates` - 위 파일에 남긴 항목만 카탈로그에 반영

### Docker
- `docker build -t demo-app .` - Docker 이미지 빌드
- Docker 실행 시 포트 8000 노출

## 의존성 관리
- **Demo 정책**: 이 리포는 신규 버전 선체험과 변화점 발견이 목적이라 사전 릴리스(M/RC/Beta/Alpha/Preview)를 허용하고 우선한다. `versionCatalogUpdate` 는 `LATEST` 선택기, `dependencyUpdates` 는 `rejectPreReleases = false` 로 사전 릴리스까지 후보로 본다. `repo.spring.io/milestone` 저장소는 `settings.gradle.kts` 와 `build.gradle.kts` 에 두고, 스냅샷 저장소는 넣지 않는다. Spring Boot 플러그인·Kotlin·Gradle wrapper 는 Demo 리포 공통값을 쓴다.
- 플러그인·라이브러리 좌표는 전부 `gradle/libs.versions.toml` 에 있고, `build.gradle.kts` 는 `libs.xxx` / `alias(libs.plugins.xxx)` 로만 참조한다.
- 원래 버전이 없던 좌표는 카탈로그에도 버전 없이(`{ module = "g:a" }`) 두어 Spring Boot BOM 을 따른다. Boot 플러그인을 올리면 이 항목들도 그 BOM 이 고른 버전을 따라간다.
- BOM 이 관리하지만 원래부터 버전을 명시한 좌표(`hibernate-core`, `jakarta.persistence-api`, `mysql-connector-j`)는 BOM 보다 앞서 체험하려는 의도라 버전을 명시한 채 두고, `versionCatalogUpdate` 로 사전 릴리스 포함 최신까지 올린다. 직접 적은 버전은 BOM 을 이기므로, BOM 관리 좌표에 버전을 새로 적는 것은 BOM 보다 앞서 가겠다는 결정이다. 그 의도가 없으면 버전 없이 넣는다.
- `versionCatalogUpdate`(VCU) 는 버전 있는 항목만 갱신하고 버전 없는 항목은 건너뛴다. VCU 가 카탈로그를 다시 쓸 때 항목 옆 주석은 사라질 수 있으므로 설명은 `build.gradle.kts` 에 두고, 카탈로그에는 `@pin` / `@keep` 만 쓴다.
- 최신 버전에서 깨지고 고칠 수 없는 좌표는 동작하는 가장 최신 버전으로 내리고 `# @pin` 을 붙인다. 사유는 `build.gradle.kts` 주석에 적는다.
- Kotlin JVM / Spring 플러그인은 `[versions] kotlin` 하나를 공유한다.
- `gradle.properties` 가 설정 캐시·빌드 캐시·병렬 실행을 켠다. 그래서 CI 명령에 해당 플래그가 없다. `versionCatalogUpdate` 는 설정 캐시와 호환되지 않아 "Configuration cache entry discarded" 가 뜨지만 빌드는 성공한다.

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
- Kotlin 2.4.20
- Spring Boot 4.2.0-M1
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
- `io.github.bestheroz:mybatis-repository` 라이브러리 사용 (0.10.1)
- Repository 인터페이스는 `MybatisRepository<T>` 상속
- XML 매퍼 파일 없이 기본 CRUD 메서드 제공:
  - `getItemById()`, `getItemByMap()`, `getItemsByMapOrderByLimitOffset()`
  - `insert()`, `updateById()`, `deleteById()`
  - `countByMap()`
- 복잡한 쿼리는 XML 매퍼 파일로 확장 가능
- **엔티티 경로(`insert`/`insertBatch`/`updateById`/`updateByMap`)의 `null` 은 "미설정"이다.** INSERT 는 그 자리에 `DEFAULT` 를 내고, UPDATE 는 그 필드를 SET 에서 뺀다. 컬럼을 NULL 로 비우려면 맵 경로(`updateMapById`/`updateMapByMap`)에 값이 `null` 인 키를 넣는다.

## API 문서 접근
- Swagger UI: `http://localhost:8000/swagger-ui.html` (local 프로필에서만)
- OpenAPI Spec: `http://localhost:8000/v3/api-docs`

## CLAUDE.md 관리 규칙
- 이 파일은 200줄 이하 유지. 매 세션 필요한 내용만 둔다: 빌드/테스트 명령, 전역 컨벤션, 도메인 간 의존 규칙, 함정과 그 이유
- 코드에서 유추 가능한 내용(디렉터리 구조, 의존성 목록, 아키텍처 개요)은 쓰지 않는다
- 지시는 검증 가능한 수준으로 구체적으로 쓴다 (X "포맷 잘 맞춰라" / O "2-space 들여쓰기")
- 특정 도메인/경로에만 해당하는 규칙은 이 파일에 넣지 않는다
  - 도메인이 단일 폴더로 분리돼 있으면 → 해당 폴더의 CLAUDE.md
  - 여러 폴더에 흩어져 있으면 → `.claude/rules/<topic>.md` + `paths` frontmatter
  - 다단계 절차는 → 스킬
- 하위 CLAUDE.md 와 rules 에는 루트 규칙을 재진술하지 않는다. 충돌/중복 발견 시 사용자에게 알린다
- 도메인 규칙을 분리하면 아래 "도메인 인덱스"에 한 줄 추가한다
- 지시 파일을 추가/수정할 때는 변경 전 사용자에게 위치와 내용을 먼저 제안한다

## 도메인 인덱스
<!-- 형식: `경로/` — 한 줄 설명, 규칙 파일 위치 -->
<!-- 이 리포는 레이어 우선 구조라 도메인이 단일 폴더로 모이지 않는다. 도메인 규칙은 `.claude/rules/<domain>.md` + paths 로 작성한다. -->
- `demo/**/Admin*` — 관리자 계정·로그인·토큰 재발급. 규칙 파일 없음
- `demo/**/User*` — 일반 사용자 계정·로그인·토큰 재발급. 규칙 파일 없음
- `demo/**/Notice*` — 공지 CRUD. 규칙 파일 없음
- `standard/` — 인증, 예외 변환, 응답 포맷, MyBatis 확장 등 공통 프레임워크. 규칙 파일 없음
- `migration/` — Flyway SQL 마이그레이션. 규칙 파일 없음
- 지시 파일 자체(CLAUDE.md, rules) 작성 기준 — `.claude/rules/claude-md-maintenance.md`
