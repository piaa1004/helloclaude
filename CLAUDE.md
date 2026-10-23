# CLAUDE.md

이 파일은 Claude Code가 이 리포지토리에서 작업할 때 참고하는 가이드입니다.

## 프로젝트 개요

게시판(Board) 백엔드 REST API 서버.

## 기술 스택

| 구분 | 기술 |
|------|------|
| 언어 | Java 21 (LTS) |
| 프레임워크 | Spring Boot 4.x (Spring Framework 7, Jakarta EE 11) |
| 영속성 | Spring Data JPA (Hibernate 7) |
| 데이터베이스 | PostgreSQL 17 |
| DB 마이그레이션 | Flyway |
| 빌드 도구 | Gradle (Kotlin DSL, Wrapper 사용) |
| 테스트 | JUnit 6 (Jupiter), Spring Boot Test, Mockito, Testcontainers 2.x (PostgreSQL) |
| 실행 환경 | Docker / Docker Compose |
| IDE | IntelliJ IDEA |

### Spring Boot 4 주의사항 — 예전 예제 코드를 그대로 가져오지 않는다

| 항목 | 사용 ✓ | 금지 ✗ |
|------|--------|--------|
| Jakarta | `jakarta.*` | `javax.*` |
| Jackson | Jackson 3 (`tools.jackson.*`) | Jackson 2 (`com.fasterxml.jackson.databind`) |
| Mock Bean | `@MockitoBean`, `@MockitoSpyBean` | `@MockBean`, `@SpyBean` (Boot 4에서 제거) |
| Testcontainers | `org.testcontainers:testcontainers-postgresql`, `org.testcontainers.postgresql.PostgreSQLContainer` | `org.testcontainers:postgresql`, `org.testcontainers.containers.PostgreSQLContainer` |
| JUnit | JUnit 6 (Jupiter) | JUnit 4 (`org.junit.Test`) |

### Boot 4 모듈화 — starter 명명 규칙

Boot 4는 auto-configuration이 기술별 모듈로 분리되어 있다. 3.x 방식으로 라이브러리만 추가하면 auto-config가 적용되지 않는다. **반드시 해당 기술의 starter를 사용한다.**

| 용도 | 의존성 |
|------|--------|
| Web MVC | `spring-boot-starter-webmvc` |
| JPA | `spring-boot-starter-data-jpa` |
| Validation | `spring-boot-starter-validation` |
| Flyway | `spring-boot-starter-flyway` + `org.flywaydb:flyway-database-postgresql` |
| Actuator | `spring-boot-starter-actuator` |
| Web MVC 테스트 | `spring-boot-starter-webmvc-test` |
| JPA 슬라이스 테스트 | `spring-boot-starter-data-jpa-test` |
| Testcontainers 연동 | `spring-boot-testcontainers` + `org.testcontainers:testcontainers-junit-jupiter` + `org.testcontainers:testcontainers-postgresql` |

버전은 Spring Boot BOM에 맡기고 직접 명시하지 않는다. BOM에 없는 의존성을 추가할 때는 Boot 4 / Framework 7 호환 여부를 확인한다.

## 디렉터리 구조 (목표)

```
.
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/
├── gradlew / gradlew.bat
├── Dockerfile
├── .dockerignore
├── docker-compose.yml
├── .env.example
└── src
    ├── main
    │   ├── java/com/example/board
    │   │   ├── BoardApplication.java
    │   │   ├── global/
    │   │   │   ├── config/      # JpaAuditingConfig, WebConfig 등
    │   │   │   ├── error/       # 비즈니스 예외, ErrorCode, GlobalExceptionHandler
    │   │   │   ├── common/      # 공통 응답 DTO (PageResponse 등)
    │   │   │   └── entity/      # BaseEntity
    │   │   └── post/            # 도메인별 패키지 (post, comment, member ...)
    │   │       ├── controller/
    │   │       ├── service/
    │   │       ├── repository/
    │   │       ├── domain/      # JPA 엔티티
    │   │       └── dto/
    │   └── resources
    │       ├── application.yml
    │       ├── application-local.yml
    │       ├── application-docker.yml
    │       └── db/migration/    # Flyway 스크립트 (V1__init.sql ...)
    └── test
        ├── java/com/example/board
        │   └── support/         # Testcontainers 공통 설정
        └── resources/application-test.yml
```

패키지는 **도메인(기능) 단위**로 나누고, 그 안에서 계층별로 나눈다. 도메인 간 참조는 Service 계층을 통해서만 한다.

## 주요 명령어

```bash
# 빌드 (테스트 포함)
./gradlew build

# 테스트만 실행
./gradlew test

# 단일 테스트 실행
./gradlew test --tests "com.example.board.post.service.PostServiceTest"

# 로컬 실행 (DB는 docker compose로 띄운 상태에서)
docker compose up -d db
./gradlew bootRun --args='--spring.profiles.active=local'

# 전체 스택(앱 + DB) 컨테이너 실행
docker compose up --build -d

# 로그 확인 / 종료
docker compose logs -f app
docker compose down          # 데이터 유지
docker compose down -v       # 볼륨(데이터)까지 삭제
```

> Windows PowerShell에서는 `./gradlew` 대신 `.\gradlew.bat`을 사용한다.

## Docker 구성 원칙

### Dockerfile

- **멀티 스테이지 빌드**로 작성한다.
- build 스테이지 (`eclipse-temurin:21-jdk`)
  1. `gradlew`, `gradle/`, `build.gradle.kts`, `settings.gradle.kts`를 **먼저** COPY하고 `./gradlew dependencies`로 의존성 레이어를 캐시한다.
  2. 그 다음 `src/`를 COPY하고 `./gradlew bootJar -x test`.
  3. `java -Djarmode=tools -jar app.jar extract --layers --launcher`로 레이어를 분리한다.
- runtime 스테이지 (`eclipse-temurin:21-jre`)
  - 분리된 레이어(`dependencies`, `spring-boot-loader`, `snapshot-dependencies`, `application`)를 변경 빈도 낮은 순서로 COPY한다.
  - 루트가 아닌 사용자로 실행한다.
- `.dockerignore`에 `build/`, `.gradle/`, `.git/`, `.env`, IDE 설정 파일을 넣는다.

### docker-compose.yml

- `db`
  - `postgres:17`, named volume으로 데이터 영속화
  - healthcheck는 사용자/DB를 명시한다 (생략 시 `role "root" does not exist` 로그가 반복된다):
    ```yaml
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U $${POSTGRES_USER} -d $${POSTGRES_DB}"]
      interval: 5s
      timeout: 3s
      retries: 10
    ```
- `app`
  - `depends_on: db: condition: service_healthy`
  - `SPRING_PROFILES_ACTIVE=docker`
- 컨테이너 내부에서 앱은 DB를 호스트명 `db`로 접근한다 (`jdbc:postgresql://db:5432/board`).

### 비밀값

- DB 접속 정보 등 비밀값은 `.env`로 주입한다. **`.env`는 커밋하지 않는다** (`.env.example`만 커밋).
- `application*.yml`에는 `${DB_USERNAME}`, `${DB_PASSWORD}` 형태의 placeholder만 쓴다.
- 예외: `application-local.yml`에 한해 로컬 전용 기본값을 허용한다 (`${DB_PASSWORD:board}`). 이 값은 `.env.example`의 로컬 기본값과 일치시킨다. `docker`/운영 프로파일에는 기본값을 두지 않는다.

## JPA / 데이터베이스 규칙

### 스키마

- 스키마는 **Flyway로만 관리**한다. `spring.jpa.hibernate.ddl-auto: validate` (`create`/`update` 금지).
- 이미 적용된 마이그레이션 파일은 수정하지 않고, 새 버전 파일(`V{n}__설명.sql`)을 추가한다.
- 조회 조건/정렬에 쓰이는 컬럼과 FK 컬럼에는 마이그레이션에서 인덱스를 함께 생성한다.

### 설정

```yaml
spring:
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        default_batch_fetch_size: 100
  data:
    web:
      pageable:
        max-page-size: 100
```

### 엔티티

- 기본 생성자는 `protected`. 무분별한 setter 금지 — 상태 변경은 의미 있는 도메인 메서드로.
- 연관관계는 기본적으로 `FetchType.LAZY`. `@ManyToOne`, `@OneToOne`도 명시적으로 LAZY 지정.
- 생성/수정 시각은 `BaseEntity` + JPA Auditing(`@CreatedDate`, `@LastModifiedDate`)으로 처리.
- **`@EnableJpaAuditing`은 `BoardApplication`이 아닌 별도 `@Configuration`(`global/config/JpaAuditingConfig`)에 둔다.** 메인 클래스에 붙이면 `@WebMvcTest`가 "JPA metamodel must not be empty"로 실패한다.
- PK는 `Long` + `GenerationType.IDENTITY`.
  - IDENTITY는 Hibernate의 JDBC batch insert가 비활성화된다. 대량 insert가 필요한 기능이 생기면 해당 엔티티만 `SEQUENCE` 전략 도입을 검토한다.
- `equals`/`hashCode`를 재정의하지 않는다 (필요 시 PK 기반으로만, 프록시 고려).

### 조회 / N+1

- `ToOne` 관계는 fetch join 또는 `@EntityGraph`로 함께 조회한다.
- **컬렉션(`ToMany`)은 페이징 쿼리에서 fetch join하지 않는다.** Hibernate가 전체 결과를 메모리에 올려 페이징한다 (HHH90003004 경고). 컬렉션은 `default_batch_fetch_size`로 해결한다.
- 목록 조회는 반드시 페이징(`Pageable`)을 적용한다.
- 정렬 가능한 필드는 화이트리스트로 제한한다 (임의의 `sort` 파라미터를 그대로 쿼리에 넘기지 않는다).

## 코드 컨벤션

### 계층

- 계층 흐름: `Controller → Service → Repository`. Controller에서 Repository를 직접 호출하지 않는다.
- Service 클래스는 `@Transactional(readOnly = true)`를 클래스 레벨 기본으로, 쓰기 메서드에만 `@Transactional`을 붙인다.
- 의존성 주입은 생성자 주입만 사용한다 (`@Autowired` 필드 주입 금지).

### DTO / API

- **엔티티를 API 응답으로 직접 노출하지 않는다.** 요청/응답은 DTO(Java `record`)로 분리한다.
- 요청 DTO는 Bean Validation(`@NotBlank`, `@Size` 등) + `@Valid`로 검증한다.
- **`Page<T>`를 응답으로 직접 반환하지 않는다.** `global/common/PageResponse<T>` record로 변환하거나 `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)`를 적용한다.
- REST API 경로는 `/api/v1/...` 형태, 리소스는 복수형 명사 (`/api/v1/posts`, `/api/v1/posts/{postId}/comments`).
- 상태 코드: 생성 `201 Created` + `Location` 헤더, 삭제 `204 No Content`.

### 예외

- 커스텀 비즈니스 예외(`BusinessException` + `ErrorCode` enum)를 사용한다.
- `@RestControllerAdvice` 전역 핸들러에서 `ProblemDetail`(RFC 9457)로 일관된 에러 응답을 반환한다.
- `spring.mvc.problemdetails.enabled: true`로 프레임워크 기본 예외도 `ProblemDetail` 형식으로 맞춘다.
- `MethodArgumentNotValidException`은 필드별 에러 목록을 `ProblemDetail`의 확장 속성(`errors`)으로 담는다.

### Lombok

- 허용: `@Getter`, `@RequiredArgsConstructor`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, `@Slf4j`
- 금지: `@Data`, `@Setter`, 엔티티의 `@ToString`/`@EqualsAndHashCode` (LAZY 연관관계 초기화 및 순환 참조 위험)
- DTO는 Lombok 대신 `record`를 사용한다.

### 로깅

- `@Slf4j` 사용, `System.out` 금지.
- 비밀번호, 토큰, 개인정보는 로그에 남기지 않는다.
- 예상된 비즈니스 예외는 `WARN`(스택트레이스 없이), 예상하지 못한 예외는 `ERROR`(스택트레이스 포함).

### 인증/인가

- 현재 Spring Security는 도입하지 않았다. 도입 시 이 섹션에 방식(JWT/세션)과 규칙을 추가하고, 그 전까지 임의로 Security 의존성을 추가하지 않는다.

## 테스트 규칙

- Repository/통합 테스트는 H2가 아닌 **Testcontainers PostgreSQL**로 수행한다.
  - 컨테이너는 `test/.../support/`의 `@TestConfiguration`에서 `@ServiceConnection` Bean으로 한 번만 정의하고 재사용한다 (테스트 클래스마다 컨테이너를 새로 띄우지 않는다).
  - 이미지 태그는 운영과 동일하게 `postgres:17`.
- Service 단위 테스트는 Spring 컨텍스트 없이 Mockito(`@ExtendWith(MockitoExtension.class)`)로 의존성을 대체한다.
- Controller 테스트는 `@WebMvcTest` + `MockMvcTester`를 사용하고, Service는 `@MockitoBean`으로 대체한다.
- 새로운 기능을 추가할 때는 관련 테스트를 함께 작성하고, 커밋 전 `./gradlew build`가 통과해야 한다.

## 설정 프로파일

| 프로파일 | 용도 | DB 주소 | 비밀값 |
|----------|------|---------|--------|
| `local` | IDE/`bootRun`으로 로컬 실행 | `localhost:5432` (compose의 db 컨테이너) | 로컬 전용 기본값 허용 |
| `docker` | 컨테이너로 실행 | `db:5432` | `.env` 필수 |
| `test` | 테스트 | Testcontainers가 자동 구성 | 불필요 |

## 작업 시 유의사항

- 의존성 추가 시 Boot 4 starter 명명 규칙과 Spring Boot BOM 관리 여부를 먼저 확인한다.
- 비밀번호, 토큰 등 민감 정보를 코드나 `application*.yml`에 하드코딩하지 않는다.
- 스키마 변경이 필요하면 엔티티 수정과 Flyway 마이그레이션 추가를 같은 커밋에서 함께 한다.