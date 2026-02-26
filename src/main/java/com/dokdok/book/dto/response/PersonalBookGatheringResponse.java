package com.dokdok.book.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "책과 연결된 모임 정보")
public record PersonalBookGatheringResponse(
        @Schema(description = "모임 ID", example = "10")
        Long gatheringId,

        @Schema(description = "모임 이름", example = "예제 모임")
        String gatheringName
) {
}
