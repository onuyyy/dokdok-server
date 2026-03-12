# TESTING_RULES.md

## 1. 목적

이 문서는 현재 프로젝트에 존재하는 테스트 코드를 분석하여 도출한 **테스트 작성 표준 규칙**이다.  
신규 테스트 코드는 반드시 기존 테스트 스타일을 유지하는 것을 목표로 한다.

또한 AI(Copilot / Codex / Claude)가 테스트 코드를 생성할 때 **이 문서를 기준으로 테스트를 작성한다.**

---

# 2. 테스트 기술 스택

본 프로젝트는 다음 테스트 스택을 사용한다.

테스트 프레임워크
- JUnit5 (Jupiter)

Mock 라이브러리
- Mockito
- @ExtendWith(MockitoExtension.class)
- @Mock
- @InjectMocks

Assertion
- AssertJ (assertThat, assertThatThrownBy)
- 일부 JUnit Assertions (assertDoesNotThrow, Assumptions)

Spring 통합 테스트
- @SpringBootTest
- @ActiveProfiles("test")

DB 테스트
- H2 In-Memory Database
- jdbc:h2:mem:testdb
- ddl-auto: create-drop

주의
- 신규 테스트는 기존에 사용 중인 기술 스택만 사용한다.
- 새로운 테스트 라이브러리를 도입하지 않는다.

---

# 3. 테스트 구조 패턴

모든 테스트는 기본적으로 Given / When / Then 구조를 따른다.

구조

Given  
테스트 데이터 및 환경 준비

When  
테스트 대상 메서드 실행

Then  
결과 검증

예시

@Test
@DisplayName("모임 생성 성공")
void givenValidRequest_whenCreateMeeting_thenSuccess() {

    // given
    MeetingCreateRequest request = new MeetingCreateRequest("독서 모임");

    given(meetingRepository.save(any()))
        .willReturn(new Meeting());

    // when
    Meeting result = meetingService.createMeeting(request);

    // then
    assertThat(result).isNotNull();
    verify(meetingRepository).save(any());
}

예외 케이스의 경우 "// when + then" 구조를 함께 사용할 수 있다.

---

# 4. 테스트 메서드 네이밍 규칙

기본 형식

givenX_whenY_thenZ

예

givenValidRequest_whenCreateMeeting_thenSuccess  
givenNullEmail_whenRegister_thenThrowException

일부 레거시 테스트에서 다음 형식이 존재할 수 있다.

createBook_Success  
updateMeeting_Fail

하지만 신규 테스트는 given_when_then 형식을 우선 사용한다.

---

# 5. DisplayName 규칙

모든 테스트 메서드는 한글 설명의 @DisplayName을 반드시 작성한다.

예

@DisplayName("모임 생성 성공")  
@DisplayName("이메일이 null이면 예외 발생")

테스트의 목적이 명확히 드러나도록 작성한다.

---

# 6. Mock 사용 규칙

단위 테스트는 다음 규칙을 따른다.

MockitoExtension 사용

@ExtendWith(MockitoExtension.class)

의존성 주입

@Mock
private MeetingRepository meetingRepository;

@InjectMocks
private MeetingService meetingService;

Stubbing

given(repository.save(any())).willReturn(entity);

또는

when(repository.save(any())).thenReturn(entity);

행위 검증

verify(repository).save(any());
verify(repository, times(1)).save(any());
verify(repository, never()).delete(any());

lenient mock

필요한 경우 lenient() 사용 가능.

---

# 7. 정적 메서드 Mock 규칙

정적 메서드는 MockedStatic을 사용한다.

예

try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
mocked.when(SecurityUtil::getCurrentUserId).thenReturn(1L);

    // 테스트 실행
}

패턴

- 테스트 메서드 내부에서 try-with-resources 사용
- 일부 테스트에서는 @BeforeEach / @AfterEach 패턴 사용 가능

---

# 8. 통합 테스트 규칙

통합 테스트는 다음 목적에서 사용한다.

- Spring Context 로딩 검증
- Repository 동작 검증
- Listener / Scheduler 검증
- 실제 Bean 연동 검증

설정

@SpringBootTest
@ActiveProfiles("test")

DB

- H2 in-memory database 사용

테스트 격리

- deleteAll() 또는 cleanup 로직 사용
- 트랜잭션 롤백 사용 가능

외부 API 테스트

- 환경변수 또는 .env 기반 키 사용
- Assumptions를 이용한 조건부 실행 허용

---

# 9. 반드시 테스트해야 하는 케이스

각 서비스 메서드는 최소 다음 케이스를 테스트해야 한다.

1. 정상 동작 케이스
2. 예외 발생 케이스
3. 경계값(edge case)

예

- 정상 등록
- null 입력
- 잘못된 값

---

# 10. 금지 사항

다음 테스트는 작성하지 않는다.

- 단순 getter / setter 테스트
- 의미 없는 테스트
- 랜덤 데이터 기반 테스트
- 환경 의존 테스트

---

# 11. AI 테스트 생성 규칙

AI(Copilot / Codex / Claude)가 테스트 코드를 생성할 때 반드시 다음 규칙을 따른다.

1. JUnit5 사용
2. MockitoExtension 기반 테스트 작성
3. AssertJ assertion 사용
4. 메서드 이름은 givenX_whenY_thenZ 형식 사용
5. 모든 테스트에 한글 @DisplayName 작성
6. 테스트 본문은 // given // when // then 구조 유지
7. 정상 케이스 + 예외 케이스 포함
8. 서비스 의존성은 @Mock 사용
9. 테스트 대상 클래스는 @InjectMocks 사용
10. 정적 메서드는 MockedStatic으로 처리

AI 요청 예

"TESTING_RULES.md를 참고하여 이 클래스의 테스트 코드를 작성하라."

또는

"Follow TESTING_RULES.md and generate tests for this class."

---

# 12. CI 환경

모든 테스트는 다음 환경에서 실행된다.

- GitHub Actions
- Maven / Gradle test task
- H2 In-Memory DB

테스트는 반드시 CI 환경에서 통과해야 한다.