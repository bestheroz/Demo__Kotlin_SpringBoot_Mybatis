# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요
Kotlin + Spring Boot + MyBatis 데모 애플리케이션. 관리자(Admin), 사용자(User), 공지(Notice) 도메인을 포함한다.
프로필: local, sandbox, qa, prod.

## 주요 명령어
- `./gradlew bootRun` — 애플리케이션 실행 (포트 8000)
- `./gradlew spotlessApply` — 포맷팅 적용. 코드를 고쳤으면 커밋 전에 돌린다
- `./gradlew spotlessCheck assemble` — CI(`.github/workflows/test.yml`)와 같은 검증. 이게 통과하면 검증 끝
- `docker build -t demo-app .` — 이미지 빌드 (컨테이너도 포트 8000)
- 테스트 소스셋이 없다(`src/test` 자체가 없음). `./gradlew test` 는 아무것도 실행하지 않고 CI 도 `-x test` 로 돈다. "테스트 통과"를 완료 근거로 쓰지 말고 위 `spotlessCheck assemble` 로 검증한다
- `./gradlew dependencyUpdates` — 업데이트 보고서
- `./gradlew versionCatalogUpdate --interactive` — 올릴 후보를 `gradle/libs.versions.updates.toml` 에 쓴다
- `./gradlew versionCatalogApplyUpdates` — 위 파일에 남긴 항목만 카탈로그에 반영

## 의존성 관리
- 사전 릴리스(M/RC/Beta/Alpha/Preview)를 허용하고 우선한다. 신규 버전 선체험과 변화점 발견이 이 리포의 목적이다
- 플러그인·라이브러리 좌표는 전부 `gradle/libs.versions.toml` 에 두고, `build.gradle.kts` 는 `libs.xxx` / `alias(libs.plugins.xxx)` 로만 참조한다
- BOM·핀·VCU 관련 세부 기준은 `.claude/rules/dependency-catalog.md`

## 아키텍처 규칙
- `demo` → `standard` 단방향 의존. `standard` 에서 `demo` 패키지를 참조하지 않는다
- 새 도메인을 추가할 때는 Controller / Service / Repository / `domain/` 엔티티 / `dtos/<domain>/` 5종을 모두 만든다
- 공개 API 엔드포인트는 `SecurityConfig.kt` 의 `GET_PUBLIC`, `POST_PUBLIC` 배열에만 추가한다. 다른 경로로 열지 않는다
- Controller 에서 예외를 try-catch 로 삼키지 않는다. 전역 `ApiExceptionHandler` 가 응답으로 변환한다
- JWT 기반 인증. Access Token 만료 5분(local 1440분), Refresh Token 만료 30분

## 트랜잭션 경계
- 클래스 레벨에 `@Transactional(readOnly = true)`, 쓰기 메서드에만 메서드 레벨 `@Transactional` — Service 레이어에만 붙인다
- Controller 와 Repository 에는 `@Transactional` 을 붙이지 않는다

## 비동기 처리
- Service 메서드는 `suspend` 로 정의하고, Controller 에서 `runBlocking` 으로 실행한다
- I/O 작업은 `withContext(Dispatchers.IO)` 로 감싼다

## MyBatis Repository 패턴
- Repository 인터페이스는 `MybatisRepository<T>` 를 상속하고 기본 CRUD 는 XML 매퍼 없이 쓴다. 복잡한 쿼리만 XML 매퍼로 확장한다
- **엔티티 경로(`insert`/`insertBatch`/`updateById`/`updateByMap`)의 `null` 은 "미설정"이다.** INSERT 는 그 자리에 `DEFAULT` 를 내고, UPDATE 는 그 필드를 SET 에서 뺀다. 컬럼을 NULL 로 비우려면 맵 경로(`updateMapById`/`updateMapByMap`)에 값이 `null` 인 키를 넣는다

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
- `gradle/`, `build.gradle.kts`, `settings.gradle.kts` — 버전 카탈로그·BOM·핀 기준, `.claude/rules/dependency-catalog.md`
- 지시 파일 자체(CLAUDE.md, rules) 작성 기준 — `.claude/rules/claude-md-maintenance.md`
