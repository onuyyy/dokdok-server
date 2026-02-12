# DokDok API 에러코드 문서

## Global 에러코드

### 기본 에러

| 코드 | 에러명 | 메시지 | HTTP |
| --- | --- | --- | --- |
| E001 | INVALID_ENUM_VALUE | 유효하지 않은 값입니다. | 400 |
| E002 | INVALID_REQUEST_FORMAT | 잘못된 요청 형식입니다. | 400 |
| E003 | STATUS_ALREADY_SET | 이미 해당 상태입니다. | 400 |
| E004 | JSON_SERIALIZATION_ERROR | JSON 직렬화 처리 중 오류가 발생했습니다. | 500 |

### 공통 시스템 에러

| 코드 | 에러명 | 메시지 | HTTP |
| --- | --- | --- | --- |
| G001 | INTERNAL_SERVER_ERROR | 서버 내부 오류가 발생했습니다. | 500 |
| G002 | INVALID_INPUT_VALUE | 입력값이 올바르지 않습니다. | 400 |
| G003 | INVALID_TYPE_VALUE | 타입이 올바르지 않습니다. | 400 |
| G004 | METHOD_NOT_ALLOWED | 지원하지 않는 HTTP 메서드입니다. | 405 |

### 인증/인가

| 코드 | 에러명 | 메시지 | HTTP |
| --- | --- | --- | --- |
| G101 | ACCESS_DENIED | 접근 권한이 없습니다. | 403 |
| G102 | UNAUTHORIZED | 인증이 필요합니다. | 401 |
| G103 | INVALID_TOKEN | 유효하지 않은 토큰입니다. | 401 |
| G104 | EXPIRED_TOKEN | 만료된 토큰입니다. | 401 |
| G105 | REFRESH_TOKEN_NOT_FOUND | 리프레시 토큰을 찾을 수 없습니다. | 401 |

### 파일 처리

| 코드 | 에러명 | 메시지 | HTTP |
| --- | --- | --- | --- |
| G201 | FILE_UPLOAD_FAILED | 파일 업로드에 실패했습니다. | 400 |
| G202 | INVALID_FILE_TYPE | 지원하지 않는 파일 형식입니다. | 400 |
| G203 | FILE_SIZE_EXCEEDED | 파일 크기가 제한을 초과했습니다. | 400 |

### 외부 API

| 코드 | 에러명 | 메시지 | HTTP |
| --- | --- | --- | --- |
| G301 | EXTERNAL_API_ERROR | 외부 API 호출에 실패했습니다. | 502 |
| G302 | KAKAO_API_ERROR | 카카오 API 호출에 실패했습니다. | 502 |

---

## User 에러코드

| 코드 | 에러명 | 메시지 | HTTP |
| --- | --- | --- | --- |
| U001 | USER_NOT_FOUND | 존재하지 않는 사용자입니디. | 404 |
| U002 | NICKNAME_ALREADY_EXISTS | 이미 존재하는 사용자 닉네임입니다. | 409 |
| U003 | NICKNAME_EMPTY | 닉네임은 필수 입력 항목입니다. | 400 |
| U004 | NICKNAME_LENGTH_INVALID | 닉네임은 2자 이상 20자 이하로 입력해주세요. | 400 |
| U005 | NICKNAME_FORMAT_INVALID | 닉네임은 한글, 영문, 숫자만 사용 가능합니다. | 400 |

---

## Book 에러코드

| 코드 | 에러명 | 메시지 | HTTP |
| --- | --- | --- | --- |
| B001 | BOOK_NOT_FOUND | 책을 찾을 수 없습니다. | 404 |
| B002 | BOOK_ALREADY_EXISTS | 이미 등록된 책입니다. | 409 |
| B003 | BOOK_NOT_IN_SHELF | 책장에 해당 책이 존재하지 않습니다. | 404 |
| B004 | BOOK_REVIEW_NOT_FOUND | 책 리뷰를 찾을 수 없습니다. | 404 |
| B005 | BOOK_REVIEW_ALREADY_EXISTS | 이미 책 리뷰가 존재합니다. | 409 |
| B006 | KEYWORD_NOT_FOUND | 키워드를 찾을 수 없습니다. | 404 |
| B007 | KEYWORD_NOT_SELECTABLE | 선택할 수 없는 키워드입니다. | 400 |
| B008 | BOOK_REVIEW_INVALID_RATING | 별점은 0.5 단위의 5점 만점 값이어야 합니다. | 400 |
| B009 | BOOK_REVIEW_DELETED | 삭제된 책 리뷰입니다. | 400 |
| B010 | BOOK_REVIEW_ACCESS_DENIED_NOT_WRITTEN | 평가를 작성한 사용자만 조회할 수 있습니다. | 403 |

---

## Record 에러코드 (개인기록)

| 코드 | 에러명 | 메시지 | HTTP |
| --- | --- | --- | --- |
| R001 | INVALID_RECORD_REQUEST | 기록 타입에 필요한 입력값이 누락되었습니다. | 400 |
| R002 | INVALID_RECORD_TYPE | 존재하지 않는 타입입니다. | 400 |
| R003 | RECORD_NOT_FOUND | 기록을 찾을 수 없습니다. | 404 |
| R004 | RECORD_ALREADY_DELETED | 이미 삭제된 기록입니다. | 409 |

---

## Gathering 에러코드 (모임)

| 코드 | 에러명 | 메시지 | HTTP |
| --- | --- | --- | --- |
| GA001 | GATHERING_NOT_FOUND | 모임을 찾을 수 없습니다. | 404 |
| GA002 | NOT_GATHERING_MEMBER | 모임의 멤버가 아닙니다. | 403 |
| GA003 | NOT_GATHERING_LEADER | 리더만 가능한 작업입니다. | 403 |
| GA004 | ALREADY_INACTIVE | 이미 비활성 상태인 모임은 삭제할 수 없습니다. | 409 |
| GA005 | CANNOT_REMOVE_LEADER | 유일한 리더는 강퇴할 수 없습니다. | 403 |
| GA006 | ALREADY_REMOVED_MEMBER | 이미 제거된 멤버입니다. | 409 |
| GA007 | INVITATION_CODE_GENERATION_FAILED | 초대 코드 생성에 실패했습니다. 다시 시도해주세요. | 500 |
| GA008 | ALREADY_GATHERING_MEMBER | 이미 가입된 모임입니다. | 409 |
| GA009 | JOIN_REQUEST_ALREADY_PENDING | 이미 가입 요청이 진행 중입니다. | 409 |
| GA010 | INVALID_INVITATION_LINK | 초대링크는 필수입니다. | 400 |
| GA011 | NOT_PENDING_STATUS | 대기 중인 가입 요청만 처리할 수 있습니다. | 400 |
| GA012 | INVALID_APPROVE_TYPE | 승인 상태는 ACTIVE 또는 REJECTED만 가능합니다. | 400 |
| GA013 | FAVORITE_LIMIT_EXCEEDED | 즐겨찾기는 최대 4개까지만 등록할 수 있습니다. | 400 |

---

## Meeting 에러코드 (약속)

| 코드 | 에러명 | 메시지 | HTTP |
| --- | --- | --- | --- |
| M001 | MEETING_NOT_FOUND | 약속을 찾을 수 없습니다. | 404 |
| M002 | GATHERING_NOT_FOUND | 모임을 찾을 수 없습니다. | 404 |
| M003 | NOT_GATHERING_MEETING | 모임에 속한 약속이 아닙니다. | 403 |
| M004 | NOT_MEETING_MEMBER | 약속의 멤버가 아닙니다. | 403 |
| M005 | MEETING_MEMBER_NOT_FOUND | 해당 약속의 멤버들을 찾을 수 없습니다. | 403 |
| M006 | NOT_MEETING_LEADER | 약속장만 수정할 수 있습니다. | 403 |
| M007 | MEETING_ALREADY_CONFIRMED | 약속이 확정된 경우에는 주제를 제안할 수 없습니다. | 400 |
| M008 | MEETING_FULL | 약속 정원이 마감되었습니다. | 400 |
| M009 | INVALID_MEETING_STATUS_CHANGE | 약속 상태를 변경할 수 없습니다. | 400 |
| M010 | MEETING_ALREADY_JOINED | 이미 참가한 약속입니다. | 400 |
| M011 | MEETING_ALREADY_CANCELED | 이미 취소된 약속입니다. | 400 |
| M012 | MEETING_CANCEL_NOT_ALLOWED | 약속 시작 24시간 이내에는 취소할 수 없습니다. | 400 |
| M013 | INVALID_MAX_PARTICIPANTS | 최대 참가 인원은 1명 이상이어야 하며, 모임 전체 인원을 초과할 수 없습니다. | 400 |
| M014 | MAX_PARTICIPANTS_LESS_THAN_CURRENT | 현재 참가 확정된 인원 수보다 적게 수정할 수 없습니다. | 400 |
| M015 | MEETING_DELETE_NOT_ALLOWED | 약속 시작 24시간 이내에는 삭제할 수 없습니다. | 400 |
| M016 | MEETING_JOIN_NOT_ALLOWED | 약속 시작 24시간 이내에는 참가 신청할 수 없습니다. | 400 |
| M017 | MEETING_UPDATE_NOT_ALLOWED | 약속 시작 24시간 이내에는 수정할 수 없습니다. | 400 |
| M018 | MEETING_NOT_CONFIRMED | 약속이 확정된 경우에만 주제를 제안할 수 있습니다. | 400 |
| M019 | MEETING_DATE_REQUIRED | 약속 시작/종료 일시는 필수입니다. | 400 |

---

## Topic 에러코드 (주제)

| 코드 | 에러명 | 메시지 | HTTP |
| --- | --- | --- | --- |
| E101 | TOPIC_NOT_FOUND | 주제를 찾을 수 없습니다. | 404 |
| E102 | TOPIC_NOT_IN_MEETING | 해당 주제는 지정한 약속에 속하지 않습니다. | 404 |
| E103 | TOPIC_ANSWER_NOT_FOUND | 답변을 찾을 수 없습니다. | 404 |
| E104 | TOPIC_ANSWER_ALREADY_SUBMITTED | 이미 제출된 답변입니다. | 409 |
| E105 | TOPIC_USER_CANNOT_DELETE | 사용자에게 주제 삭제 권한이 없습니다. | 404 |
| E106 | TOPIC_ALREADY_DELETED | 이미 삭제된 주제입니다. | 409 |
| E107 | TOPIC_ANSWER_ALREADY_DELETED | 이미 삭제된 답변입니다. | 409 |
| E108 | TOPIC_ANSWER_ALREADY_EXISTS | 이미 답변이 존재합니다. | 409 |

---

## Retrospective 에러코드 (회고)

| 코드 | 에러명 | 메시지 | HTTP |
| --- | --- | --- | --- |
| R101 | RETROSPECTIVE_ALREADY_EXISTS | 이미 해당 약속에 대한 회고가 존재합니다. | 409 |
| R102 | RETROSPECTIVE_NOT_FOUND | 회고를 찾을 수 없습니다. | 404 |
| R103 | MEETING_RETROSPECTIVE_NOT_FOUND | 공동 회고 내용을 찾을 수 없습니다. | 404 |
| R104 | RETROSPECTIVE_ALREADY_DELETED | 이미 삭제된 개인 회고입니다. | 404 |
| R105 | NO_ACCESS_RETROSPECTIVE | 회고에 접근할 권한이 없습니다. | 403 |
| R106 | SUMMARY_NOT_FOUND | AI 요약을 찾을 수 없습니다. | 404 |
| R107 | NOT_AUTHOR_OF_RETROSPECTIVE | 사용자가 작성한 회고가 아닙니다. | 403 |

---

## Storage 에러코드 (파일 저장소)

| 코드 | 에러명 | 메시지 | HTTP |
| --- | --- | --- | --- |
| S001 | FILE_UPLOAD_FAILED | 파일 업로드에 실패했습니다. | 500 |
| S002 | FILE_DELETE_FAILED | 파일 삭제에 실패했습니다. | 500 |
| S003 | INVALID_FILE_TYPE | 지원하지 않는 파일 형식입니다. | 400 |
| S004 | FILE_SIZE_EXCEEDED | 파일 크기가 제한을 초과했습니다. | 400 |
| S005 | BUCKET_NOT_FOUND | 스토리지 버킷을 찾을 수 없습니다. | 500 |
| S006 | PRESIGNED_URL_GENERATION_FAILED | Presigned URL 생성에 실패했습니다. | 500 |

---

## OAuth2 에러코드 (소셜로그인)

### OAuth2 인증 관련

| 코드 | 에러명 | 메시지 |
| --- | --- | --- |
| O001 | INVALID_OAUTH_PROVIDER | 지원하지 않는 소셜 로그인입니다. |
| O002 | OAUTH_AUTHENTICATION_FAILED | OAuth 인증에 실패했습니다. |
| O003 | INVALID_USER_PRINCIPAL | 사용자 인증 정보를 추출할 수 없습니다. |

### 카카오 사용자 정보 추출

| 코드 | 에러명 | 메시지 |
| --- | --- | --- |
| O101 | INVALID_KAKAO_ID | 카카오 ID를 추출할 수 없습니다. |
| O102 | INVALID_KAKAO_EMAIL | 카카오 이메일 정보가 올바르지 않습니다. |
| O103 | INVALID_KAKAO_RESPONSE | 카카오 응답 데이터가 올바르지 않습니다. |

---

## HTTP 상태코드 요약

| HTTP | 의미 | 사용 케이스 |
| --- | --- | --- |
| 400 | Bad Request | 잘못된 요청, 유효성 검증 실패 |
| 401 | Unauthorized | 인증 필요, 토큰 만료/무효 |
| 403 | Forbidden | 권한 없음 (인증O, 인가X) |
| 404 | Not Found | 리소스 없음 |
| 405 | Method Not Allowed | HTTP 메서드 불일치 |
| 409 | Conflict | 중복, 이미 존재함 |
| 500 | Internal Server Error | 서버 내부 오류 |
| 502 | Bad Gateway | 외부 API 호출 실패 |
