package com.dokdok.gathering.dto.response;

import com.dokdok.gathering.entity.GatheringMember;
import com.dokdok.gathering.entity.GatheringMemberStatus;
import com.dokdok.gathering.entity.GatheringRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Schema(description = "모임 멤버 관리 정보")
@Builder
public record GatheringMemberResponse(
        @Schema(description = "모임 멤버 ID", example = "10")
        Long gatheringMemberId,

        @Schema(description = "사용자 ID", example = "1")
        Long userId,

        @Schema(description = "닉네임", example = "독서왕")
        String nickname,

        @Schema(description = "프로필 이미지 URL")
        String profileImageUrl,

        @Schema(description = "모임 역할", example = "MEMBER")
        GatheringRole role,

        @Schema(description = "멤버 상태", example = "PENDING")
        GatheringMemberStatus memberStatus,

        @Schema(description = "가입 승인 일시")
        LocalDateTime joinedAt
) {
    public static GatheringMemberResponse from(GatheringMember member, String  presignedProfileImageUrl) {
        return GatheringMemberResponse.builder()
                .gatheringMemberId(member.getId())
                .userId(member.getUser().getId())
                .nickname(member.getUser().getNickname())
                .profileImageUrl(presignedProfileImageUrl)
                .role(member.getRole())
                .memberStatus(member.getMemberStatus())
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
