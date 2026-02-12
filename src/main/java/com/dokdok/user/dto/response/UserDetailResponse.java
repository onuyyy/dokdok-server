package com.dokdok.user.dto.response;

import com.dokdok.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Schema(description = "사용자 상세 정보 응답")
@Builder
public record UserDetailResponse(
        @Schema(description = "사용자 ID", example = "1")
        Long userId,

        @Schema(description = "닉네임", example = "독서왕")
        String nickname,

        @Schema(description = "이메일", example = "user@example.com")
        String email,

        @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
        String profileImageUrl,

        @Schema(description = "가입 일시", example = "2025-01-01T10:00:00")
        LocalDateTime createdAt
)
{
    public static UserDetailResponse from(User user) {
        return UserDetailResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .email(user.getUserEmail())
                .profileImageUrl(user.getProfileImageUrl())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public static UserDetailResponse from(User user, String presignedProfileImage) {
        return UserDetailResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .email(user.getUserEmail())
                .profileImageUrl(presignedProfileImage)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
