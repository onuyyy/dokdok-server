package com.dokdok.gathering.dto.response;

import com.dokdok.gathering.entity.GatheringMember;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "모임 멤버 관리 커서")
public record GatheringMemberCursor(
        @Schema(description = "모임 멤버 ID", example = "10")
        Long gatheringMemberId
) {
    public static GatheringMemberCursor from(GatheringMember member){
        return new GatheringMemberCursor(member.getId());
    }
}
