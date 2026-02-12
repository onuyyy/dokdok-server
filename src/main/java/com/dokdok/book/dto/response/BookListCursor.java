package com.dokdok.book.dto.response;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record BookListCursor(
        OffsetDateTime addedAt,
        Long bookId
) {
    public static BookListCursor from(LocalDateTime addedAt, Long bookId) {
        if (addedAt == null || bookId == null) {
            return null;
        }
        return new BookListCursor(OffsetDateTime.of(addedAt, ZoneOffset.UTC), bookId);
    }
}
