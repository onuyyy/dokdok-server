package com.dokdok.book.repository.dto;

import java.time.LocalDateTime;

public record ReadingTimelineIndexRow(
        LocalDateTime eventAt,
        String type,
        Long sourceId,
        Integer typeOrder
) {
}
