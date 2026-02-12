package com.dokdok.book.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.util.List;

public record BookReviewRequest(
        @Schema(description = "별점 (0.5 단위, 0.5~5.0)", example = "4.5")
        BigDecimal rating,

        @Schema(description = "키워드 ID 리스트", example = "[3, 7]")
        @NotEmpty(message = "keywordIds는 필수입니다")
        List<Long> keywordIds
) {
}
