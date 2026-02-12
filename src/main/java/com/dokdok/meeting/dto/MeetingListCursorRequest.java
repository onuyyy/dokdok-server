package com.dokdok.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Schema(description = "약속 리스트 커서 요청")
public record MeetingListCursorRequest(
        @Schema(description = "이전 페이지의 마지막 약속 시작 시간")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime startDateTime,

        @Schema(description = "이전 페이지의 마지막 약속 ID")
        Long meetingId
) {
    public MeetingListCursor toCursorOrNull() {
        if (startDateTime == null || meetingId == null) {
            return null;
        }
        return new MeetingListCursor(startDateTime, meetingId);
    }
}
