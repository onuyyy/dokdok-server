package com.dokdok.user.api;

import com.dokdok.global.response.ApiResponse;
import com.dokdok.user.dto.request.DevLoginRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "[Dev] 인증", description = "개발/로컬 환경 전용 테스트 로그인 API (prod에서는 비활성화)")
@RequestMapping("/api/auth")
public interface DevAuthApi {

	@Operation(
			summary = "테스트 로그인 (dev/local 전용)",
			description = "이메일과 고정 비밀번호(dev1234)로 세션을 생성합니다. dev/local 프로필에서만 사용 가능합니다."
	)
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "200",
					description = "로그인 성공",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							examples = @ExampleObject(
									value = "{\"code\":\"SUCCESS\",\"message\":\"로그인 성공\",\"data\":null}"
							)
					)
			),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "400",
					description = "비밀번호 불일치 또는 존재하지 않는 이메일",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							examples = {
									@ExampleObject(
											name = "비밀번호 불일치",
											value = "{\"code\":\"INVALID_PASSWORD\",\"message\":\"비밀번호가 일치하지 않습니다.\",\"data\":null}"
									),
									@ExampleObject(
											name = "이메일 없음",
											value = "{\"code\":\"USER_NOT_FOUND\",\"message\":\"존재하지 않는 이메일입니다.\",\"data\":null}"
									)
							}
					)
			)
	})
	@PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<ApiResponse<Void>> devLogin(@RequestBody DevLoginRequest request, HttpServletRequest httpRequest);
}
