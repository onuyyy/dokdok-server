package com.dokdok.book.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "독서 타임라인 커서")
public record ReadingTimelineCursor(
        @Schema(description = "마지막 이벤트 시간", example = "2026-01-05T21:38:00")
        LocalDateTime eventAt,
        @Schema(description = "마지막 이벤트 타입", example = "READING_RECORD")
        ReadingTimelineType type,
        @Schema(description = "마지막 이벤트 원본 ID", example = "10")
        Long sourceId
) {
    public static ReadingTimelineCursor from(LocalDateTime eventAt, ReadingTimelineType type, Long sourceId) {
        return new ReadingTimelineCursor(eventAt, type, sourceId);
    }
}
