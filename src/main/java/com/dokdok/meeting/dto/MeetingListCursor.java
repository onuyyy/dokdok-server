package com.dokdok.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "약속 리스트 커서")
public record MeetingListCursor(
        @Schema(description = "마지막 약속 시작 시간 (정렬 기준)", example = "2026-01-22T10:25:40")
        LocalDateTime startDateTime,

        @Schema(description = "마지막 약속 ID (startDateTime이 같은 경우 tie-break)", example = "127")
        Long meetingId
) {
}
