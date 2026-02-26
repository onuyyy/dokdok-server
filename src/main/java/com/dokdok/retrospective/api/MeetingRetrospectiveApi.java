package com.dokdok.retrospective.api;

import com.dokdok.global.response.ApiResponse;
import com.dokdok.global.response.CursorResponse;
import com.dokdok.retrospective.dto.request.MeetingRetrospectiveRequest;
import com.dokdok.retrospective.dto.response.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Tag(name = "공동 회고", description = "공동 회고 관련 API")
@RequestMapping("/api/meetings/{meetingId}/retrospectives")
public interface MeetingRetrospectiveApi {

    @Operation(
            summary = "공동 회고 조회 (developer: 오주현)",


            description = """                                                                                                                                                    
                  공동 회고 정보를 조회합니다.
                - 토픽 목록, 요약, 주요 포인트를 반환합니다.
                - 코멘트는 별도 API로 조회합니다.
                """,
            parameters = {
                    @Parameter(name = "meetingId", description = "약속 식별자", in = ParameterIn.PATH, required = true)
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "공동 회고 조회 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MeetingRetrospectiveResponse.class),
                            examples = @ExampleObject(value = """                                                                                                                
                                      {
                                        "code": "SUCCESS",
                                        "message": "공동 회고 조회 성공",
                                        "data": {
                                          "meetingId": 1,
                                          "meetingName": "데미안을 읽어보아요",
                                          "meetingDate": "2026-01-15",
                                          "meetingTime": "19:00-20:00",
                                          "meetingLeaderId": 123,
                                          "gathering": {
                                            "gatheringId": 1,
                                            "gatheringName": "독서 모임"
                                          },
                                          "topics": [
                                            {
                                              "topicId": 1,
                                              "confirmOrder": 1,
                                              "topicTitle": "가짜 욕망, 유사 욕망",
                                              "topicDescription": "가짜욕망, 유사욕망에 대해 이야기해봅시다.",
                                              "summary": "참여자들은 데미안 속 싱클레어가 느꼈던 혼란을 자신들의 경험과 연결하며...",
                                              "keyPoints": [
                                                {
                                                  "title": "사회가 만든 욕망의 구조",
                                                  "details": [
                                                    "안정적인 직업, 성과, 인정 욕구가 개인의 욕망처럼 내면화된 경험 공유",
                                                    "원해서 선택했다기보다 선택하지 않으면 불안해서 택했다는 표현이 반복됨"
                                                  ]
                                                }
                                              ]
                                            }
                                          ]
                                        }
                                      }
                                      """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 - 로그인이 필요합니다.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """                                                                                                                
                                      {"code": "G102", "message": "인증이 필요합니다.", "data": null}
                                      """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 - 약속 참여자만 조회할 수 있습니다.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "R105", "message": "회고에 접근할 권한이 없습니다.", "data": null}
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
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<MeetingRetrospectiveResponse>> getMeetingRetrospective(
            @Parameter(description = "약속 ID", required = true, example = "1")
            @PathVariable Long meetingId
    );

    @Operation(
            summary = "코멘트 조회 (developer: 오주현)",
            description = """                                                                                                                                                    
                  약속 회고의 코멘트를 조회합니다.
                  - 커서 기반 무한스크롤을 지원합니다.
                  - 첫 페이지: cursorCreatedAt, cursorCommentId 없이 호출
                  - 다음 페이지: 응답의 nextCursor 값을 파라미터로 전달
                  - 정렬: createdAt DESC, id DESC
                  """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TopicCommentCursorResponse.class),
                            examples = @ExampleObject(value = """                                                                                                                
                                      {
                                        "code": "SUCCESS",
                                        "message": "코멘트 조회 성공",
                                        "data": {
                                          "items": [
                                            {
                                              "commentId": 1,
                                              "userId": 1,
                                              "nickname": "사용자1",
                                              "profileImageUrl": "https://example.com/profile.jpg",
                                              "comment": "모임 하기전엔 이랬는데, 누구의 이런 말을 듣고 이렇게 생각이 바뀌었다.",
                                              "createdAt": "2026-01-15T15:30:00"
                                            }
                                          ],
                                          "pageSize": 10,
                                          "hasNext": true,
                                          "nextCursor": {
                                            "createdAt": "2026-01-15T15:00:00",
                                            "commentId": 5
                                          },
                                          "totalCount": 15
                                        }
                                      }
                                      """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 - 로그인이 필요합니다.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """                                                                                                                
                                      {"code": "G102", "message": "인증이 필요합니다.", "data": null}
                                      """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 - 약속 참여자만 조회할 수 있습니다.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """                                                                                                                
                                      {"code": "R105", "message": "회고 접근 권한이 없습니다.", "data": null}
                                      """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "약속을 찾을 수 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            value = """                                                                                                                          
                                                      {"code": "M001", "message": "약속을 찾을 수 없습니다.", "data": null}
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
                            examples = @ExampleObject(value = """                                                                                                                
                                      {"code": "E000", "message": "서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.", "data": null}
                                      """)
                    )
            )
    })
    @GetMapping("/comments")
    ResponseEntity<ApiResponse<CursorResponse<MeetingRetrospectiveResponse.CommentResponse, CommentCursor>>> getTopicComments(
            @Parameter(description = "약속 ID", required = true, example = "1")
            @PathVariable Long meetingId,

            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") int pageSize,

            @Parameter(description = "커서 - 마지막 코멘트의 작성일시 (ISO 8601)", example = "2026-01-15T15:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cursorCreatedAt,

            @Parameter(description = "커서 - 마지막 코멘트의 ID", example = "5")
            @RequestParam(required = false) Long cursorCommentId
    );

    @Operation(
            summary = "공동 회고 작성 (developer: 오주현)",
            description = """
            약속에 대한 공동 회고를 작성합니다.
            - 제약: 약속에 참여한 사용자만 작성 가능
            """,
            parameters = {
                    @Parameter(name = "meetingId", description = "약속 식별자", in = ParameterIn.PATH, required = true)
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "공동 회고 작성 완료",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MeetingRetrospectiveResponse.CommentResponse.class),
                            examples = @ExampleObject(value = """                                                                                                                
                                      {
                                        "code": "CREATED",
                                        "message": "공동 회고 작성 완료",
                                        "data": {
                                          "commentId": 1,
                                          "userId": 1,
                                          "nickname": "독서왕",
                                          "profileImageUrl": "https://example.com/profile.jpg",
                                          "comment": "좋았습니다.",
                                          "createdAt": "2025-02-01T16:30:00"
                                        }
                                      }
                                      """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "G002", "message": "입력값이 올바르지 않습니다.", "data": null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "R105", "message": "회고에 접근할 권한이 없습니다.", "data": null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "약속 또는 토픽을 찾을 수 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "M001", "message": "약속을 찾을 수 없습니다.", "data": null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 삭제된 주제",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "E106", "message": "이미 삭제된 주제입니다.", "data": null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "E000", "message": "서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.", "data": null}
                                    """)))
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<MeetingRetrospectiveResponse.CommentResponse>> createMeetingRetrospective(
            @PathVariable Long meetingId,
            @Valid @RequestBody MeetingRetrospectiveRequest request
    );

    @Operation(
            summary = "공동 회고 코멘트 삭제 (developer: 오주현)",
            description = """
            약속에 대한 공동 회고 코멘트를 삭제합니다.
            - 권한: 작성자 또는 약속장
            """,
            parameters = {
                    @Parameter(name = "meetingId", description = "약속 식별자", in = ParameterIn.PATH, required = true),
                    @Parameter(name = "commentId", description = "코멘트 식별자", in = ParameterIn.PATH, required = true)
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "공동 회고 코멘트 삭제 완료",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                                    {"code": "DELETED", "message": "공동 회고 코멘트 삭제 완료", "data": null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "GA003", "message": "리더만 가능한 작업입니다.", "data": null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "공동 회고를 찾을 수 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "R103", "message": "공동 회고 내용을 찾을 수 없습니다.", "data": null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "E000", "message": "서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.", "data": null}
                                    """)))
    })
    @DeleteMapping(path = "/{commentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<Void>> deleteMeetingRetrospective(
            @PathVariable Long meetingId,
            @PathVariable Long commentId
    );

    @Operation(
            summary = "수집된 사전 의견 조회 (developer: 오주현)",
            description = """                                                                                                                                                        
                  약속 회고 생성 화면에서 멤버별로 그룹화된 사전 의견을 조회합니다.
                  - 권한: 약속장만 조회 가능
                  - 커서 기반 무한스크롤을 지원합니다.
                  - 첫 페이지: cursorUserId 없이 호출
                  - 다음 페이지: 응답의 nextCursor.userId 값을 cursorUserId로 전달
                  - 정렬: userId ASC
                  """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "수집된 사전 의견 조회 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CollectedAnswersCursorResponse.class),
                            examples = @ExampleObject(value = """                                                                                                                    
                                  {
                                    "code": "SUCCESS",
                                    "message": "수집된 사전 의견 조회 성공",
                                    "data": {
                                      "items": [
                                        {
                                          "userId": 1,
                                          "nickname": "곰곰",
                                          "profileImageUrl": "https://example.com/profile.jpg",
                                          "topics": [
                                            {
                                              "topicId": 1,
                                              "title": "가짜욕망, 유사 욕망",
                                              "confirmOrder": 1,
                                              "answerId": 101,
                                              "content": "어쩌구 저쩌구..."
                                            },
                                            {
                                              "topicId": 2,
                                              "title": "진정한 자아 찾기",
                                              "confirmOrder": 2,
                                              "answerId": 102,
                                              "content": "저쩌구 어쩌구..."
                                            }
                                          ]
                                        },
                                        {
                                          "userId": 2,
                                          "nickname": "독서왕",
                                          "profileImageUrl": "https://example.com/profile2.jpg",
                                          "topics": [
                                            {
                                              "topicId": 1,
                                              "title": "가짜욕망, 유사 욕망",
                                              "confirmOrder": 1,
                                              "answerId": 103,
                                              "content": "내 생각은..."
                                            }
                                          ]
                                        }
                                      ],
                                      "pageSize": 10,
                                      "hasNext": true,
                                      "nextCursor": {
                                        "userId": 2
                                      },
                                      "totalCount": 8
                                    }
                                  }
                                  """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 - 로그인이 필요합니다.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """                                                                                                                    
                                  {"code": "G102", "message": "인증이 필요합니다.", "data": null}
                                  """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 - 약속장만 조회할 수 있습니다.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """                                                                                                                    
                                  {"code": "M003", "message": "약속장만 가능한 작업입니다.", "data": null}
                                  """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "약속을 찾을 수 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """                                                                                                                    
                                  {"code": "M001", "message": "약속을 찾을 수 없습니다.", "data": null}
                                  """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """                                                                                                                    
                                  {"code": "E000", "message": "서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.", "data": null}
                                  """)
                    )
            )
    })
    @GetMapping("/collected-answers")
    ResponseEntity<ApiResponse<CursorResponse<MemberAnswerResponse, CollectedAnswersCursor>>> getCollectedAnswers(
            @Parameter(description = "약속 ID", required = true, example = "1")
            @PathVariable Long meetingId,

            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") int pageSize,

            @Parameter(description = "커서 - 마지막 멤버의 userId", example = "2")
            @RequestParam(required = false) Long cursorUserId
    );
}
