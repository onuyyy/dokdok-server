package com.dokdok.retrospective.api;

import com.dokdok.global.response.ApiResponse;
import com.dokdok.retrospective.dto.request.PersonalRetrospectiveRequest;
import com.dokdok.retrospective.dto.response.PersonalRetrospectiveDetailResponse;
import com.dokdok.retrospective.dto.response.PersonalRetrospectiveEditResponse;
import com.dokdok.retrospective.dto.response.PersonalRetrospectiveFormResponse;
import com.dokdok.retrospective.dto.response.PersonalRetrospectiveResponse;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "개인 회고", description = "개인 회고 관련 API")
public interface PersonalRetrospectiveApi {

    @Operation(
            summary = "개인 회고 작성 (developer: 경서영)",
            description = """
            약속에 대한 개인 회고를 작성합니다.
            - 입력: 생각의 변화(changedThoughts), 인상 깊은 의견(othersPerspectives), 자유 기록(freeTexts)
            - 권한: 약속 멤버
            - 제약: 약속 1건당 1회 작성 (이미 작성한 경우 409 에러)
            """,
            parameters = {
                    @Parameter(name = "meetingId", description = "약속 식별자", in = ParameterIn.PATH, required = true)
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "개인 회고 저장 완료",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PersonalRetrospectiveResponse.class),
                            examples = @ExampleObject(value = """
                                    {"code": "CREATED", "message": "개인 회고 저장 완료했습니다.", "data": {"personalMeetingRetrospectiveId": 1, "meetingId": 1, "userId": 1}}
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {"code": "INVALID_INPUT_VALUE", "message": "입력값이 올바르지 않습니다.", "data": null}
                                            """
                            )
                    )
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
                    responseCode = "403",
                    description = "약속 멤버가 아님",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {"code": "M004", "message": "약속의 멤버가 아닙니다.", "data": null}
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "약속을 찾을 수 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {"code": "M001", "message": "약속을 찾을 수 없습니다.", "data": null}
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 해당 약속에 대한 회고가 존재함",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {"code": "R101", "message": "이미 해당 약속에 대한 회고가 존재합니다.", "data": null}
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
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PersonalRetrospectiveResponse>> writePersonalRetrospective(
            @PathVariable Long meetingId,
            @Valid @RequestBody PersonalRetrospectiveRequest request
    );

    @Operation(
            summary = "개인 회고 입력 폼 조회 (developer: 경서영)",
            description = """
            개인 회고 작성에 필요한 폼 데이터를 조회합니다.
            - 권한: 약속 멤버
            - 제약: 이미 회고를 작성한 경우 409 에러

            **응답 구조**
            - topics: 확정된 주제 목록 (confirmOrder 순)
            - topicAnswers: 본인의 사전 의견 목록
            - meetingMembers: 본인 제외 약속 멤버 목록
            """,
            parameters = {
                    @Parameter(name = "meetingId", description = "약속 식별자", in = ParameterIn.PATH, required = true)
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "개인 회고 입력 폼 조회 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PersonalRetrospectiveFormResponse.class),
                            examples = @ExampleObject(value = """
                                    {"code": "SUCCESS", "message": "개인 회고 입력 폼 조회를 성공했습니다.", "data": {"meetingId": 1, "preOpinions": [{"topicId": 1, "topicName": "깨끗한 코드", "content": "사전 의견 내용을 작성합니다."}], "topics": [{"topicId": 1, "topicName": "깨끗한 코드", "confirmOrder": 1}], "meetingMembers": [{"meetingMemberId": 10, "nickname": "독서왕", "profileImage": "https://example.com/profile.jpg"}]}}
                                    """)
                    )
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
                    responseCode = "403",
                    description = "약속 멤버가 아님",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {"code": "M004", "message": "약속의 멤버가 아닙니다.", "data": null}
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "약속을 찾을 수 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {"code": "M001", "message": "약속을 찾을 수 없습니다.", "data": null}
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 해당 약속에 대한 회고가 존재함",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {"code": "R101", "message": "이미 해당 약속에 대한 회고가 존재합니다.", "data": null}
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
    @GetMapping(value = "/form", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PersonalRetrospectiveFormResponse>> getPersonalRetrospectiveForm(
            @PathVariable Long meetingId
    );

    @Operation(
            summary = "개인 회고 수정 폼 조회 (developer: 경서영)",
            description = """
            기존에 작성한 개인 회고를 수정하기 위한 데이터를 조회합니다.
            - 권한: 약속 멤버

            **응답 구조**
            - changedThoughts: 기존 생각의 변화 목록 (confirmOrder 순)
            - othersPerspectives: 기존 인상 깊은 의견 목록 (배열 순서 = 표시 순서)
            - freeTexts: 기존 자유 기록 목록
            - topics: 확정된 주제 목록 (confirmOrder 순)
            - meetingMembers: 본인 제외 약속 멤버 목록
            """,
            parameters = {
                    @Parameter(name = "meetingId", description = "약속 식별자", in = ParameterIn.PATH, required = true),
                    @Parameter(name = "retrospectiveId", description = "개인 회고 식별자", in = ParameterIn.PATH, required = true)
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "개인 회고 수정 폼 조회 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PersonalRetrospectiveEditResponse.class),
                            examples = @ExampleObject(value = """
                                    {"code": "SUCCESS", "message": "개인 회고 수정 폼 조회를 성공했습니다.", "data": {"retrospectiveId": 1, "retrospective": {"changedThoughts": [{"topicId": 1, "keyIssue": "요약된 핵심 쟁점", "preOpinion": "토론 전 나의 생각", "postOpinion": "토론 후 바뀐 생각"}], "othersPerspectives": [{"topicId": 1, "meetingMemberId": 10, "opinionContent": "상대 의견이 인상적이었습니다.", "impressiveReason": "새로운 관점을 제공했기 때문입니다."}], "freeTexts": [{"title": "오늘의 한 줄", "content": "회고 내용을 작성합니다."}]}, "topics": [{"topicId": 1, "topicName": "깨끗한 코드", "confirmOrder": 1}], "meetingMembers": [{"meetingMemberId": 10, "nickname": "독서왕", "profileImage": "https://example.com/profile.jpg"}]}}
                                    """)
                    )
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
                    responseCode = "403",
                    description = "약속 멤버가 아님",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {"code": "M004", "message": "약속의 멤버가 아닙니다.", "data": null}
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "약속 또는 개인 회고를 찾을 수 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "약속 없음",
                                            description = "약속을 찾을 수 없는 경우",
                                            value = """
                                                    {"code": "M001", "message": "약속을 찾을 수 없습니다.", "data": null}
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "회고 없음",
                                            description = "개인 회고를 찾을 수 없는 경우",
                                            value = """
                                                    {"code": "R102", "message": "회고를 찾을 수 없습니다.", "data": null}
                                                    """
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
                                    value = """
                                            {"code": "E000", "message": "서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.", "data": null}
                                            """
                            )
                    )
            )
    })
    @GetMapping(value = "/{retrospectiveId}/form", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PersonalRetrospectiveEditResponse>> getPersonalRetrospectiveEditForm(
            @PathVariable Long meetingId,
            @PathVariable Long retrospectiveId
    );

    @Operation(
            summary = "개인 회고 상세 조회 (developer: 경서영)",
            description = """
            특정 개인 회고의 상세 정보를 조회합니다.
            - 권한: 약속 멤버

            **응답 구조**
            - changedThoughts: 생각의 변화 목록 (confirmOrder 순, 주제별 사전/사후 의견)
            - othersPerspectives: 인상 깊은 의견 목록 (confirmOrder 순 -> 같은 주제가 있을 경우 작성된 순서, 멤버 프로필 이미지 포함)
            - freeTexts: 자유 기록 목록
            """,
            parameters = {
                    @Parameter(name = "meetingId", description = "약속 식별자", in = ParameterIn.PATH, required = true),
                    @Parameter(name = "retrospectiveId", description = "개인 회고 식별자", in = ParameterIn.PATH, required = true)
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "개인 회고 상세 조회 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PersonalRetrospectiveDetailResponse.class),
                            examples = @ExampleObject(value = """
                                    {"code": "SUCCESS", "message": "개인 회고 조회를 성공했습니다.", "data": {"retrospectiveId": 1, "retrospective": {"changedThoughts": [{"topicId": 1, "topicTitle": "깨끗한 코드", "keyIssue": "요약된 핵심 쟁점", "preOpinion": "토론 전 나의 생각", "postOpinion": "토론 후 바뀐 생각"}], "othersPerspectives": [{"topicId": 1, "meetingMemberId": 10, "profileImage": "https://example.com/profile.jpg", "nickname": "독서왕", "opinionContent": "상대 의견이 인상적이었습니다.", "impressiveReason": "새로운 관점을 제공했기 때문입니다."}], "freeTexts": [{"title": "오늘의 한 줄", "content": "회고 내용을 작성합니다."}]}}}
                                    """)
                    )
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
                    responseCode = "403",
                    description = "약속 멤버가 아님",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {"code": "M004", "message": "약속의 멤버가 아닙니다.", "data": null}
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "약속 또는 개인 회고를 찾을 수 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "약속 없음",
                                            description = "약속을 찾을 수 없는 경우",
                                            value = """
                                                    {"code": "M001", "message": "약속을 찾을 수 없습니다.", "data": null}
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "회고 없음",
                                            description = "개인 회고를 찾을 수 없는 경우",
                                            value = """
                                                    {"code": "R102", "message": "회고를 찾을 수 없습니다.", "data": null}
                                                    """
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
                                    value = """
                                            {"code": "E000", "message": "서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.", "data": null}
                                            """
                            )
                    )
            )
    })
    @GetMapping(value = "/{retrospectiveId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PersonalRetrospectiveDetailResponse>> getPersonalRetrospective(
            @PathVariable Long meetingId,
            @PathVariable Long retrospectiveId
    );

    @Operation(
            summary = "개인 회고 수정 (developer: 경서영)",
            description = """
            기존에 작성한 개인 회고를 수정합니다.
            - 입력: 생각의 변화(changedThoughts), 인상 깊은 의견(othersPerspectives), 자유 기록(freeTexts)
            - 권한: 작성자 본인
            - 동작: 기존 데이터 삭제 후 새로운 데이터로 전체 교체
            """,
            parameters = {
                    @Parameter(name = "meetingId", description = "약속 식별자", in = ParameterIn.PATH, required = true),
                    @Parameter(name = "retrospectiveId", description = "개인 회고 식별자", in = ParameterIn.PATH, required = true)
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "개인 회고 수정 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PersonalRetrospectiveResponse.class),
                            examples = @ExampleObject(value = """
                                    {"code": "SUCCESS", "message": "개인 회고 수정을 성공했습니다.", "data": {"personalMeetingRetrospectiveId": 1, "meetingId": 1, "userId": 1}}
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {"code": "INVALID_INPUT_VALUE", "message": "입력값이 올바르지 않습니다.", "data": null}
                                            """
                            )
                    )
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
                    responseCode = "403",
                    description = "약속 멤버가 아님",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {"code": "M004", "message": "약속의 멤버가 아닙니다.", "data": null}
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "약속 또는 개인 회고를 찾을 수 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "약속 없음",
                                            description = "약속을 찾을 수 없는 경우",
                                            value = """
                                                    {"code": "M001", "message": "약속을 찾을 수 없습니다.", "data": null}
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "회고 없음",
                                            description = "개인 회고를 찾을 수 없는 경우",
                                            value = """
                                                    {"code": "R102", "message": "회고를 찾을 수 없습니다.", "data": null}
                                                    """
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
                                    value = """
                                            {"code": "E000", "message": "서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.", "data": null}
                                            """
                            )
                    )
            )
    })
    @PutMapping(value = "/{retrospectiveId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PersonalRetrospectiveResponse>> editPersonalRetrospective(
            @PathVariable Long meetingId,
            @PathVariable Long retrospectiveId,
            @Valid @RequestBody PersonalRetrospectiveRequest request
    );

    @Operation(
            summary = "개인 회고 삭제 (developer: 경서영)",
            description = """
            기존에 작성한 개인 회고를 삭제합니다.
            - 권한: 작성자 본인
            - 삭제 방식: Soft Delete
            - 제약: 이미 삭제된 회고는 다시 삭제 불가
            """,
            parameters = {
                    @Parameter(name = "meetingId", description = "약속 식별자", in = ParameterIn.PATH, required = true),
                    @Parameter(name = "retrospectiveId", description = "개인 회고 식별자", in = ParameterIn.PATH, required = true)
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "개인 회고 삭제 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "{\"code\":\"DELETED\",\"message\":\"개인 회고 삭제를 성공했습니다.\",\"data\":null}"
                            )
                    )
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
                    responseCode = "403",
                    description = "본인이 작성한 회고가 아님",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {"code": "G101", "message": "접근 권한이 없습니다.", "data": null}
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "약속 또는 개인 회고를 찾을 수 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "약속 없음",
                                            description = "약속을 찾을 수 없는 경우",
                                            value = """
                                                    {"code": "M001", "message": "약속을 찾을 수 없습니다.", "data": null}
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "회고 없음",
                                            description = "개인 회고를 찾을 수 없는 경우",
                                            value = """
                                                    {"code": "R102", "message": "회고를 찾을 수 없습니다.", "data": null}
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "이미 삭제됨",
                                            description = "이미 삭제된 개인 회고인 경우",
                                            value = """
                                                    {"code": "R104", "message": "이미 삭제된 개인 회고입니다.", "data": null}
                                                    """
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
                                    value = """
                                            {"code": "E000", "message": "서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.", "data": null}
                                            """
                            )
                    )
            )
    })
    @DeleteMapping(value = "/{retrospectiveId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<Void>> deletePersonalRetrospective(
            @PathVariable Long meetingId,
            @PathVariable Long retrospectiveId
    );
}
