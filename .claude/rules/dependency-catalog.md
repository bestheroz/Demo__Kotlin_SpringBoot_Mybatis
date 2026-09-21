---
paths:
  - "gradle/**"
  - "build.gradle.kts"
  - "settings.gradle.kts"
  - "gradle.properties"
---

# 버전 카탈로그·의존성 갱신 세부 기준

## 사전 릴리스를 후보로 만드는 설정
- `versionCatalogUpdate` 는 `LATEST` 선택기, `dependencyUpdates` 는 `rejectPreReleases = false` 로 둔다. 둘 중 하나만 바꾸면 두 명령의 후보가 달라진다
- `repo.spring.io/milestone` 저장소는 `settings.gradle.kts` 와 `build.gradle.kts` 양쪽에 둔다(플러그인 해석과 의존성 해석이 별도 저장소 목록을 본다). 스냅샷 저장소는 넣지 않는다
- Spring Boot 플러그인·Kotlin·Gradle wrapper 버전은 Demo 리포 공통값을 쓴다

## BOM 과 버전 명시
- 원래 버전이 없던 좌표는 카탈로그에도 버전 없이(`{ module = "g:a" }`) 둬서 Spring Boot BOM 을 따르게 한다. Boot 플러그인을 올리면 이 항목들은 그 BOM 이 고른 버전으로 함께 움직인다
- 직접 적은 버전은 BOM 을 이긴다. 그래서 BOM 관리 좌표에 버전을 새로 적는 것은 "BOM 보다 앞서 가겠다"는 결정이다. 그 의도가 없으면 버전 없이 넣는다
- 현재 그 의도로 버전을 명시한 좌표: `hibernate-core`, `jakarta.persistence-api`, `mysql-connector-j`. 사전 릴리스 포함 최신까지 올린다
- Kotlin JVM 플러그인과 Spring 플러그인은 `[versions] kotlin` 하나를 공유한다

## versionCatalogUpdate(VCU) 함정
- VCU 는 버전이 있는 항목만 갱신하고 버전 없는 항목은 건너뛴다
- VCU 가 카탈로그를 다시 쓸 때 항목 옆 주석은 사라질 수 있다. 그래서 설명은 `build.gradle.kts` 에 두고 카탈로그에는 `@pin` / `@keep` 만 쓴다
- VCU 는 설정 캐시와 호환되지 않아 "Configuration cache entry discarded" 가 뜨지만 빌드는 성공한다. 실패로 보고하지 않는다

## 깨진 좌표 처리
- 최신 버전에서 깨지고 고칠 수 없는 좌표는 동작하는 가장 최신 버전으로 내리고 `# @pin` 을 붙인다
- 내린 사유는 `build.gradle.kts` 주석에 적는다(카탈로그 주석은 VCU 가 지울 수 있으므로)

## 빌드 플래그
- `gradle.properties` 가 설정 캐시·빌드 캐시·병렬 실행을 켠다. 그래서 CI 명령과 로컬 명령에 그 플래그를 따로 적지 않는다. 플래그를 CI 쪽에만 추가하면 로컬·IDE 빌드가 같은 이점을 못 받는다
