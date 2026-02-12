package com.dokdok.history.dto;

import com.dokdok.book.entity.KeywordType;
import com.dokdok.history.entity.BookReviewHistory;
import com.dokdok.keyword.entity.Keyword;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Schema(description = "책 리뷰 변경 이력 응답")
public record BookReviewHistoryResponse(
		@Schema(description = "리뷰 이력 ID", example = "1")
		Long bookReviewHistoryId,

		@Schema(description = "작성 일시", example = "2025-12-08T14:30:00")
		LocalDateTime createdAt,

		@Schema(description = "별점", example = "4.0")
		BigDecimal rating,

		@Schema(description = "책 키워드 목록")
		List<KeywordInfo> bookKeywords,

		@Schema(description = "감상 키워드 목록")
		List<KeywordInfo> impressionKeywords
) {

	public static BookReviewHistoryResponse from(BookReviewHistory history, Map<Long, Keyword> keywordMap) {
		BookReviewSnapshot snapshot = history.getSnapshot();

		Map<KeywordType, List<KeywordInfo>> groupedKeywords = snapshot.getKeywordIds().stream()
				.filter(keywordMap::containsKey)
				.map(keywordMap::get)
				.map(keyword -> new KeywordInfo(
						keyword.getId(),
						keyword.getKeywordName(),
						keyword.getKeywordType()
				))
				.collect(Collectors.groupingBy(KeywordInfo::type));

		return new BookReviewHistoryResponse(
				history.getId(),
				snapshot.getUpdatedAt(),
				snapshot.getRating(),
				groupedKeywords.getOrDefault(KeywordType.BOOK, List.of()),
				groupedKeywords.getOrDefault(KeywordType.IMPRESSION, List.of())
		);
	}

	@Schema(description = "키워드 정보")
	public record KeywordInfo(
			@Schema(description = "키워드 ID", example = "3")
			Long id,

			@Schema(description = "키워드 이름", example = "성장")
			String name,

			@Schema(description = "키워드 타입", example = "BOOK")
			KeywordType type
	) {
	}
}
