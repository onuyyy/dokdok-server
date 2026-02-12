package com.dokdok.topic.api;

import com.dokdok.global.response.ApiResponse;
import com.dokdok.topic.dto.request.TopicAnswerBulkSaveRequest;
import com.dokdok.topic.dto.request.TopicAnswerBulkSubmitRequest;
import com.dokdok.topic.dto.response.PreOpinionSaveResponse;
import com.dokdok.topic.dto.response.PreOpinionSubmitResponse;
import com.dokdok.topic.dto.response.TopicAnswerDetailResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "토픽 답변", description = "토픽 답변 관련 API")
@RequestMapping("/api/gatherings/{gatheringId}/meetings/{meetingId}/answers")
public interface TopicAnswerApi {

    @Operation(
            summary = "토픽 답변 일괄 저장 (developer: 양재웅)",
            description = """
            토픽 답변과 책 평가를 일괄 저장합니다.
            - 권한: 모임 구성원
            - 제약: 제출 완료된 답변은 수정 불가
            """,
            parameters = {
                    @Parameter(name = "gatheringId", description = "모임 식별자", in = ParameterIn.PATH, required = true),
                    @Parameter(name = "meetingId", description = "약속 식별자", in = ParameterIn.PATH, required = true)
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "토픽 답변 일괄 저장 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PreOpinionSaveResponse.class),
                            examples = @ExampleObject(value = """
                                    {"code":"CREATED","message":"사전 의견이 저장되었습니다.","data":{"review":{"reviewId":1,"bookId":10,"userId":1,"rating":4.5,"keywords":[]},"answers":[{"topicId":1,"isSubmitted":false,"updatedAt":"2026-01-19T20:01:37.105545"}]}}
                                    """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "G002", "message": "입력값이 올바르지 않습니다.", "data": null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "모임 멤버가 아님",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "G002", "message": "모임의 멤버가 아닙니다.", "data": null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "토픽 또는 답변을 찾을 수 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "E101", "message": "주제를 찾을 수 없습니다.", "data": null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 답변이 존재함",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "E108", "message": "이미 답변이 존재합니다.", "data": null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "E000", "message": "서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.", "data": null}
                                    """)))
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PreOpinionSaveResponse>> createAnswer(
            @PathVariable("gatheringId") Long gatheringId,
            @PathVariable("meetingId") Long meetingId,
            @Valid @RequestBody TopicAnswerBulkSaveRequest request
    );

    @Operation(
            summary = "사전 의견 작성 화면 조회 (developer: 양재웅)",
            description = """
            현재 로그인 사용자의 사전 의견 작성 화면 정보를 조회합니다.
            - 권한: 모임 구성원
            """,
            parameters = {
                    @Parameter(name = "gatheringId", description = "모임 식별자", in = ParameterIn.PATH, required = true),
                    @Parameter(name = "meetingId", description = "약속 식별자", in = ParameterIn.PATH, required = true)
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "사전 의견 작성 화면 조회 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TopicAnswerDetailResponse.class),
                            examples = @ExampleObject(value = """
                                    {"code":"SUCCESS","message":"사전 의견 작성 화면 조회를 성공했습니다.","data":{"book":{"bookId":10,"title":"아주 작은 습관의 힘","author":"제임스 클리어"},"review":{"reviewId":1,"bookId":10,"userId":1,"rating":4.5,"keywords":[]},"preOpinion":{"updatedAt":"2026-02-06T09:12:30","topics":[{"topicId":1,"topicTitle":"책의 주요 메시지","topicDescription":"이 책에서 전달하고자 하는 핵심 메시지는 무엇인가요?","topicType":"DISCUSSION","topicTypeLabel":"토론형","confirmOrder":1,"content":"이 책은 작은 행동의 반복이 인생을 바꾼다고 생각합니다."},{"topicId":2,"topicTitle":"인상 깊은 구절","topicDescription":"가장 인상 깊었던 문장을 공유해주세요.","topicType":"EMOTION","topicTypeLabel":"감정 공유형","confirmOrder":2,"content":null}]}}}
                                    """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "모임 멤버가 아님",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "G002", "message": "모임의 멤버가 아닙니다.", "data": null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "답변을 찾을 수 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "E103", "message": "답변을 찾을 수 없습니다.", "data": null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "E000", "message": "서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.", "data": null}
                                    """)))
    })
    @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<TopicAnswerDetailResponse>> findMyAnswer(
            @PathVariable("gatheringId") Long gatheringId,
            @PathVariable("meetingId") Long meetingId
    );

    @Operation(
            summary = "토픽 답변 일괄 저장 (developer: 양재웅)",
            description = """
            현재 로그인 사용자의 토픽 답변과 책 평가를 일괄 저장합니다.
            - 권한: 모임 구성원
            - 제약: 제출 완료된 답변은 수정 불가
            """,
            parameters = {
                    @Parameter(name = "gatheringId", description = "모임 식별자", in = ParameterIn.PATH, required = true),
                    @Parameter(name = "meetingId", description = "약속 식별자", in = ParameterIn.PATH, required = true)
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "토픽 답변 일괄 저장 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PreOpinionSaveResponse.class),
                            examples = @ExampleObject(value = """
                                    {"code":"UPDATED","message":"사전 의견이 저장되었습니다.","data":{"review":{"reviewId":1,"bookId":10,"userId":1,"rating":4.5,"keywords":[]},"answers":[{"topicId":1,"isSubmitted":false,"updatedAt":"2026-01-19T20:01:37.105545"}]}}
                                    """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "G002", "message": "입력값이 올바르지 않습니다.", "data": null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "모임 멤버가 아님",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "G002", "message": "모임의 멤버가 아닙니다.", "data": null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "답변을 찾을 수 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "E103", "message": "답변을 찾을 수 없습니다.", "data": null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 제출된 답변",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "E104", "message": "이미 제출된 답변입니다.", "data": null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "E000", "message": "서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.", "data": null}
                                    """)))
    })
    @PatchMapping(value = "/me", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PreOpinionSaveResponse>> updateMyAnswer(
            @PathVariable("gatheringId") Long gatheringId,
            @PathVariable("meetingId") Long meetingId,
            @Valid @RequestBody TopicAnswerBulkSaveRequest request
    );

    @Operation(
            summary = "토픽 답변 일괄 제출 (developer: 양재웅)",
            description = """
            현재 로그인 사용자의 토픽 답변과 책 평가를 일괄 제출합니다.
            - 권한: 모임 구성원
            - 제약: 제출 완료된 답변은 재제출 불가
            """,
            parameters = {
                    @Parameter(name = "gatheringId", description = "모임 식별자", in = ParameterIn.PATH, required = true),
                    @Parameter(name = "meetingId", description = "약속 식별자", in = ParameterIn.PATH, required = true)
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "토픽 답변 일괄 제출 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PreOpinionSubmitResponse.class),
                            examples = @ExampleObject(value = """
                                    {"code":"SUCCESS","message":"사전 의견이 제출되었습니다.","data":{"review":{"reviewId":1,"bookId":10,"userId":1,"rating":4.5,"keywords":[]},"answers":[{"topicId":1,"isSubmitted":true,"submittedAt":"2026-01-19T20:01:37.105545"}]}}
                                    """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "모임 멤버가 아님",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "G002", "message": "모임의 멤버가 아닙니다.", "data": null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "답변을 찾을 수 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "E103", "message": "답변을 찾을 수 없습니다.", "data": null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 제출된 답변",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "E104", "message": "이미 제출된 답변입니다.", "data": null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "E000", "message": "서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.", "data": null}
                                    """)))
    })
    @PatchMapping(value = "/submit", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PreOpinionSubmitResponse>> submitMyAnswer(
            @PathVariable("gatheringId") Long gatheringId,
            @PathVariable("meetingId") Long meetingId,
            @Valid @RequestBody TopicAnswerBulkSubmitRequest request
    );

}
