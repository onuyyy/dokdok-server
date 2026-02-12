package com.dokdok.history.dto;

import com.dokdok.history.entity.BookReviewHistory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "책 리뷰 이력 조회를 위한 커서")
public record BookReviewHistoryCursor(
		@Schema(description = "마지막 항목의 이력 ID", example = "10")
		Long historyId
) {
	public static BookReviewHistoryCursor from(BookReviewHistory history) {
		return BookReviewHistoryCursor.builder()
				.historyId(history.getId())
				.build();
	}
}