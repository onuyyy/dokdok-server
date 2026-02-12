package com.dokdok.user.api;

import com.dokdok.global.response.ApiResponse;
import com.dokdok.user.dto.response.UserInfoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "인증", description = "인증 관련 API")
@RequestMapping("/api/auth")
public interface AuthApi {

    @Operation(
            summary = "현재 로그인 사용자 정보 조회 (developer: 조건희)",
            description = "현재 로그인한 사용자의 세션 정보를 조회합니다. 온보딩 필요 여부도 함께 반환됩니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그인 사용자 정보 조회 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserInfoResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "온보딩 완료된 사용자",
                                            description = "닉네임이 설정된 사용자",
                                            value = "{\"code\":\"SUCCESS\",\"message\":\"로그인 사용자 정보 조회 성공\",\"data\":{\"userId\":1,\"nickname\":\"테스트닉네임\",\"profileImageUrl\":\"https://example.com/profile.jpg\",\"needsOnboarding\":false}}"
                                    ),
                                    @ExampleObject(
                                            name = "온보딩 필요한 사용자",
                                            description = "닉네임이 설정되지 않은 신규 사용자",
                                            value = "{\"code\":\"SUCCESS\",\"message\":\"로그인 사용자 정보 조회 성공\",\"data\":{\"userId\":1,\"nickname\":\"null\",\"profileImageUrl\":\"null\",\"needsOnboarding\":true}}"
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
                                    value = "{\"code\":\"G001\",\"message\":\"인증되지 않은 사용자입니다.\",\"data\":null}"
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
    @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<UserInfoResponse>> getCurrentUser();

    @Operation(
            summary = "로그아웃 (developer: 조건희)",
            description = "현재 세션을 무효화하고 보안 컨텍스트를 초기화합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그아웃 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    value = "{\"code\":\"SUCCESS\",\"message\":\"로그아웃 성공\",\"data\":null}"
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
    @PostMapping(value = "/logout", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<Void>> logout();
}
