package com.dokdok.gathering.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "책별 평균 평점 정보")
public record BookRatingAverage(
        @Schema(description = "책 ID", example = "1")
        Long bookId,

        @Schema(description = "평균 평점", example = "4.25")
        Double averageRating
) {}