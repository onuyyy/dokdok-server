package com.dokdok.history.dto;

import com.dokdok.book.entity.BookReview;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookReviewSnapshot {

    private Long bookId;
    private BigDecimal rating;
    private List<Long> keywordIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BookReviewSnapshot from(BookReview bookReview) {

        return BookReviewSnapshot.builder()
                .bookId(bookReview.getBook().getId())
                .rating(bookReview.getRating())
                .keywordIds(bookReview.getKeywords().stream()
                        .map(k -> k.getKeyword().getId())
                        .toList()
                )
                .createdAt(bookReview.getCreatedAt())
                .updatedAt(bookReview.getUpdatedAt())
                .build();
    }
}
