package com.dokdok.user.controller;

import com.dokdok.global.response.ApiResponse;
import com.dokdok.user.api.UserApi;
import com.dokdok.user.dto.request.OnboardRequest;
import com.dokdok.user.dto.request.UpdateUserInfoRequest;
import com.dokdok.user.dto.response.UserDetailResponse;
import com.dokdok.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final UserService userService;

    @Override
    @PatchMapping(value = "/onboarding", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> onboard(
            @Valid @RequestPart("request") OnboardRequest request,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage) {
        userService.onboard(request, profileImage);
        return ApiResponse.success("온보딩 완료");
    }

    @Override
    @GetMapping("/check-nickname")
    public ResponseEntity<ApiResponse<Void>> checkNickname(String nickname) {
        userService.checkNickname(nickname);
        return ApiResponse.success("사용 가능한 닉네임입니다.");
    }

    @Override
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDetailResponse>> getCurrentUser() {
        UserDetailResponse currentUserInfo = userService.getCurrentUserInfo();
        return ApiResponse.success(currentUserInfo, "프로필 조회 성공");
    }

    @Override
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserDetailResponse>> updateUserInfo(@Valid @RequestBody UpdateUserInfoRequest request) {
        UserDetailResponse response = userService.updateUserInfo(request);
        return ApiResponse.success(response, "프로필 수정 성공");
    }

    @Override
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteCurrentUser(HttpServletRequest request) {
        userService.deleteCurrentUser();
        ResponseEntity<ApiResponse<Void>> response = ApiResponse.deleted("회원 탈퇴가 완료되었습니다.");

        invalidateSession(request);
        SecurityContextHolder.clearContext();

        return response;
    }

    private void invalidateSession(HttpServletRequest request) {
        if (request == null) {
            return;
        }

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    @Override
    @PatchMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserDetailResponse>> updateProfileImage(
            @RequestPart(value = "profileImage") MultipartFile profileImage) {
        UserDetailResponse response = userService.updateProfileImage(profileImage);
        return ApiResponse.success(response, "프로필 이미지가 변경되었습니다.");
    }

    @Override
    @DeleteMapping("/me/profile-image")
    public ResponseEntity<ApiResponse<Void>> deleteProfileImage() {
        userService.deleteProfileImage();
        return ApiResponse.success("프로필 이미지가 삭제되었습니다.");
    }
}
