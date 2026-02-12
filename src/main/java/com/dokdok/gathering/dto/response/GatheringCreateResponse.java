package com.dokdok.gathering.dto.response;

import com.dokdok.gathering.entity.Gathering;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Schema(description = "모임 생성 응답")
@Builder
public record GatheringCreateResponse(
        @Schema(description = "모임 식별지", example = "1")
        Long gatheringId,
        @Schema(description = "모임 이름", example = "독서 모임")
        String gatheringName,
        @Schema(description = "모임 설명", example = "매주 함께 책을 읽는 모임입니다.")
        @Size(max = 150, message = "모임 설명은 150자 이내로 입력해주세요.")
        String gatheringDescription,
        @Schema(description = "전체 멤버 수", example = "10")
        Integer totalMembers,
        @Schema(description = "모임 생성 후 경과 일수", example = "1")
        Integer daysFromCreation,
        @Schema(description = "전체 약속 수", example = "0")
        Integer totalMeetings,
        @Schema(description = "초대 링크", example = "https://example.com/invite/abc123")
        String invitationLink
) {
    public static GatheringCreateResponse from(Gathering gathering, int activeMembers, int totalMeetings) {
        return GatheringCreateResponse.builder()
                .gatheringId(gathering.getId())
                .gatheringName(gathering.getGatheringName())
                .gatheringDescription(gathering.getDescription())
                .totalMembers(activeMembers)
                .daysFromCreation(gathering.getDaysFromCreation())
                .totalMeetings(totalMeetings)
                .invitationLink(gathering.getInvitationLink())
                .build();
    }
}
