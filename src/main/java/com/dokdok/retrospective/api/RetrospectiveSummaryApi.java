package com.dokdok.retrospective.api;

import com.dokdok.global.response.ApiResponse;
import com.dokdok.retrospective.dto.request.RetrospectiveSummaryUpdateRequest;
import com.dokdok.retrospective.dto.response.RetrospectiveSummaryResponse;
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

@Tag(name = "AI 요약", description = "AI 요약 관련 API")
public interface RetrospectiveSummaryApi {

    @Operation(
            summary = "AI 요약 조회 (developer: 오주현)",
            description = """                                                                                                                                                    
                      약속에 대한 AI 요약을 조회합니다.
                      - 권한: 모임장, 약속장, 약속 참여자
                      - 제약: 약속에 참여한 사용자만 조회 가능
                      """,
            parameters = {
                    @Parameter(name = "meetingId", description = "약속 식별자", in = ParameterIn.PATH, required = true)
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "AI 요약 조회 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RetrospectiveSummaryResponse.class),
                            examples = @ExampleObject(value = """                                                                                                                
                                      {
                                        "code": "SUCCESS",
                                        "message": "AI 요약 조회 성공",
                                        "data": {
                                          "meetingId": 1,
                                          "topics": [
                                            {
                                              "topicId": 1,
                                              "confirmOrder": 1,
                                              "topicTitle": "가짜 욕망, 유사 욕망",
                                              "topicDescription": "가짜욕망, 유사욕망에 대해 이야기해봅시다.",
                                              "summary": "참여자들은 『데미안』 속 싱클레어가 느꼈던 혼란을 자신들의 경험과 연결하며...",
                                              "keyPoints": [
                                                {
                                                  "title": "사회가 만든 욕망의 구조",
                                                  "details": [                                                                                                                     
                                                    "안정적인 직업, 성과, 인정 욕구가 개인의 욕망처럼 내면화된 경험 공유",                                                         
                                                    "\\"원해서 선택했다\\"기보다 \\"선택하지 않으면 불안해서 택했다\\"는 표현이 반복됨"                                            
                                                  ]                                                                                                                                
                                                },                                                                                                                                 
                                                {                                                                                                                                  
                                                  "title": "유사 욕망과 진짜 욕망의 차이",                                                                                         
                                                  "details": [                                                                                                                     
                                                    "유사 욕망은 비교와 평가 속에서 강화되며, 타인의 반응에 민감함",                                                               
                                                    "진짜 욕망은 오히려 혼자 있을 때 더 선명해지고, 남에게 말할수록 흐려지는 경우가 많다는 의견"                                   
                                                  ]                                                                                                                                
                                                }                                                                                                                                  
                                              ]
                                            },
                                            {
                                              "topicId": 2,
                                              "confirmOrder": 2,
                                              "topicTitle": "선과 악",
                                              "topicDescription": "인간의 세계에서 선과 악 어느 것이 힘이 더 셀까",
                                              "summary": "선과 악 중 어느 쪽이 더 강한지를 묻기보다...",
                                              "keyPoints": [
                                                {
                                                  "title": "악이 더 강해 보이는 이유",                                                                                             
                                                  "details": [                                                                                                                     
                                                    "결과가 빠르고 명확하게 드러나며, 책임을 외부로 돌리기 쉬움"                                                                   
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
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음",
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
    ResponseEntity<ApiResponse<RetrospectiveSummaryResponse>> getRetrospectiveSummary(
            @PathVariable Long meetingId
    );

    @Operation(
            summary = "AI 요약 수정 (developer: 오주현)",
            description = """                                                                                                                                                    
                      약속에 대한 AI 요약을 수정합니다.
                      - 권한: 모임장, 약속장
                      - 제약: 모임장 또는 약속장만 수정 가능
                      """,
            parameters = {
                    @Parameter(name = "meetingId", description = "약속 식별자", in = ParameterIn.PATH, required = true)
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "AI 요약 수정 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RetrospectiveSummaryResponse.class),
                            examples = @ExampleObject(value = """                                                                                                                
                                      {
                                        "code": "SUCCESS",
                                        "message": "AI 요약 수정 성공",
                                        "data": {
                                          "meetingId": 1,                                                                                                                          
                                          "topics": [                                                                                                                              
                                            {                                                                                                                                      
                                              "topicId": 1,                                                                                                                        
                                              "confirmOrder": 1,                                                                                                                   
                                              "topicTitle": "가짜 욕망, 유사 욕망",                                                                                                
                                              "topicDescription": "가짜욕망, 유사욕망에 대해 이야기해봅시다.",                                                                     
                                              "summary": "수정된 핵심 요약...",                                                                                                    
                                              "keyPoints": [                                                                                                                       
                                                {                                                                                                                                  
                                                  "title": "수정된 포인트 제목",                                                                                                   
                                                  "details": [                                                                                                                     
                                                    "수정된 내용 1",                                                                                                               
                                                    "수정된 내용 2"                                                                                                                
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
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "약속 또는 AI 요약을 찾을 수 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """                                                                                                                
                                      {"code": "R106", "message": "AI 요약을 찾을 수 없습니다.", "data": null}
                                      """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """                                                                                                                
                                      {"code": "E000", "message": "서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.", "data": null}
                                      """)))
    })
    @PatchMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<RetrospectiveSummaryResponse>> updateRetrospectiveSummary(
            @PathVariable Long meetingId,
            @Valid @RequestBody RetrospectiveSummaryUpdateRequest request
    );

    @Operation(
            summary = "약속 회고 생성 (developer: 오주현)",
            description = """
                      약속 회고를 생성(퍼블리시)합니다.
                      - 권한: 모임장, 약속장
                      - 제약: 모임장 또는 약속장만 생성 가능
                      - 생성 후 모든 약속 참여자가 공동 회고를 조회할 수 있습니다.
                      """,
            parameters = {
                    @Parameter(name = "meetingId", description = "약속 식별자", in = ParameterIn.PATH, required = true)
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "약속 회고 생성 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RetrospectiveSummaryResponse.class),
                            examples = @ExampleObject(value = """
                                      {
                                        "code": "CREATED",
                                        "message": "약속 회고 생성 성공",
                                        "data": {
                                          "meetingId": 1,
                                          "isPublished": true,
                                          "publishedAt": "2026-02-21T15:00:00",
                                          "topics": [...]
                                        }
                                      }
                                      """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                      {"code": "R105", "message": "회고에 접근할 권한이 없습니다.", "data": null}
                                      """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 생성됨",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                      {"code": "R108", "message": "이미 약속 회고가 생성되었습니다.", "data": null}
                                      """)))
    })
    @PostMapping(value = "/publish", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<RetrospectiveSummaryResponse>> publishRetrospective(
            @PathVariable Long meetingId
    );
}