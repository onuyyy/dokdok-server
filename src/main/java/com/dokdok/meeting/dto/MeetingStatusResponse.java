package com.dokdok.meeting.dto;

import com.dokdok.meeting.entity.Meeting;
import com.dokdok.meeting.entity.MeetingStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "약속 상태 변경 응답")
public record MeetingStatusResponse(
        @Schema(description = "약속 ID", example = "1")
        Long meetingId,

        @Schema(description = "약속 상태", example = "CONFIRMED")
        MeetingStatus meetingStatus,

        @Schema(description = "확정 일시 (확정 시에만 값 존재)", example = "2025-01-25T10:30:00")
        LocalDateTime confirmedAt
) {
    public static MeetingStatusResponse from(Meeting meeting) {
        if (meeting == null) {
            return null;
        }
        LocalDateTime confirmedAt = meeting.getMeetingStatus() == MeetingStatus.CONFIRMED
                ? LocalDateTime.now()
                : null;
        return new MeetingStatusResponse(
                meeting.getId(),
                meeting.getMeetingStatus(),
                confirmedAt
        );
    }
}
