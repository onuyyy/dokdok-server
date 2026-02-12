package com.dokdok.gathering.dto.response;

import com.dokdok.gathering.entity.Gathering;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Schema(description = "모임 수정 응답")
@Builder
public record GatheringUpdateResponse(
        @Schema(description = "모임 ID", example = "1")
        Long gatheringId,
        @Schema(description = "모임 이름", example = "독서 모임 (수정)")
        String gatheringName,
        @Schema(description = "모임 설명", example = "매주 함께 책을 읽는 모임입니다.")
        String description,
        @Schema(description = "수정 일시", example = "2025-02-01T12:00:00")
        LocalDateTime updatedAt
) {
    public static GatheringUpdateResponse from(Gathering gathering){
        return GatheringUpdateResponse.builder()
                .gatheringId(gathering.getId())
                .gatheringName(gathering.getGatheringName())
                .description(gathering.getDescription())
                .updatedAt(gathering.getUpdatedAt())
                .build();
    }
}
