package com.dokdok.user.controller;

import com.dokdok.global.response.ApiResponse;
import com.dokdok.user.api.DevAuthApi;
import com.dokdok.user.dto.request.DevLoginRequest;
import com.dokdok.user.service.DevAuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Profile({"dev", "local"})
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class DevAuthController implements DevAuthApi {

	private final DevAuthService devAuthService;

	@Override
	@PostMapping("/login")
	public ResponseEntity<ApiResponse<Void>> devLogin(DevLoginRequest request, HttpServletRequest httpRequest) {

		devAuthService.login(request, httpRequest);
		return ApiResponse.success("로그인 성공");
	}
}