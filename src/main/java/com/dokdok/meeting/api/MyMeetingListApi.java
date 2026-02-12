package com.dokdok.meeting.api;

import com.dokdok.global.response.ApiResponse;
import com.dokdok.global.response.CursorResponse;
import com.dokdok.meeting.dto.MeetingListCursor;
import com.dokdok.meeting.dto.MeetingListCursorRequest;
import com.dokdok.meeting.dto.MyMeetingListFilter;
import com.dokdok.meeting.dto.MyMeetingListItemCursorResponse;
import com.dokdok.meeting.dto.MyMeetingListItemResponse;
import com.dokdok.meeting.dto.MyMeetingTabCountsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "약속 관리", description = "약속 관련 API")
public interface MyMeetingListApi {

    @Operation(
            summary = "메인페이지 내 약속 리스트 조회 (developer: 김윤영)",
            description = """
            로그인 사용자의 모든 모임 내 약속 리스트를 조회합니다.
            - 정렬 기준: 약속 시작 시간 오름차순, 약속 ID 오름차순
            - 전체: 확정된 약속 + 완료된 약속
            - 다가오는 약속: 3일 이내 시작하는 확정된 약속
            - 완료된 약속: 종료된 약속
            """,
            parameters = {
                    @Parameter(name = "filter", description = "필터 (ALL, UPCOMING, DONE)",
                            in = ParameterIn.QUERY, required = true,
                            schema = @Schema(allowableValues = {"ALL", "UPCOMING", "DONE"}))
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "내 약속 리스트 조회 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MyMeetingListItemCursorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "SUCCESS",
                                      "message": "내 약속 리스트 조회 성공",
                                      "data": {
                                        "items": [
                                          {
                                            "meetingId": 1,
                                            "meetingName": "1월 독서 모임",
                                            "gatheringId": 10,
                                            "gatheringName": "독서 모임",
                                            "meetingLeaderName": "독서왕",
                                            "bookName": "클린 코드",
                                            "startDateTime": "2025-02-01T14:00:00",
                                            "endDateTime": "2025-02-01T16:00:00",
                                            "meetingStatus": "CONFIRMED",
                                            "myRole": "LEADER",
                                            "progressStatus": "UPCOMING"
                                          }
                                        ],
                                        "totalCount": 8,
                                        "pageSize": 4,
                                        "hasNext": true,
                                        "nextCursor": {
                                          "meetingId": 2,
                                          "startDateTime": "2025-02-03T14:00:00"
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
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "E000", "message": "서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.", "data": null}
                                    """)))
    })
    ResponseEntity<ApiResponse<CursorResponse<MyMeetingListItemResponse, MeetingListCursor>>> getMyMeetingList(
            @RequestParam MyMeetingListFilter filter,
            @ParameterObject MeetingListCursorRequest cursor,
            @RequestParam(defaultValue = "4") int size
    );

    @Operation(
            summary = "메인페이지 내 약속 탭 카운트 조회 (developer: 김윤영)",
            description = """
            로그인 사용자의 내 약속 탭 카운트를 조회합니다.
            - 전체: 확정된 약속 + 완료된 약속
            - 다가오는 약속: 3일 이내 시작하는 확정된 약속
            - 완료된 약속: 종료된 약속
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "내 약속 탭 카운트 조회 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MyMeetingTabCountsResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "SUCCESS",
                                      "message": "내 약속 탭 카운트 조회 성공",
                                      "data": {
                                        "all": 10,
                                        "upcoming": 2,
                                        "done": 5
                                      }
                                    }
                                    """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "E000", "message": "서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.", "data": null}
                                    """)))
    })
    ResponseEntity<ApiResponse<MyMeetingTabCountsResponse>> getMyMeetingTabCounts();
}
