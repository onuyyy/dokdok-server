package com.dokdok.meeting.dto;

import com.dokdok.meeting.entity.MeetingLocation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Builder;

import java.time.LocalDateTime;

@Schema(description = "약속 수정 요청")
@Builder
public record MeetingUpdateRequest(
        @Schema(description = "약속 이름", example = "1월 독서 모임 (수정)")
        String meetingName,

        @Schema(description = "시작 일시", example = "2025-02-01T14:00:00")
        LocalDateTime startDate,

        @Schema(description = "종료 일시", example = "2025-02-01T16:00:00")
        LocalDateTime endDate,

        @Schema(description = "장소 정보")
        MeetingLocationDto location,

        @Schema(description = "최대 참가 인원 (최소 1명)", example = "10", minimum = "1")
        @Min(1)
        Integer maxParticipants
) {
    public MeetingLocation toLocationEntity() {
        if (location == null) {
            return null;
        }
        return location.toEntity();
    }
}
