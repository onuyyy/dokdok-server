package com.dokdok.book.dto.response;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record ReadingRecordCursor(
        OffsetDateTime createdAt,
        Long recordId
) {
    public static ReadingRecordCursor from(LocalDateTime createdAt, Long recordId) {
        if (createdAt == null || recordId == null) {
            return null;
        }
        return new ReadingRecordCursor(OffsetDateTime.of(createdAt, ZoneOffset.UTC), recordId);
    }
}
