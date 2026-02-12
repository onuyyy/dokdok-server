package com.dokdok.gathering.dto.response;

import com.dokdok.gathering.entity.GatheringMember;
import com.dokdok.gathering.entity.GatheringMemberStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "모임 가입 응답")
@Builder
public record GatheringJoinResponse(
        @Schema(description = "모임 ID", example = "1")
        Long gatheringId,
        @Schema(description = "모임 이름", example = "독서 모임")
        String gatheringName,
        @Schema(description = "가입 상태", example = "PENDING")
        GatheringMemberStatus memberStatus
) {
    public static GatheringJoinResponse from(GatheringMember member) {
        return GatheringJoinResponse.builder()
                .gatheringId(member.getGathering().getId())
                .gatheringName(member.getGathering().getGatheringName())
                .memberStatus(member.getMemberStatus())
                .build();
    }
}
