package com.dokdok.topic.api;

import com.dokdok.global.response.ApiResponse;
import com.dokdok.topic.dto.response.PreOpinionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "사전 의견", description = "약속의 사전 의견 관련 API")
@RequestMapping("/api/gatherings/{gatheringId}/meetings/{meetingId}/answers")
public interface PreOpinionApi {

    @Operation(
            summary = "사전 의견 목록 조회 (developer: 경서영)",
            description = """
                    약속에 참여한 멤버들의 사전 의견(독서 리뷰, 주제 답변)을 조회합니다.
                    - 권한: 약속의 멤버
                    - 제약: 본인이 사전 의견을 작성한 경우에만 조회 가능
                    - 응답: 확정된 주제 목록, 멤버별 사전 의견 (책 평가, 주제 답변)
                    
                    **응답 구조**
                    - topics: 확정된 주제 목록 (confirmOrder 순)
                    - members: 멤버별 사전 의견 (프로필, 책 평가, 주제별 답변)
                    """,
            parameters = {
                    @Parameter(name = "gatheringId", description = "모임 식별자", in = ParameterIn.PATH, required = true),
                    @Parameter(name = "meetingId", description = "약속 식별자", in = ParameterIn.PATH, required = true)
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "사전 의견 목록 조회 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PreOpinionResponse.class),
                            examples = @ExampleObject(value = """
                                    {"code":"SUCCESS","message":"약속의 사전 의견 목록 조회를 성공했습니다.","data":{"topics":[{"topicId":1,"title":"책의 주요 메시지","description":"이 책에서 전달하고자 하는 핵심 메시지는 무엇인가요?","topicType":"DISCUSSION","topicTypeLabel":"토론형","confirmOrder":1}],"members":[{"memberInfo":{"userId":1,"nickname":"독서왕","profileImage":"https://example.com/profile.jpg","role":"GATHERING_LEADER"},"bookReview":{"rating":4.5,"keywordInfo":[{"id":1,"name":"성장","type":"BOOK"},{"id":2,"name":"여운이 남는","type":"IMPRESSION"}]},"topicOpinions":[{"topicId":1,"content":"저는 이 책의 핵심 메시지가 자기 성찰이라고 생각합니다."}],"isSubmitted":true}]}}
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
                    description = "접근 권한 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "약속 멤버 아님",
                                            description = "약속의 멤버가 아닌 경우",
                                            value = "{\"code\":\"M004\",\"message\":\"약속의 멤버가 아닙니다.\",\"data\":null}"
                                    ),
                                    @ExampleObject(
                                            name = "사전 의견 미작성",
                                            description = "본인이 사전 의견을 작성하지 않은 경우",
                                            value = "{\"code\":\"B010\",\"message\":\"평가를 작성한 사용자만 조회할 수 있습니다.\",\"data\":null}"
                                    )
                            }
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
                                    ),
                                    @ExampleObject(
                                            name = "약속이 모임에 속하지 않음",
                                            description = "약속이 해당 모임에 속하지 않는 경우",
                                            value = "{\"code\":\"M003\",\"message\":\"해당 약속은 해당 모임에 속해있지 않습니다.\",\"data\":null}"
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
                                    value = "{\"code\":\"E000\",\"message\":\"서버 내부 오류가 발생했습니다.\",\"data\":null}"
                            )
                    )
            )
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PreOpinionResponse>> findAnswers(
            @PathVariable("gatheringId") Long gatheringId,
            @PathVariable("meetingId") Long meetingId
    );

    @Operation(
            summary = "내 사전의견 삭제 (developer: 경서영)",
            description = """
                    현재 로그인 사용자의 약속에 대한 사전의견을 삭제합니다.
                    - 권한: 약속의 멤버
                    """,
            parameters = {
                    @Parameter(name = "gatheringId", description = "모임 식별자", in = ParameterIn.PATH, required = true),
                    @Parameter(name = "meetingId", description = "약속 식별자", in = ParameterIn.PATH, required = true)
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "내 사전의견 삭제 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code":"DELETED","message":"내 사전의견이 삭제되었습니다.","data":null}
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
    @DeleteMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<Void>> deleteMyAnswer(
            @PathVariable("gatheringId") Long gatheringId,
            @PathVariable("meetingId") Long meetingId
    );
}
