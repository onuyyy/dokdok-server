package com.dokdok.book.dto.response;

import com.dokdok.book.entity.BookReview;
import com.dokdok.book.entity.KeywordType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Schema(description = "책 리뷰 응답")
public record BookReviewResponse(
        @Schema(description = "리뷰 ID", example = "1")
        Long reviewId,
        @Schema(description = "책 ID", example = "10")
        Long bookId,
        @Schema(description = "작성자 사용자 ID", example = "5")
        Long userId,
        @Schema(description = "평점", example = "4.5")
        BigDecimal rating,
        @Schema(description = "선택한 키워드 목록")
        List<KeywordInfo> keywords
) {
    public static BookReviewResponse from(BookReview review) {
        List<KeywordInfo> keywordInfos = review.getKeywords().stream()
                .map(reviewKeyword -> new KeywordInfo(
                        reviewKeyword.getKeyword().getId(),
                        reviewKeyword.getKeyword().getKeywordName(),
                        reviewKeyword.getKeyword().getKeywordType()
                ))
                .collect(Collectors.toList());

        return new BookReviewResponse(
                review.getId(),
                review.getBook().getId(),
                review.getUser().getId(),
                review.getRating(),
                keywordInfos
        );
    }

    @Schema(description = "리뷰 키워드 정보")
    public record KeywordInfo(
            @Schema(description = "키워드 ID", example = "3")
            Long id,
            @Schema(description = "키워드 이름", example = "판타지")
            String name,
            @Schema(description = "키워드 타입", example = "BOOK")
            KeywordType type
    ) {
    }
}
