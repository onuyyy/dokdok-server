package com.dokdok.gathering.dto.response;

import com.dokdok.gathering.entity.GatheringMember;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "내 모임 목록 커서")
public record MyGatheringCursor(
        @Schema(description = "가입 일시", example = "2025-02-01T10:00:00")
        LocalDateTime joinedAt,
        @Schema(description = "모임 멤버 ID", example = "10")
        Long gatheringMemberId
) {
    public static MyGatheringCursor from(GatheringMember member) {
        return new MyGatheringCursor(member.getJoinedAt(), member.getId());
    }
}
