package com.dokdok.user.dto.response;

import com.dokdok.user.entity.User;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "사용자 정보 응답")
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserInfoResponse(
        @Schema(description = "사용자 ID", example = "1")
        Long userId,

        @Schema(description = "닉네임", example = "독서왕")
        String nickname,

        @Schema(description = "온보딩 필요 여부", example = "true")
        Boolean needsOnboarding
) {
    public static UserInfoResponse from(User user) {
        boolean needsOnboarding = user.getNickname() == null || user.getNickname().isBlank();

        return UserInfoResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .needsOnboarding(needsOnboarding ? true : null)  // 온보딩 필요한 경우만 포함
                .build();
    }
}