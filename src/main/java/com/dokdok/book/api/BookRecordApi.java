package com.dokdok.book.api;

import com.dokdok.global.response.ApiResponse;
import com.dokdok.global.response.CursorResponse;
import com.dokdok.retrospective.dto.response.RetrospectiveRecordResponse;
import com.dokdok.retrospective.dto.response.RetrospectiveRecordsCursor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

@Tag(name = "독서 기록", description = "책별 독서 기록 관련 API")
public interface BookRecordApi {

    @Operation(
            summary = "책별 개인 회고 목록 조회 (developer: 경서영)",
            description = """
                    특정 책에 대해 사용자가 작성한 개인 회고 목록을 커서 기반 페이지네이션으로 조회합니다.
                    - 권한: 인증된 사용자

                    **정렬 기준**
                    - 1차: createdAt(생성일시) 내림차순
                    - 2차: retrospectiveId 내림차순 (동점일 경우)

                    **사용 방법**
                    - 첫 페이지: `?pageSize=10` (커서 파라미터 없이 요청)
                    - 다음 페이지: `?pageSize=10&cursorCreatedAt={nextCursor.createdAt}&cursorRetrospectiveId={nextCursor.retrospectiveId}`

                    **응답 구조**
                    - hasNext: 다음 페이지 존재 여부
                    - nextCursor: 다음 페이지 요청 시 사용할 커서 (hasNext가 false면 null)
                    - items[].topicGroups: 주제별로 그룹화된 회고 데이터 (confirmOrder 순 정렬, null인 경우 마지막 배치)
                    - items[].freeTexts: 자유 기록 목록
                    """,
            parameters = {
                    @Parameter(name = "bookId", description = "책 식별자", in = ParameterIn.PATH, required = true),
                    @Parameter(name = "pageSize", description = "페이지 크기 (기본값: 10)", in = ParameterIn.QUERY, required = false),
                    @Parameter(name = "cursorCreatedAt", description = "커서 - 마지막 항목의 생성 시간 (ISO 8601 형식)", in = ParameterIn.QUERY, required = false),
                    @Parameter(name = "cursorRetrospectiveId", description = "커서 - 마지막 항목의 회고 ID", in = ParameterIn.QUERY, required = false)
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "개인 회고 목록 조회 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CursorResponse.class),
                            examples = @ExampleObject(value = """
                                    {"code":"SUCCESS","message":"해당 책의 개인 회고 목록 조회를 성공했습니다.","data":{"items":[{"retrospectiveId":1,"gatheringName":"독서 모임","recordType":"개인 회고","createdAt":"2025-02-01T16:30:00","topicGroups":[{"topicId":1,"topicTitle":"이 책의 핵심 메시지는 무엇인가?","confirmOrder":1,"changedThoughts":{"keyIssue":"요약된 핵심 쟁점","postOpinion":"토론 후 바뀐 생각"},"othersPerspectives":[{"meetingMemberId":10,"memberNickname":"독서왕","opinionContent":"상대 의견이 인상적이었습니다.","impressiveReason":"새로운 관점을 제공했기 때문입니다."}]}],"freeTexts":[{"title":"오늘의 한 줄","content":"회고 내용을 작성합니다."}]}],"pageSize":10,"hasNext":false,"nextCursor":null}}
                                    """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {"code": "G102", "message": "인증이 필요합니다.", "data": null}
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "책을 찾을 수 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {"code": "B001", "message": "책을 찾을 수 없습니다.", "data": null}
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {"code": "E000", "message": "서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.", "data": null}
                                            """
                            )
                    )
            )
    })
    @GetMapping(value = "/retrospectives", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<CursorResponse<RetrospectiveRecordResponse, RetrospectiveRecordsCursor>>> getRetrospectiveRecords(
            @PathVariable Long bookId,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cursorCreatedAt,
            @RequestParam(required = false) Long cursorRetrospectiveId
    );
}
