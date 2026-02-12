package com.dokdok.book.dto.response;

import com.dokdok.retrospective.dto.response.RetrospectiveRecordResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "독서 타임라인 아이템")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReadingTimelineItem(
        @Schema(description = "타임라인 타입", example = "READING_RECORD")
        ReadingTimelineType type,
        @Schema(description = "정렬 기준 시간", example = "2026-01-05T21:38:00")
        LocalDateTime eventAt,
        @Schema(description = "원본 ID", example = "1")
        Long sourceId,
        @Schema(description = "독서 기록 데이터 (type=READING_RECORD)")
        PersonalReadingRecordListResponse readingRecord,
        @Schema(description = "개인 회고 데이터 (type=PERSONAL_RETROSPECTIVE)")
        RetrospectiveRecordResponse retrospective,
        @Schema(description = "사전 의견 데이터 (type=PRE_OPINION)")
        PersonalReadingTopicAnswerResponse preOpinion
) {
    public static ReadingTimelineItem readingRecord(
            LocalDateTime eventAt,
            Long sourceId,
            PersonalReadingRecordListResponse readingRecord
    ) {
        return new ReadingTimelineItem(
                ReadingTimelineType.READING_RECORD,
                eventAt,
                sourceId,
                readingRecord,
                null,
                null
        );
    }

    public static ReadingTimelineItem retrospective(
            LocalDateTime eventAt,
            Long sourceId,
            RetrospectiveRecordResponse retrospective
    ) {
        return new ReadingTimelineItem(
                ReadingTimelineType.PERSONAL_RETROSPECTIVE,
                eventAt,
                sourceId,
                null,
                retrospective,
                null
        );
    }

    public static ReadingTimelineItem preOpinion(
            LocalDateTime eventAt,
            Long sourceId,
            PersonalReadingTopicAnswerResponse preOpinion
    ) {
        return new ReadingTimelineItem(
                ReadingTimelineType.PRE_OPINION,
                eventAt,
                sourceId,
                null,
                null,
                preOpinion
        );
    }
}
