package com.dokdok.meeting.dto;

import com.dokdok.meeting.entity.Meeting;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Schema(description = "약속 수정 응답")
@Builder
public record MeetingUpdateResponse(
        @Schema(description = "약속 ID", example = "1")
        Long meetingId,

        @Schema(description = "약속 이름", example = "1월 독서 모임 (수정)")
        String meetingName,

        @Schema(description = "시작 일시", example = "2025-02-01T14:00:00")
        LocalDateTime startDate,

        @Schema(description = "종료 일시", example = "2025-02-01T16:00:00")
        LocalDateTime endDate,

        @Schema(description = "장소 정보")
        MeetingLocationDto location,

        @Schema(description = "최대 참가 인원", example = "10")
        Integer maxParticipants
) {
    public static MeetingUpdateResponse from(Meeting meeting) {
        return MeetingUpdateResponse.builder()
                .meetingId(meeting.getId())
                .meetingName(meeting.getMeetingName())
                .startDate(meeting.getMeetingStartDate())
                .endDate(meeting.getMeetingEndDate())
                .location(MeetingLocationDto.from(meeting.getLocation()))
                .maxParticipants(meeting.getMaxParticipants())
                .build();
    }
}
