package com.dokdok.topic.api;

import com.dokdok.global.response.ApiResponse;
import com.dokdok.topic.dto.request.ConfirmTopicsRequest;
import com.dokdok.topic.dto.request.SuggestTopicRequest;
import com.dokdok.topic.dto.response.ConfirmTopicsResponse;
import com.dokdok.topic.dto.response.SuggestTopicResponse;
import com.dokdok.topic.dto.response.TopicLikeResponse;
import com.dokdok.topic.dto.response.TopicsWithActionsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "주제 관리", description = "주제 관련 API")
public interface TopicApi {

    @Operation(
            summary = "주제 제안 (developer: 경서영)",
            description = """
                    약속에 대한 주제를 제안합니다.
                    - 입력: 제목*, 설명*, 주제 타입*
                    - 주제 타입: FREE(자유형), DISCUSSION(토론형), EMOTION(감정 공유형), EXPERIENCE(경험 연결형), CHARACTER_ANALYSIS(인물 분석형), COMPARISON(비교 분석형), STRUCTURE(구조 분석형), IN_DEPTH(심층 분석형), CREATIVE(창작형), CUSTOM(질문형)
                    - 권한: 약속의 멤버
                    - 제약: 약속 상태가 주제 제안 가능 상태여야 함
                    """,
            parameters = {
                    @Parameter(name = "gatheringId", description = "모임 식별자", in = ParameterIn.PATH, required = true),
                    @Parameter(name = "meetingId", description = "약속 식별자", in = ParameterIn.PATH, required = true)
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "주제 제안 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SuggestTopicResponse.class),
                            examples = @ExampleObject(value = """
                                    {"code":"CREATED","message":"주제 제안이 완료되었습니다.","data":{"topicId":1,"title":"주제 제목","description":"주제 설명","topicType":"FREE"}}
                                    """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (유효성 검사 실패)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "제목 필수",
                                            description = "제목이 null이거나 빈 문자열인 경우",
                                            value = "{\"code\":\"G002\",\"message\":\"제목은 필수입니다\",\"data\":null}"
                                    ),
                                    @ExampleObject(
                                            name = "제목 길이 오류",
                                            description = "제목이 100자를 초과하는 경우",
                                            value = "{\"code\":\"G002\",\"message\":\"제목은 100자 이내여야 합니다\",\"data\":null}"
                                    ),
                                    @ExampleObject(
                                            name = "설명 필수",
                                            description = "설명이 null이거나 빈 문자열인 경우",
                                            value = "{\"code\":\"G002\",\"message\":\"설명은 필수입니다\",\"data\":null}"
                                    ),
                                    @ExampleObject(
                                            name = "주제 타입 필수",
                                            description = "주제 타입이 null인 경우",
                                            value = "{\"code\":\"G002\",\"message\":\"주제 타입은 필수입니다\",\"data\":null}"
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "{\"code\":\"G102\",\"message\":\"인증이 필요합니다.\",\"data\":null}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "약속의 멤버가 아님",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "{\"code\":\"M004\",\"message\":\"약속의 멤버가 아닙니다.\",\"data\":null}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "모임 또는 약속을 찾을 수 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "모임 없음",
                                            description = "모임을 찾을 수 없는 경우",
                                            value = "{\"code\":\"G001\",\"message\":\"모임을 찾을 수 없습니다.\",\"data\":null}"
                                    ),
                                    @ExampleObject(
                                            name = "약속 없음",
                                            description = "약속을 찾을 수 없는 경우",
                                            value = "{\"code\":\"M001\",\"message\":\"약속을 찾을 수 없습니다.\",\"data\":null}"
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "약속 상태 오류",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "{\"code\":\"M002\",\"message\":\"주제 제안이 불가능한 약속 상태입니다.\",\"data\":null}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "{\"code\":\"E000\",\"message\":\"서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.\",\"data\":null}"
                            )
                    )
            )
    })
    @PostMapping
    ResponseEntity<ApiResponse<SuggestTopicResponse>> createTopic(
            @PathVariable Long gatheringId,
            @PathVariable Long meetingId,
            @Valid @RequestBody SuggestTopicRequest request
    );

    @Operation(
            summary = "제안된 주제 조회 (developer: 경서영)",
            description = """
                    약속에 제안된 주제 목록을 커서 기반 페이지네이션으로 조회합니다.
                    - 권한: 모임의 멤버
                    - 주제 상태: PROPOSED(제안됨), CONFIRMED(확정됨)
                    - 주제 타입: FREE(자유형), DISCUSSION(토론형), EMOTION(감정 공유형), EXPERIENCE(경험 연결형), CHARACTER_ANALYSIS(인물 분석형), COMPARISON(비교 분석형), STRUCTURE(구조 분석형), IN_DEPTH(심층 분석형), CREATIVE(창작형), CUSTOM(질문형)

                    **정렬 기준**
                    - 1차: likeCount(좋아요 수) 내림차순
                    - 2차: topicId 오름차순 (동점일 경우)

                    **사용 방법**
                    - 첫 페이지: `?pageSize=10` (커서 파라미터 없이 요청)
                    - 다음 페이지: `?pageSize=10&cursorLikeCount={nextCursor.likeCount}&cursorTopicId={nextCursor.topicId}`

                    **응답 구조**
                    - items: 주제 목록
                    - pageSize: 페이지 크기
                    - hasNext: 다음 페이지 존재 여부
                    - nextCursor: 다음 페이지 요청 시 사용할 커서 (hasNext가 false면 null)
                    - totalCount : 전체 주제 수 (첫 요청 시만 포함, 이후 요청에서는 생략)
                    - actions: 현재 사용자의 권한 정보
                      - canConfirm: 주제 확정 가능 여부 (모임장과 약속장만 true)
                      - canSuggest: 주제 제안 가능 여부 (약속 멤버이고 약속 상태가 CONFIRMED일 때 true)
                    """,
            parameters = {
                    @Parameter(name = "gatheringId", description = "모임 식별자", in = ParameterIn.PATH, required = true),
                    @Parameter(name = "meetingId", description = "약속 식별자", in = ParameterIn.PATH, required = true),
                    @Parameter(name = "pageSize", description = "페이지 크기 (기본값: 10)", in = ParameterIn.QUERY),
                    @Parameter(name = "cursorLikeCount", description = "커서: 이전 페이지 마지막 항목의 좋아요 수", in = ParameterIn.QUERY),
                    @Parameter(name = "cursorTopicId", description = "커서: 이전 페이지 마지막 항목의 주제 ID", in = ParameterIn.QUERY)
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "주제 조회 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TopicsWithActionsResponse.class),
                            examples = @ExampleObject(value = """
                                    {"code":"SUCCESS","message":"제안된 주제 조회를 성공했습니다.","data":{"items":[{"topicId":1,"meetingId":10,"title":"이 책의 핵심 메시지는 무엇인가?","description":"저자가 전달하고자 하는 핵심 메시지에 대해 토론합니다.","topicType":"DISCUSSION","topicTypeLabel":"토론형","topicStatus":"PROPOSED","likeCount":5,"canDelete":true,"isLiked":false,"createdByInfo":{"userId":1,"nickname":"독서왕"}}],"pageSize":10,"hasNext":true,"nextCursor":{"likeCount":5,"topicId":1},"totalCount":25,"actions":{"canConfirm":false,"canSuggest":true}}}
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "{\"code\":\"G102\",\"message\":\"인증이 필요합니다.\",\"data\":null}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "모임 또는 약속을 찾을 수 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "모임 없음",
                                            description = "모임을 찾을 수 없는 경우",
                                            value = "{\"code\":\"G001\",\"message\":\"모임을 찾을 수 없습니다.\",\"data\":null}"
                                    ),
                                    @ExampleObject(
                                            name = "약속 없음",
                                            description = "약속을 찾을 수 없는 경우",
                                            value = "{\"code\":\"M001\",\"message\":\"약속을 찾을 수 없습니다.\",\"data\":null}"
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "{\"code\":\"E000\",\"message\":\"서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.\",\"data\":null}"
                            )
                    )
            )
    })
    @GetMapping
    ResponseEntity<ApiResponse<TopicsWithActionsResponse>> getTopics(
            @PathVariable Long gatheringId,
            @PathVariable Long meetingId,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer cursorLikeCount,
            @RequestParam(required = false) Long cursorTopicId
    );

    @Operation(
            summary = "제안된 주제 확정 (developer: 양재웅)",
            description = """
                    약속에서 제안된 주제를 확정합니다.
                    - 입력: 확정할 주제 ID 목록* (순서대로 confirmOrder 부여)
                    - 상태: PROPOSED(제안됨) → CONFIRMED(확정됨)
                    - 권한: 모임의 멤버
                    - 제약: 요청된 모든 주제가 해당 약속에 존재해야 함
                    """,
            parameters = {
                    @Parameter(name = "gatheringId", description = "모임 식별자", in = ParameterIn.PATH, required = true),
                    @Parameter(name = "meetingId", description = "약속 식별자", in = ParameterIn.PATH, required = true)
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "주제 확정 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ConfirmTopicsResponse.class),
                            examples = @ExampleObject(value = """
                                    {"code":"SUCCESS","message":"주제가 확정되었습니다.","data":{"meetingId":1,"topicStatus":"CONFIRMED","topics":[{"topicId":1,"confirmOrder":1},{"topicId":2,"confirmOrder":2},{"topicId":3,"confirmOrder":3}]}}
                                    """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "{\"code\":\"G002\",\"message\":\"입력값이 올바르지 않습니다.\",\"data\":null}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "{\"code\":\"G102\",\"message\":\"인증이 필요합니다.\",\"data\":null}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "모임 또는 약속의 멤버가 아님",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "모임 멤버 아님",
                                            description = "모임의 멤버가 아닌 경우",
                                            value = "{\"code\":\"G002\",\"message\":\"모임의 멤버가 아닙니다.\",\"data\":null}"
                                    ),
                                    @ExampleObject(
                                            name = "약속 멤버 아님",
                                            description = "약속의 멤버가 아닌 경우",
                                            value = "{\"code\":\"M004\",\"message\":\"약속의 멤버가 아닙니다.\",\"data\":null}"
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "모임, 약속 또는 주제를 찾을 수 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "모임 없음",
                                            description = "모임을 찾을 수 없는 경우",
                                            value = "{\"code\":\"G001\",\"message\":\"모임을 찾을 수 없습니다.\",\"data\":null}"
                                    ),
                                    @ExampleObject(
                                            name = "약속 없음",
                                            description = "약속을 찾을 수 없는 경우",
                                            value = "{\"code\":\"M001\",\"message\":\"약속을 찾을 수 없습니다.\",\"data\":null}"
                                    ),
                                    @ExampleObject(
                                            name = "주제 없음",
                                            description = "주제를 찾을 수 없는 경우",
                                            value = "{\"code\":\"E101\",\"message\":\"주제를 찾을 수 없습니다.\",\"data\":null}"
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "{\"code\":\"E000\",\"message\":\"서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.\",\"data\":null}"
                            )
                    )
            )
    })
    @PatchMapping(value = "/topics/confirm", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<ConfirmTopicsResponse>> confirmTopics(
            @PathVariable Long gatheringId,
            @PathVariable Long meetingId,
            @Valid @RequestBody ConfirmTopicsRequest request
    );

    @Operation(
            summary = "주제 삭제 (developer: 경서영)",
            description = """
                    제안된 주제를 삭제합니다.
                    - 권한: 주제 작성자 또는 약속장
                    - 제약: 약속의 멤버만 삭제 가능
                    - 제약: 이미 삭제된 주제는 다시 삭제 불가
                    - 삭제 방식: Soft Delete
                    """,
            parameters = {
                    @Parameter(name = "gatheringId", description = "모임 식별자", in = ParameterIn.PATH, required = true),
                    @Parameter(name = "meetingId", description = "약속 식별자", in = ParameterIn.PATH, required = true),
                    @Parameter(name = "topicId", description = "주제 식별자", in = ParameterIn.PATH, required = true)
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "주제 삭제 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code":"DELETED","message":"주제가 삭제되었습니다.","data":null}
                                    """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "{\"code\":\"G002\",\"message\":\"입력값이 올바르지 않습니다.\",\"data\":null}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "{\"code\":\"G102\",\"message\":\"인증이 필요합니다.\",\"data\":null}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "주제 삭제 권한 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "{\"code\":\"E105\",\"message\":\"사용자에게 주제 삭제 권한이 없습니다.\",\"data\":null}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "모임, 약속 또는 주제를 찾을 수 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "모임 없음",
                                            description = "모임을 찾을 수 없는 경우",
                                            value = "{\"code\":\"G001\",\"message\":\"모임을 찾을 수 없습니다.\",\"data\":null}"
                                    ),
                                    @ExampleObject(
                                            name = "약속 없음",
                                            description = "약속을 찾을 수 없는 경우",
                                            value = "{\"code\":\"M001\",\"message\":\"약속을 찾을 수 없습니다.\",\"data\":null}"
                                    ),
                                    @ExampleObject(
                                            name = "주제 없음",
                                            description = "주제를 찾을 수 없는 경우",
                                            value = "{\"code\":\"E101\",\"message\":\"주제를 찾을 수 없습니다.\",\"data\":null}"
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 삭제된 주제",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "{\"code\":\"E106\",\"message\":\"이미 삭제된 주제입니다.\",\"data\":null}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "{\"code\":\"E000\",\"message\":\"서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.\",\"data\":null}"
                            )
                    )
            )
    })
    @DeleteMapping(value = "/{topicId}")
    ResponseEntity<ApiResponse<Void>> deleteTopic(
            @PathVariable Long gatheringId,
            @PathVariable Long meetingId,
            @PathVariable Long topicId
    );

    @Operation(
            summary = "주제 좋아요 토글 (developer: 경서영)",
            description = """
                    주제에 대한 좋아요를 추가하거나 취소합니다.
                    - 동작: 좋아요가 없으면 추가, 있으면 취소 (토글)
                    - 권한: 약속의 멤버
                    - 응답: 좋아요 상태(liked)와 변경 후 좋아요 수(newCount) 반환
                    """,
            parameters = {
                    @Parameter(name = "gatheringId", description = "모임 식별자", in = ParameterIn.PATH, required = true),
                    @Parameter(name = "meetingId", description = "약속 식별자", in = ParameterIn.PATH, required = true),
                    @Parameter(name = "topicId", description = "주제 식별자", in = ParameterIn.PATH, required = true)
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "좋아요 토글 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TopicLikeResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "좋아요 추가",
                                            value = """
                                                    {"code":"SUCCESS","message":"주제를 좋아요 했습니다.","data":{"topicId":1,"liked":true,"newCount":5}}
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "좋아요 취소",
                                            value = """
                                                    {"code":"SUCCESS","message":"주제 좋아요를 취소했습니다.","data":{"topicId":1,"liked":false,"newCount":4}}
                                                    """
                                    )
                            })
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "{\"code\":\"G002\",\"message\":\"입력값이 올바르지 않습니다.\",\"data\":null}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "{\"code\":\"G102\",\"message\":\"인증이 필요합니다.\",\"data\":null}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "모임 또는 약속의 멤버가 아님",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "모임 멤버 아님",
                                            description = "모임의 멤버가 아닌 경우",
                                            value = "{\"code\":\"G002\",\"message\":\"모임의 멤버가 아닙니다.\",\"data\":null}"
                                    ),
                                    @ExampleObject(
                                            name = "약속 멤버 아님",
                                            description = "약속의 멤버가 아닌 경우",
                                            value = "{\"code\":\"M004\",\"message\":\"약속의 멤버가 아닙니다.\",\"data\":null}"
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "모임, 약속 또는 주제를 찾을 수 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "모임 없음",
                                            description = "모임을 찾을 수 없는 경우",
                                            value = "{\"code\":\"G001\",\"message\":\"모임을 찾을 수 없습니다.\",\"data\":null}"
                                    ),
                                    @ExampleObject(
                                            name = "약속 없음",
                                            description = "약속을 찾을 수 없는 경우",
                                            value = "{\"code\":\"M001\",\"message\":\"약속을 찾을 수 없습니다.\",\"data\":null}"
                                    ),
                                    @ExampleObject(
                                            name = "주제 없음",
                                            description = "주제를 찾을 수 없는 경우",
                                            value = "{\"code\":\"E101\",\"message\":\"주제를 찾을 수 없습니다.\",\"data\":null}"
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "{\"code\":\"E000\",\"message\":\"서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.\",\"data\":null}"
                            )
                    )
            )
    })
    @PostMapping("/{topicId}/likes")
    ResponseEntity<ApiResponse<TopicLikeResponse>> toggleLike(
            @PathVariable Long gatheringId,
            @PathVariable Long meetingId,
            @PathVariable Long topicId
    );
}
