package com.dokdok.meeting.api;

import com.dokdok.global.response.ApiResponse;
import com.dokdok.global.response.PageResponse;
import com.dokdok.meeting.dto.MeetingListFilter;
import com.dokdok.meeting.dto.MeetingListItemPageResponse;
import com.dokdok.meeting.dto.MeetingListItemResponse;
import com.dokdok.meeting.entity.MeetingStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "약속 관리", description = "약속 관련 API")
public interface MeetingListApi {

    @Operation(
            summary = "모임 약속 리스트 조회 (developer: 김윤영)",
            description = """
            모임 내 약속 리스트를 조회합니다.
            - 정렬 기준: 약속 시작 시간 오름차순, 약속 ID 오름차순
            - 전체: 확정된 약속
            - 다가오는 약속: 3일 이내 시작하는 확정된 약속
            - 완료된 약속: 종료된 약속
            - 내가 참여한 약속: 완료된 약속 중 참여한 약속
            """,
            parameters = {
                    @Parameter(name = "gatheringId", description = "모임 식별자", in = ParameterIn.PATH, required = true),
                    @Parameter(name = "filter", description = "탭 필터 (ALL, UPCOMING, DONE, JOINED)",
                            in = ParameterIn.QUERY, required = true,
                            schema = @Schema(allowableValues = {"ALL", "UPCOMING", "DONE", "JOINED"}))
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "약속 리스트 조회 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MeetingListItemPageResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "SUCCESS",
                                      "message": "약속 리스트 조회 성공",
                                      "data": {
                                        "items": [
                                          {
                                            "meetingId": 1,
                                            "meetingName": "1월 독서 모임",
                                            "meetingLeaderName": "독서왕",
                                            "bookName": "클린 코드",
                                            "startDateTime": "2025-02-01T14:00:00",
                                            "endDateTime": "2025-02-01T16:00:00",
                                            "meetingStatus": "CONFIRMED"
                                          }
                                        ],
                                        "totalCount": 12,
                                        "currentPage": 0,
                                        "pageSize": 5,
                                        "totalPages": 3
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
    ResponseEntity<ApiResponse<PageResponse<MeetingListItemResponse>>> getMeetingList(
            @PathVariable Long gatheringId,
            @RequestParam MeetingListFilter filter,
            @ParameterObject
            @PageableDefault(size = 5, sort = {"meetingStartDate", "id"}) Pageable pageable
    );

    @Operation(
            summary = "모임장 약속 승인 리스트 조회 (developer: 김윤영)",
            description = """
            모임장이 약속 승인 상태별 리스트를 조회합니다.
            - 확정 대기: PENDING
            - 확정 완료: CONFIRMED
            - 거절(REJECTED)은 노출되지 않음
            """,
            parameters = {
                    @Parameter(name = "gatheringId", description = "모임 식별자", in = ParameterIn.PATH, required = true),
                    @Parameter(name = "status", description = "상태 (PENDING, CONFIRMED)",
                            in = ParameterIn.QUERY, required = true,
                            schema = @Schema(allowableValues = {"PENDING", "CONFIRMED"}))
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "약속 승인 리스트 조회 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MeetingListItemPageResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "SUCCESS",
                                      "message": "약속 승인 리스트 조회 성공",
                                      "data": {
                                        "items": [
                                          {
                                            "meetingId": 1,
                                            "meetingName": "1월 독서 모임",
                                            "meetingLeaderName": "독서왕",
                                            "bookName": "클린 코드",
                                            "startDateTime": "2025-02-01T14:00:00",
                                            "endDateTime": "2025-02-01T16:00:00",
                                            "meetingStatus": "PENDING"
                                          }
                                        ],
                                        "totalCount": 1,
                                        "currentPage": 0,
                                        "pageSize": 10,
                                        "totalPages": 1
                                      }
                                    }
                                    """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "INVALID_INPUT_VALUE", "message": "입력값이 올바르지 않습니다.", "data": null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "모임장 아님",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "ACCESS_DENIED", "message": "접근 권한이 없습니다.", "data": null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "모임을 찾을 수 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "M002", "message": "모임을 찾을 수 없습니다.", "data": null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "E-000", "message": "서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.", "data": null}
                                    """)))
    })
    ResponseEntity<ApiResponse<PageResponse<MeetingListItemResponse>>> getApprovalMeetingList(
            @PathVariable Long gatheringId,
            @RequestParam MeetingStatus status,
            @ParameterObject
            @PageableDefault(size = 10) Pageable pageable
    );
}
