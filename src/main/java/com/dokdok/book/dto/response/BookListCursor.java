package com.dokdok.book.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record BookListCursor(
        @Schema(description = "마지막 아이템의 별점 (RATING 정렬 시 사용)", example = "4.5", nullable = true)
        BigDecimal rating,
        @Schema(description = "마지막 아이템의 등록 시간", example = "2026-01-22T10:25:40Z")
        OffsetDateTime addedAt,
        @Schema(description = "마지막 아이템의 bookId", example = "127")
        Long bookId
) {
    public static BookListCursor from(BigDecimal rating, LocalDateTime addedAt, Long bookId) {
        if (addedAt == null || bookId == null) {
            return null;
        }
        return new BookListCursor(rating, OffsetDateTime.of(addedAt, ZoneOffset.UTC), bookId);
    }
}
