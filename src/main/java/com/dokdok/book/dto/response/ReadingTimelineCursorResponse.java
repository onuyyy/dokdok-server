package com.dokdok.book.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "독서 타임라인 커서 응답(문서용)")
public record ReadingTimelineCursorResponse(
        @Schema(description = "아이템 목록")
        List<ReadingTimelineItem> items,

        @Schema(description = "페이지 크기", example = "10")
        int pageSize,

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext,

        @Schema(description = "다음 커서")
        ReadingTimelineCursor nextCursor
) {
}
