package com.dokdok.stt.api;

import com.dokdok.global.response.ApiResponse;
import com.dokdok.stt.dto.SttJobResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "STT", description = "STT 작업 관련 API")
@RequestMapping("/api/gatherings/{gatheringId}/meetings/{meetingId}/stt/jobs")
public interface SttApi {

    @Operation(
            summary = "STT 작업 생성",
            description = "오디오 파일 업로드 또는 사전의견만으로 STT 요약 작업을 생성합니다.",
            parameters = {
                    @Parameter(name = "gatheringId", description = "모임 식별자", in = ParameterIn.PATH, required = true),
                    @Parameter(name = "meetingId", description = "약속 식별자", in = ParameterIn.PATH, required = true)
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "STT 작업 생성 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SttJobResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = """
                                    {"code":"E000","message":"잘못된 요청입니다.","data":null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "약속 멤버가 아님",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = """
                                    {"code":"G002","message":"모임의 멤버가 아닙니다.","data":null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "약속을 찾을 수 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = """
                                    {"code":"M001","message":"약속을 찾을 수 없습니다.","data":null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = """
                                    {"code":"E000","message":"서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.","data":null}
                                    """)))
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<SttJobResponse>> createJob(
            @PathVariable Long gatheringId,
            @PathVariable Long meetingId,
            @RequestPart(value = "file", required = false) MultipartFile file
    );

    @Operation(
            summary = "STT 작업 조회",
            description = "STT 작업 상태/결과를 조회합니다.",
            parameters = {
                    @Parameter(name = "gatheringId", description = "모임 식별자", in = ParameterIn.PATH, required = true),
                    @Parameter(name = "meetingId", description = "약속 식별자", in = ParameterIn.PATH, required = true),
                    @Parameter(name = "jobId", description = "STT 작업 식별자", in = ParameterIn.PATH, required = true)
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "STT 작업 조회 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SttJobResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = """
                                    {"code":"E000","message":"잘못된 요청입니다.","data":null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "약속 멤버가 아님",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = """
                                    {"code":"G002","message":"모임의 멤버가 아닙니다.","data":null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "작업을 찾을 수 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = """
                                    {"code":"E101","message":"작업을 찾을 수 없습니다.","data":null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = """
                                    {"code":"E000","message":"서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.","data":null}
                                    """)))
    })
    @GetMapping(value = "/{jobId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<SttJobResponse>> getJob(
            @PathVariable Long gatheringId,
            @PathVariable Long meetingId,
            @PathVariable Long jobId
    );
}
