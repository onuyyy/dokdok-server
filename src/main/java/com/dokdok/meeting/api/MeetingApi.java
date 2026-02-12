package com.dokdok.meeting.api;

import com.dokdok.global.response.ApiResponse;
import com.dokdok.meeting.dto.MeetingCreateRequest;
import com.dokdok.meeting.dto.MeetingDetailResponse;
import com.dokdok.meeting.dto.MeetingResponse;
import com.dokdok.meeting.dto.MeetingStatusResponse;
import com.dokdok.meeting.dto.MeetingTabCountsResponse;
import com.dokdok.meeting.dto.MeetingUpdateRequest;
import com.dokdok.meeting.dto.MeetingUpdateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@Tag(name = "약속 관리", description = "약속 관련 API")
public interface MeetingApi {

    @Operation(
            summary = "약속 상세 조회 (developer: 김윤영)",
            description = """
            약속 상세 정보를 조회합니다.
            - 약속 이름, 책 이름, 약속 일시, 주제 타입 확인
            - 정렬 필터: 최신순/이름순
            - 권한: 모임장, 모임원, 약속장
            - 제약: 모임에 속한 사용자만 조회 가능
            """,
            parameters = {
                    @Parameter(name = "meetingId", description = "약속 식별자", in = ParameterIn.PATH, required = true)
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "약속 상세 조회 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MeetingDetailResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "SUCCESS",
                                      "message": "약속 상세 조회에 성공했습니다.",
                                      "data": {
                                        "meetingId": 1,
                                        "meetingName": "1월 독서 모임",
                                        "meetingStatus": "CONFIRMED",
                                        "gathering": {
                                          "gatheringId": 1,
                                          "gatheringName": "독서 모임"
                                        },
                                        "book": {
                                          "bookId": 1,
                                          "bookName": "클린 코드",
                                          "authors": "로버트 C. 마틴",
                                          "thumbnail": "https://example.com/thumb.jpg"
                                        },
                                        "schedule": {
                                          "startDateTime": "2025-02-01T14:00:00",
                                          "endDateTime": "2025-02-01T16:00:00",
                                          "displayDate": "2025.02.01(토) 14:00 ~ 2025.02.01(토) 16:00"
                                        },
                                        "location": {
                                          "name": "강남 스터디룸 A",
                                          "address": "서울 강남구 ...",
                                          "latitude": 37.4979,
                                          "longitude": 127.0276
                                        },
                                        "participants": {
                                          "currentCount": 2,
                                          "maxCount": 10,
                                          "members": [
                                            {
                                              "userId": 1,
                                              "nickname": "독서왕",
                                              "profileImageUrl": "https://example.com/profile.jpg",
                                              "role": "LEADER"
                                            },
                                            {
                                              "userId": 2,
                                              "nickname": "모임원A",
                                              "profileImageUrl": "https://example.com/profile2.jpg",
                                              "role": "MEMBER"
                                            }
                                          ]
                                        },
                                        "actionState": {
                                          "type": "CAN_EDIT",
                                          "buttonLabel": "수정하기",
                                          "enabled": true
                                        }
                                      }
                                    }
                                    """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "INVALID_INPUT_VALUE", "message": "입력값이 올바르지 않습니다.", "data": null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "약속을 찾을 수 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "M001", "message": "약속을 찾을 수 없습니다.", "data": null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "E000", "message": "서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.", "data": null}
                                    """)))
    })
    ResponseEntity<ApiResponse<MeetingDetailResponse>> findMeeting(
            Long meetingId
    );

    @Operation(
            summary = "약속 생성 신청 (developer: 김윤영)",
            description = """
            모임 구성원이 약속 생성을 신청합니다.
            - 입력: 약속 제목(미입력 시 책 제목), 책 정보(제목/저자/출판사/ISBN/썸네일), 약속 일시*, 최대 인원 수(null 허용), 장소 정보(null 허용)
            - 권한: 해당 모임의 구성원
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "약속 생성 요청 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MeetingResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "CREATED",
                                      "message": "약속 생성 요청에 성공했습니다.",
                                      "data": {
                                        "meetingId": 1,
                                        "meetingName": "1월 독서 모임",
                                        "meetingStatus": "PENDING",
                                        "gathering": {
                                          "gatheringId": 1,
                                          "gatheringName": "독서 모임"
                                        },
                                        "book": {
                                          "bookId": 1,
                                          "bookName": "클린 코드",
                                          "thumbnail": "https://example.com/thumb.jpg"
                                        },
                                        "schedule": {
                                          "date": "2025-02-01",
                                          "time": "14:00:00",
                                          "startDateTime": "2025-02-01T14:00:00",
                                          "endDateTime": "2025-02-01T16:00:00"
                                        },
                                        "location": {
                                          "name": "강남 스터디룸 A",
                                          "address": "서울 강남구 ...",
                                          "latitude": 37.4979,
                                          "longitude": 127.0276
                                        },
                                        "participants": {
                                          "currentCount": 1,
                                          "maxCount": 10,
                                          "members": [
                                            {
                                              "userId": 1,
                                              "nickname": "독서왕",
                                              "profileImageUrl": "https://example.com/profile.jpg"
                                            }
                                          ]
                                        }
                                      }
                                    }
                                    """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "INVALID_INPUT_VALUE", "message": "입력값이 올바르지 않습니다.", "data": null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "모임 또는 책 또는 사용자를 찾을 수 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "M002", "message": "모임을 찾을 수 없습니다.", "data": null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "E000", "message": "서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.", "data": null}
                                    """)))
    })
    ResponseEntity<ApiResponse<MeetingResponse>> createMeeting(
            @RequestBody(
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {
                                      "gatheringId": 1,
                                      "book": {
                                        "title": "클린 코드",
                                        "authors": "로버트 C. 마틴",
                                        "publisher": "인사이트",
                                        "isbn": "9788966260959",
                                        "thumbnail": "https://example.com/thumb.jpg"
                                      },
                                      "meetingName": "1월 독서 모임",
                                      "meetingStartDate": "2025-02-01T14:00:00",
                                      "meetingEndDate": "2025-02-01T16:00:00",
                                      "maxParticipants": 10,
                                      "location": {
                                        "name": "강남 스터디룸 A",
                                        "address": "서울 강남구 ...",
                                        "latitude": 37.4979,
                                        "longitude": 127.0276
                                      }
                                    }
                                    """))
            )
            MeetingCreateRequest request
    );

    @Operation(
            summary = "약속 확정 (developer: 김윤영)",
            description = """
            신청된 약속을 확정합니다.
            - 상태: PENDING(신청), CONFIRMED(모임장 확정), REJECTED(거절), DONE(종료)
            - 확정 시 신청자가 약속장으로 변경된다.
            - 약속장은 자동으로 약속에 포함된다 (MeetingMember).
            - 확정된 약속은 되돌릴 수 없다.
            - 모임장은 만든 약속을 바로 확정할 수 있다.
            - 동일 시간에 약속 하나만 확정 가능
            - 신청된 약속이 없으면 모임장이 약속을 바로 생성/확정 가능
            """,
            parameters = {
                    @Parameter(name = "meetingId", description = "약속 식별자", in = ParameterIn.PATH, required = true)
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "약속 확정 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MeetingStatusResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "UPDATED",
                                      "message": "약속 확정에 성공했습니다.",
                                      "data": {
                                        "meetingId": 1,
                                        "meetingStatus": "CONFIRMED",
                                        "confirmedAt": "2025-01-25T10:30:00"
                                      }
                                    }
                                    """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "M009", "message": "약속 상태를 변경할 수 없습니다.", "data": null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "약속을 찾을 수 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "M001", "message": "약속을 찾을 수 없습니다.", "data": null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "E000", "message": "서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.", "data": null}
                                    """)))
    })
    ResponseEntity<ApiResponse<MeetingStatusResponse>> confirmMeeting(
            Long meetingId
    );

    @Operation(
            summary = "약속 거절 (developer: 김윤영)",
            description = """
            신청된 약속을 거절합니다.
            - 상태: PENDING(신청), CONFIRMED(모임장 확정), REJECTED(거절), DONE(종료)
            - 거절된 약속은 되돌릴 수 없다.
            """,
            parameters = {
                    @Parameter(name = "meetingId", description = "약속 식별자", in = ParameterIn.PATH, required = true)
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "약속 거절 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MeetingStatusResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "UPDATED",
                                      "message": "약속 거절에 성공했습니다.",
                                      "data": {
                                        "meetingId": 1,
                                        "meetingStatus": "REJECTED",
                                        "confirmedAt": null
                                      }
                                    }
                                    """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "M009", "message": "약속 상태를 변경할 수 없습니다.", "data": null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "약속을 찾을 수 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "M001", "message": "약속을 찾을 수 없습니다.", "data": null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "E000", "message": "서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.", "data": null}
                                    """)))
    })
    ResponseEntity<ApiResponse<MeetingStatusResponse>> rejectMeeting(
            Long meetingId
    );

    @Operation(
            summary = "약속 참가 신청 (developer: 김윤영)",
            description = """
            약속에 참가 신청합니다.
            - 권한: 모임원 전원
            - 제약: 모집 정원 마감 전
            - 제약: 약속 시작 24시간 이내 참가 신청 불가
            """,
            parameters = {
                    @Parameter(name = "meetingId", description = "약속 식별자", in = ParameterIn.PATH, required = true)
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "약속 참가 신청 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Long.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "SUCCESS",
                                      "message": "약속 참가 신청에 성공했습니다.",
                                      "data": 1
                                    }
                                    """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(name = "정원 마감", value = """
                                            {"code": "M008", "message": "약속 정원이 마감되었습니다.", "data": null}
                                            """),
                                    @ExampleObject(name = "이미 참가", value = """
                                            {"code": "M010", "message": "이미 참가한 약속입니다.", "data": null}
                                            """),
                                    @ExampleObject(name = "24시간 이내 참가 불가", value = """
                                            {"code": "M016", "message": "약속 시작 24시간 이내에는 참가 신청할 수 없습니다.", "data": null}
                                            """)
                            })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "모임 멤버가 아님",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "M004", "message": "약속의 멤버가 아닙니다.", "data": null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "약속 또는 사용자를 찾을 수 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "M001", "message": "약속을 찾을 수 없습니다.", "data": null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "E000", "message": "서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.", "data": null}
                                    """)))
    })
    ResponseEntity<ApiResponse<Long>> joinMeeting(
            Long meetingId
    );

    @Operation(
            summary = "약속 참가 취소 (developer: 김윤영)",
            description = """
            약속 참가 신청을 취소합니다.
            - 권한: 약속에 참여한 모임원
            - 제약: 약속 시작 24시간 이내 취소 불가
            - 주제를 등록한 참가자가 취소하면 주제도 삭제 처리
            """,
            parameters = {
                    @Parameter(name = "meetingId", description = "약속 식별자", in = ParameterIn.PATH, required = true)
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "약속 참가 취소 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Long.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "SUCCESS",
                                      "message": "약속 참가 취소에 성공했습니다.",
                                      "data": 1
                                    }
                                    """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(name = "취소 불가", value = """
                                            {"code": "M012", "message": "약속 시작 24시간 이내에는 취소할 수 없습니다.", "data": null}
                                            """),
                                    @ExampleObject(name = "이미 취소", value = """
                                            {"code": "M011", "message": "이미 취소된 약속입니다.", "data": null}
                                            """)
                            })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "약속 멤버가 아님",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "M004", "message": "약속의 멤버가 아닙니다.", "data": null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "약속을 찾을 수 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "M001", "message": "약속을 찾을 수 없습니다.", "data": null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "E000", "message": "서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.", "data": null}
                                    """)))
    })
    ResponseEntity<ApiResponse<Long>> cancelMeeting(
            Long meetingId
    );

    @Operation(
            summary = "약속 수정 (developer: 김윤영)",
            description = """
            약속 정보를 수정합니다.
            - 권한: 약속장
            - 제약: 종료된 약속은 수정 불가
            - 제약: 약속 시작 24시간 이내 수정 불가
            - 제약: 종료 일시는 시작 일시보다 이전일 수 없음
            - 제약: 최대 참여 인원은 현재 참여 인원보다 작을 수 없음
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "약속 수정 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MeetingUpdateResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "UPDATED",
                                      "message": "약속 수정에 성공했습니다.",
                                      "data": {
                                        "meetingId": 1,
                                        "meetingName": "1월 독서 모임 (수정)",
                                        "startDate": "2025-02-01T14:00:00",
                                        "endDate": "2025-02-01T16:00:00",
                                        "location": {
                                          "name": "강남 스터디룸 A",
                                          "address": "서울 강남구 ...",
                                          "latitude": 37.4979,
                                          "longitude": 127.0276
                                        },
                                        "maxParticipants": 10
                                      }
                                    }
                                    """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(name = "최대 인원 오류", value = """
                                            {"code": "M014", "message": "현재 참가 확정된 인원 수보다 적게 수정할 수 없습니다.", "data": null}
                                            """),
                                    @ExampleObject(name = "잘못된 최대 인원", value = """
                                            {"code": "M013", "message": "최대 참가 인원은 1명 이상이어야 하며, 모임 전체 인원을 초과할 수 없습니다.", "data": null}
                                            """),
                                    @ExampleObject(name = "24시간 이내 수정 불가", value = """
                                            {"code": "M017", "message": "약속 시작 24시간 이내에는 수정할 수 없습니다.", "data": null}
                                            """)
                            })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "약속장 아님",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "M006", "message": "약속장만 수정할 수 있습니다.", "data": null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "약속을 찾을 수 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "M001", "message": "약속을 찾을 수 없습니다.", "data": null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "E000", "message": "서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.", "data": null}
                                    """)))
    })
    ResponseEntity<ApiResponse<MeetingUpdateResponse>> updateMeeting(
            @Parameter(name = "meetingId", description = "약속 식별자", in = ParameterIn.PATH, required = true)
            Long meetingId,
            MeetingUpdateRequest request
    );

    @Operation(
            summary = "약속 삭제 (developer: 김윤영)",
            description = """
            약속을 삭제합니다.
            - 권한: 모임장
            - 제약: 종료된 약속은 삭제 불가
            - 제약: 약속 시작 24시간 이내 삭제 불가
            - 약속 삭제 시 연관된 데이터도 삭제 처리
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "약속 삭제 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Void.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "DELETED",
                                      "message": "약속 삭제에 성공했습니다.",
                                      "data": null
                                    }
                                    """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "M015", "message": "약속 시작 24시간 이내에는 삭제할 수 없습니다.", "data": null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "모임장 아님",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "ACCESS_DENIED", "message": "접근 권한이 없습니다.", "data": null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "약속을 찾을 수 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "M001", "message": "약속을 찾을 수 없습니다.", "data": null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "E000", "message": "서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.", "data": null}
                                    """)))
    })
    ResponseEntity<ApiResponse<Void>> deleteMeeting(
            @Parameter(name = "meetingId", description = "약속 식별자", in = ParameterIn.PATH, required = true)
            Long meetingId
    );

    @Operation(
            summary = "약속 탭 카운트 조회 (developer: 김윤영)",
            description = """
            모임의 약속 탭별 카운트를 조회합니다.
            - 전체: 확정된 약속
            - 다가오는 약속: 3일 이내 시작하는 확정된 약속
            - 완료된 약속: 종료된 약속
            - 내가 참여한 약속: 완료된 약속 중 참여한 약속
            """,
            parameters = {
                    @Parameter(name = "gatheringId", description = "모임 식별자", in = ParameterIn.QUERY, required = true)
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "약속 탭 카운트 조회 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MeetingTabCountsResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "SUCCESS",
                                      "message": "약속 탭 카운트 조회에 성공했습니다.",
                                      "data": {
                                        "all": 10,
                                        "upcoming": 2,
                                        "done": 5,
                                        "joined": 3
                                      }
                                    }
                                    """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "INVALID_INPUT_VALUE", "message": "입력값이 올바르지 않습니다.", "data": null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "모임 멤버가 아님",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "M003", "message": "모임에 속한 약속이 아닙니다.", "data": null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "모임을 찾을 수 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "M002", "message": "모임을 찾을 수 없습니다.", "data": null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "E000", "message": "서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.", "data": null}
                                    """)))
    })
    ResponseEntity<ApiResponse<MeetingTabCountsResponse>> getMeetingTabCounts(
            Long gatheringId
    );

}
