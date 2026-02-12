package com.dokdok.gathering.dto.response;

import com.dokdok.gathering.entity.GatheringMember;
import com.dokdok.gathering.entity.GatheringRole;
import com.dokdok.gathering.entity.GatheringStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "모임 목록 아이템")
@Builder
public record GatheringListItemResponse(
        @Schema(description = "모임 ID", example = "1")
        Long gatheringId,
        @Schema(description = "모임 이름", example = "독서 모임")
        String gatheringName,
        @Schema(description = "즐겨찾기 여부", example = "true")
        Boolean isFavorite,
        @Schema(description = "모임 상태", example = "ACTIVE")
        GatheringStatus gatheringStatus,
        @Schema(description = "전체 멤버 수", example = "10")
        Integer totalMembers,
        @Schema(description = "전체 약속 수", example = "5")
        Integer totalMeetings,
        @Schema(description = "현재 사용자 역할", example = "LEADER")
        GatheringRole currentUserRole,
        @Schema(description = "가입 후 경과 일수", example = "30")
        Integer daysFromJoined
) {
    public static GatheringListItemResponse from(
            GatheringMember gatheringMember,
            int totalMembers,
            int totalMeetings,
            GatheringRole currentUserRole
    ){
        return GatheringListItemResponse.builder()
                .gatheringId(gatheringMember.getGathering().getId())
                .gatheringName(gatheringMember.getGathering().getGatheringName())
                .isFavorite(gatheringMember.getIsFavorite())
                .gatheringStatus(gatheringMember.getGathering().getGatheringStatus())
                .totalMembers(totalMembers)
                .totalMeetings(totalMeetings)
                .currentUserRole(currentUserRole)
                .daysFromJoined(gatheringMember.getDaysFromJoined())
                .build();
    }
}
