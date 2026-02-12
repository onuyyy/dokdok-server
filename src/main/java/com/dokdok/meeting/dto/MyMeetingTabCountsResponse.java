package com.dokdok.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "메인페이지 내 약속 탭별 카운트 응답")
@Builder
public record MyMeetingTabCountsResponse(
        @Schema(description = "전체 약속 수", example = "10")
        int all,

        @Schema(description = "다가오는 약속 수 (3일 이내)", example = "2")
        int upcoming,

        @Schema(description = "완료된 약속 수", example = "5")
        int done
) {
}
