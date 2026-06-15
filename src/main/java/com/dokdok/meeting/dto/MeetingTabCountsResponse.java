package com.dokdok.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "약속 탭별 카운트 응답")
@Builder
public record MeetingTabCountsResponse(
        @Schema(description = "전체 약속 수 (확정 + 완료)", example = "10")
        int all,

        @Schema(description = "예정된 약속 수 (확정된 약속 전체)", example = "2")
        int upcoming,

        @Schema(description = "완료된 약속 수", example = "5")
        int done,

        @Schema(description = "내가 참여한 약속 수 (전체 상태)", example = "3")
        int joined
) {
}
