package com.dokdok.retrospective.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "약속 멤버 정보")
public record MemberInfo(
        @Schema(description = "약속 멤버 ID", example = "10")
        Long meetingMemberId,
        @Schema(description = "닉네임", example = "독서왕")
        String nickname,
        @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
        String profileImage
) {
    public static MemberInfo of(Long meetingMemberId, String nickname, String profileImage) {
        return new MemberInfo(
                meetingMemberId,
                nickname,
                profileImage
        );
    }
}
