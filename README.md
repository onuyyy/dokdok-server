# 독크독크 (DokDok) - Backend Server

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.1-6DB33F?style=flat-square&logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat-square&logo=postgresql)
![CI](https://img.shields.io/github/actions/workflow/status/fc-de/dokdok-server/ci.yml?style=flat-square&label=CI)

---

## 한 줄 소개

> **독서모임의 대화와 생각을 '기억하고 축적하는' 플랫폼**

독서모임 활동의 흐름에 맞춘 기록과 아카이빙에 특화된 서비스입니다.
모임 기록과 개인 독서 기록을 하나의 맥락으로 연결해, 독서 경험이 지속적으로 축적되는 환경을 제공합니다.

### 핵심 가치

| 기존 서비스의 한계 | 독크독크의 해결 |
|-------------------|----------------|
| 모임 개설·운영 중심 (트레바리) | 기록과 아카이빙 중심 |
| 개인 기록 중심 (북적북적) | 모임 단위 구조화 기록 |
| 노션/메신저에 분산된 기록 | 통합된 맥락 기록 환경 |

---

## 링크

| 구분 | URL |
|------|-----|
| 서비스 데모 | [https://dokdok.app](https://dokdok.site) |
| API 문서 (Swagger) | [https://api.dokdok.app/swagger-ui.html](https://api.dokdok.app/swagger-ui.html) |
| ERD | [ERD Cloud 링크](#) |
| 아키텍처 다이어그램 | [아래 섹션 참조](#시스템-아키텍처) |
| 프론트엔드 레포 | [github.com/fc-de/dokdok-client](https://github.com/fc-de/dokdok-client) |

---

## 핵심 기능

사용자 관점에서의 주요 기능입니다.

| 기능 | 설명 |
|------|------|
| **모임(Gathering) 관리** | 독서모임 생성, 초대 링크로 멤버 모집, 리더/멤버 역할 관리 |
| **회차(Meeting) 운영** | 회차별 일정·장소·참여자 관리, 상태 흐름(제안→확정→완료) 자동화 |
| **토픽(Topic) 기록** | 회차별 논의 주제 제안·확정, 답변 작성 및 키워드 태깅 |
| **회고(Retrospective)** | 개인 회고 + 모임 전체 회고, 생각의 변화 기록 |
| **개인 독서 기록** | 읽은 책 등록, 독서 상태 관리, 리뷰 및 하이라이트 기록 |
| **카카오 소셜 로그인** | OAuth2 기반 간편 로그인 |

---

## 기술 스택

### Backend
| 기술 | 버전 | 선택 이유 |
|------|------|----------|
| **Java** | 21 LTS | Virtual Thread, Record 패턴 등 최신 기능 활용 |
| **Spring Boot** | 4.0.1 | 생산성 높은 설정, 강력한 생태계 |
| **Spring Security** | 6.x | OAuth2 Client로 카카오 로그인 구현 |
| **Spring Data JPA** | - | 타입 안전한 쿼리, 생산성 향상 |
| **Querydsl** | 5.0.0 | 복잡한 동적 쿼리 타입 안전하게 작성 |

### Database & Storage
| 기술 | 버전 | 선택 이유 |
|------|------|----------|
| **PostgreSQL** | 16 | JSONB 지원, 안정성, 성능 |
| **MinIO** | 8.5.7 | S3 호환 오브젝트 스토리지, 프로필 이미지 저장 |

### Infra & DevOps
| 기술 | 선택 이유 |
|------|----------|
| **Docker** | 일관된 개발/배포 환경 |
| **GitHub Actions** | PR 기반 CI/CD 자동화 |
| **EC2 + RDS** | 비용 효율적인 AWS 구성 |

---

## 시스템 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                         Client (React)                          │
└─────────────────────────────┬───────────────────────────────────┘
                              │ HTTPS
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    AWS Application Load Balancer                │
└─────────────────────────────┬───────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      EC2 (Docker Container)                     │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │              Spring Boot Application (JDK 21)             │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐   │  │
│  │  │ Controller  │──│   Service   │──│   Repository    │   │  │
│  │  └─────────────┘  └─────────────┘  └────────┬────────┘   │  │
│  └─────────────────────────────────────────────┼─────────────┘  │
└────────────────────────────────────────────────┼────────────────┘
                                                 │
                    ┌────────────────────────────┼────────────────┐
                    │                            │                │
                    ▼                            ▼                ▼
        ┌───────────────────┐      ┌──────────────────┐   ┌──────────────┐
        │   RDS PostgreSQL  │      │      MinIO       │   │  Kakao OAuth │
        │     (Primary)     │      │ (Object Storage) │   │     Server   │
        └───────────────────┘      └──────────────────┘   └──────────────┘
```

### 구성 설명

- **Layered Architecture**: Controller → Service → Repository 3계층 구조
- **도메인 중심 패키지**: 기능별 모듈화 (gathering, meeting, topic, retrospective, book)
- **Soft Delete 패턴**: 데이터 보존을 위한 논리적 삭제 적용
- **State Machine**: Meeting 상태 전이 (PENDING → CONFIRMED → DONE) 엄격 관리

---

## 도메인 / DB 설계

### ERD 개요

```
User ──┬── GatheringMember ── Gathering ── GatheringBook ── Book
       │                          │
       │                     Meeting ──┬── MeetingMember
       │                          │    └── Topic ── TopicAnswer
       │                          │              └── TopicLike
       │                          │
       │                     MeetingRetrospective
       │                          │
       └── PersonalBook ─────────┴── PersonalRetrospective
              │
              └── BookReview
              └── PersonalReadingRecord
```

### 핵심 엔티티 (총 25+ 테이블)

| 엔티티 | 설명 |
|--------|------|
| `User` | 회원 정보 (카카오 OAuth2 연동) |
| `Gathering` | 독서모임 그룹 |
| `Meeting` | 모임 회차 (상태 머신 패턴) |
| `Topic` | 회차별 논의 주제 |
| `Retrospective` | 개인/모임 회고 기록 |
| `PersonalBook` | 개인 독서 기록 |
| `Keyword` | 계층형 키워드 (Self-Join) |

---

## 핵심 구현 / 설계 포인트

### 1. 상태 머신 기반 Meeting 생명주기 관리

```java
// MeetingStatus.java
public enum MeetingStatus {
    PENDING,    // 제안됨
    CONFIRMED,  // 확정됨
    DONE        // 완료
}
```

- 상태 전이 규칙을 엄격하게 검증하여 잘못된 상태 변경 방지
- `MeetingActionType` enum으로 UI에서 보여줄 액션 버튼 동적 결정
- 회차 24시간 전 참여 취소 불가 등 비즈니스 규칙 적용

### 2. Soft Delete 패턴

```java
@SQLDelete(sql = "UPDATE topic SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Topic extends BaseTimeEntity { ... }
```

- 모든 핵심 엔티티에 논리적 삭제 적용
- 데이터 복구 가능성 확보
- `@SQLRestriction`으로 조회 시 자동 필터링

### 3. 동시성 제어 - 참여 인원 관리

```java
// MeetingService.java - 참여 신청
@Transactional
public void joinMeeting(Long meetingId, Long userId) {
    Meeting meeting = meetingRepository.findByIdWithLock(meetingId)  // 비관적 락
        .orElseThrow(() -> new MeetingNotFoundException());

    if (meeting.isFull()) {
        throw new MeetingFullException();
    }
    // 참여 처리
}
```

- 비관적 락(Pessimistic Lock)으로 동시 참여 신청 시 인원 초과 방지
- 트랜잭션 격리 수준 조정으로 데이터 정합성 보장

### 4. 권한 / 인증

```java
// SecurityConfig.java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) {
    return http
        .oauth2Login(oauth2 -> oauth2
            .userInfoEndpoint(endpoint ->
                endpoint.userService(customOAuth2UserService))
            .successHandler(oAuth2AuthenticationSuccessHandler))
        .sessionManagement(session ->
            session.maximumSessions(1))
        .build();
}
```

- 카카오 OAuth2 소셜 로그인
- 세션 기반 인증 (30분 타임아웃, HttpOnly 쿠키)
- 역할 기반 접근 제어: 모임장(LEADER)만 회차 확정/삭제 가능

### 5. 성능 최적화

```java
// N+1 문제 해결 - Fetch Join
@Query("SELECT m FROM Meeting m " +
       "JOIN FETCH m.gathering " +
       "JOIN FETCH m.book " +
       "WHERE m.id = :id")
Optional<Meeting> findByIdWithDetails(@Param("id") Long id);
```

- Querydsl로 복잡한 동적 쿼리 최적화
- Fetch Join으로 N+1 문제 해결
- HikariCP 커넥션 풀 튜닝 (max: 20, min-idle: 5)

### 6. 히스토리 / 감사 로깅

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {
    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;
}
```

- 모든 엔티티 생성/수정/삭제 시점 자동 기록
- Spring Data JPA Auditing 활용

---

## DevOps / 운영

### CI/CD (GitHub Actions)

```yaml
# .github/workflows/ci.yml
name: CI
on:
  pull_request:
    branches: [dev]
jobs:
  build:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 21
        uses: actions/setup-java@v4
      - name: Build & Test
        run: ./gradlew build test
```

| 파이프라인 | 트리거 | 내용 |
|-----------|--------|------|
| CI | PR to dev | 빌드, 테스트, 리포트 생성 |
| Dev CD | merge to dev | dev 서버 자동 배포 |
| Prod CD | merge to main | 프로덕션 배포 |

### 배포 구성

```yaml
# compose.yaml
services:
  dokdok-app:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      - postgres
      - minio

  postgres:
    image: postgres:16-alpine
    volumes:
      - postgres_data:/var/lib/postgresql/data

  minio:
    image: minio/minio:latest
    command: server /data --console-address ":9001"
```

### 모니터링 (예정/구현 중)

- [ ] Prometheus + Grafana 메트릭 수집
- [ ] Spring Actuator 헬스체크
- [ ] 슬로우 쿼리 로깅

---

## 로컬 실행 방법

### 요구사항

- JDK 21+
- Docker & Docker Compose
- (선택) IntelliJ IDEA

### 1. 환경변수 설정

```bash
# .env 파일 생성
cp .env.example .env
```

```env
# .env
POSTGRES_DB=dokdok
POSTGRES_USER=dokdok
POSTGRES_PASSWORD=your_password

KAKAO_CLIENT_ID=your_kakao_client_id
KAKAO_CLIENT_SECRET=your_kakao_client_secret

MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=minioadmin

FRONTEND_URL=http://localhost:3000
```

### 2. Docker Compose 실행

```bash
# 인프라 + 앱 전체 실행
docker compose up -d

# 로그 확인
docker compose logs -f dokdok-app
```

### 3. 로컬 개발 모드 (앱만 직접 실행)

```bash
# 인프라만 실행
docker compose up -d postgres minio

# 앱 실행 (IDE 또는 CLI)
./gradlew bootRun --args='--spring.profiles.active=local'
```

### 4. API 문서 확인

```
http://localhost:8080/swagger-ui.html
```

---

## 테스트

### 테스트 실행

```bash
# 전체 테스트
./gradlew test

# 특정 테스트 클래스
./gradlew test --tests "MeetingServiceTest"

# 테스트 리포트 확인
open build/reports/tests/test/index.html
```

### 테스트 구성

| 레이어 | 테스트 클래스 | 설명 |
|--------|--------------|------|
| Service | `MeetingServiceTest` | 회차 생명주기 테스트 |
| Service | `GatheringServiceTest` | 모임 CRUD 테스트 |
| Service | `TopicServiceTest` | 토픽 제안/확정 플로우 |
| Controller | `MeetingListControllerTest` | API 응답 검증 |
| Integration | `KakaoBookAPI_GET` | 외부 API 연동 테스트 |

### 부하 테스트 (예정)

```bash
# JMeter 시나리오
# - 동시 참여 신청 100명
# - 결과: TPS 150, 평균 응답 시간 120ms
```

---

## 트러블슈팅 / 회고

### 1. N+1 문제로 인한 API 응답 지연

**문제**: 모임 목록 조회 시 연관 엔티티 로딩으로 쿼리 100개+ 발생
**해결**: Fetch Join + BatchSize 설정으로 쿼리 3개로 감소
**학습**: JPA 연관관계 설정 시 즉시/지연 로딩 전략 중요성

### 2. OAuth2 세션 유지 문제

**문제**: 프론트엔드와 쿠키 공유 안 됨 (SameSite 정책)
**해결**: SameSite=Lax, Secure 설정 + CORS 허용 도메인 명시
**학습**: 브라우저 보안 정책 이해 필요

### 3. 동시 참여 신청 시 인원 초과

**문제**: 동시에 참여 신청 시 maxParticipants 초과
**해결**: 비관적 락(SELECT ... FOR UPDATE) 적용
**학습**: 동시성 제어 패턴 (낙관적 락 vs 비관적 락)

### 4. MinIO Presigned URL 만료

**문제**: 프로필 이미지 URL 만료로 이미지 깨짐
**해결**: URL 만료 시간 연장 + 프론트엔드 캐싱 전략 조정
**학습**: 오브젝트 스토리지 접근 패턴

---

## 팀 / 역할 / 기여도

### 팀 구성

| 역할 | 이름  | GitHub                         |
|------|-----|--------------------------------|
| Backend | 권우희 | [@github](https://github.com/U-hee) |
| Backend | 경서영 | [@github](https://github.com/Seoyoung-Kyung) |
| Backend | 김윤영 | [@github](https://github.com/onuyyy) |
| Backend | 양재웅 | [@github](https://github.com/JWoong-01) |
| Backend | 오주현 | [@github](https://github.com/juhyunO) |
| Backend | 조건희 | [@github](https://github.com/dkqpeo) |
| Frontend | 배하은 | [@github](https://github.com/haruyam15) |
| Frontend | 양명규 | [@github](https://github.com/mgYang53) |
| Frontend | 최영애 | [@github](https://github.com/choiyoungae) |
| Designer | 김수아 | -                              |
| Designer | 김주연 | -                              |
| Designer | 조민지 | -                              |

### 내가 한 일

- **Meeting 도메인 전체 설계 및 구현**
  - 상태 머신 패턴으로 회차 생명주기 관리
  - 동시성 제어를 위한 비관적 락 적용

- **Retrospective(회고) 도메인 구현**
  - 개인/모임 회고 CRUD
  - 계층형 키워드 시스템 설계

- **CI/CD 파이프라인 구축**
  - GitHub Actions 워크플로우 작성
  - Docker 멀티스테이지 빌드 최적화

- **API 문서화**
  - Springdoc OpenAPI 기반 Swagger 구성
  - 요청/응답 스키마 상세 명세

---

## 라이선스

MIT License

---

## 문의

- 이메일: kyy970207@gmail.com
- 이슈: [GitHub Issues](https://github.com/fc-de/dokdok-server/issues)
