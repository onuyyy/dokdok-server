package com.dokdok.user.service;

import com.dokdok.global.util.SecurityUtil;
import com.dokdok.storage.service.StorageService;
import com.dokdok.user.dto.request.OnboardRequest;
import com.dokdok.user.dto.request.UpdateUserInfoRequest;
import com.dokdok.user.dto.response.UserDetailResponse;
import com.dokdok.user.entity.User;
import com.dokdok.user.exception.UserErrorCode;
import com.dokdok.user.exception.UserException;
import com.dokdok.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final StorageService storageService;

    @Transactional
    public void onboard(OnboardRequest request, MultipartFile file) {

        Long currentUserId = SecurityUtil.getCurrentUserId();
        User user = getUserById(currentUserId);

        if (request.nickname() == null) {
            throw new UserException(UserErrorCode.NICKNAME_EMPTY);
        }

        String trimmedNickname = request.nickname().trim();
        validateNickname(trimmedNickname);
        user.updateNickname(trimmedNickname);

        if (file != null && !file.isEmpty()) {
            String imageUrl = storageService.uploadProfileImage(file);
            user.updateProfileImage(imageUrl);
        }

        SecurityUtil.updateCurrentUserInContext(user);
    }

    /**
     * 닉네임 변경 전 중복 체크
     * @param nickname 변경할 닉네임
     */
    @Transactional(readOnly = true)
    public void checkNickname(String nickname) {
        if (nickname == null) {
            throw new UserException(UserErrorCode.NICKNAME_EMPTY);
        }
        validateNickname(nickname.trim());
    }

    /**
     * SecurityUtil에서 현재 세션에 저장된 사용자의 User Entity를 응답 Response Dto형식으로 반환.
     */
    public UserDetailResponse getCurrentUserInfo() {

        User currentUser = SecurityUtil.getCurrentUserEntity();
        String presignedProfileImage = storageService.getPresignedProfileImage(currentUser.getProfileImageUrl());

        return UserDetailResponse.from(currentUser, presignedProfileImage);
    }

    /**
     * 사용자의 정보를 변경합니다.
     */
    @Transactional
    public UserDetailResponse updateUserInfo(UpdateUserInfoRequest request) {

        validateNickname(request.nickname());

        Long currentUserId = SecurityUtil.getCurrentUserId();

        User user = getUserById(currentUserId);
        user.updateInfo(request);

        SecurityUtil.updateCurrentUserInContext(user);
        return UserDetailResponse.from(user);
    }

    @Transactional
    public void deleteCurrentUser() {
        Long currentUserId = SecurityUtil.getCurrentUserId();

        User user = getUserById(currentUserId);
        user.delete();
    }

    @Transactional
    public UserDetailResponse updateProfileImage(MultipartFile file) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        User user = getUserById(currentUserId);

        String oldImageUrl = user.getProfileImageUrl();
        if (oldImageUrl != null && !oldImageUrl.isBlank()) {
            storageService.deleteProfileImage(oldImageUrl);
        }

        String newImageUrl = storageService.uploadProfileImage(file);
        user.updateProfileImage(newImageUrl);

        SecurityUtil.updateCurrentUserInContext(user);

        String presignedUrl = storageService.getPresignedProfileImage(newImageUrl);
        return UserDetailResponse.from(user, presignedUrl);
    }

    @Transactional
    public void deleteProfileImage() {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        User user = getUserById(currentUserId);

        String imageUrl = user.getProfileImageUrl();
        if (imageUrl != null && !imageUrl.isBlank()) {
            storageService.deleteProfileImage(imageUrl);
            user.updateProfileImage(null);
            SecurityUtil.updateCurrentUserInContext(user);
        }
    }

    /**
     * 닉네임 유효성을 검사합니다.
     * @param nickname trim된 닉네임
     */
    private void validateNickname(String nickname) {
        if(nickname == null || nickname.trim().isEmpty()) {
            throw new UserException(UserErrorCode.NICKNAME_EMPTY);
        }

        if(nickname.length() < 2 || nickname.length() > 20) {
            throw new UserException(UserErrorCode.NICKNAME_LENGTH_INVALID);
        }

        if(!nickname.matches("^[가-힣a-zA-Z0-9]+$")) {
            throw new UserException(UserErrorCode.NICKNAME_FORMAT_INVALID);
        }

        Optional<User> checkNick = userRepository.findByNickname(nickname);
        if(checkNick.isPresent()) {
            throw new UserException(UserErrorCode.NICKNAME_ALREADY_EXISTS);
        }
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
    }
}
