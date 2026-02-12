package com.dokdok.keyword.dto.response;

import com.dokdok.book.entity.KeywordType;
import com.dokdok.keyword.entity.Keyword;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "키워드 목록 응답")
public record KeywordListResponse(
        @Schema(description = "키워드 목록")
        List<KeywordInfo> keywords
) {
    public static KeywordListResponse from(List<Keyword> keywords) {
        return new KeywordListResponse(
                keywords.stream()
                        .map(KeywordInfo::from)
                        .toList()
        );
    }

    @Schema(description = "키워드 정보")
    public record KeywordInfo(
            @Schema(description = "키워드 ID", example = "3")
            Long id,
            @Schema(description = "키워드 이름", example = "판타지")
            String name,
            @Schema(description = "키워드 타입", example = "BOOK")
            KeywordType type,
            @Schema(description = "부모 키워드 ID", example = "1")
            Long parentId,
            @Schema(description = "부모 키워드 이름", example = "기타")
            String parentName,
            @Schema(description = "키워드 레벨", example = "2")
            Integer level,
            @Schema(description = "정렬 순서", example = "3")
            Integer sortOrder,
            @Schema(description = "선택 가능 여부", example = "true")
            Boolean isSelectable
    ) {
        public static KeywordInfo from(Keyword keyword) {
            return new KeywordInfo(
                    keyword.getId(),
                    keyword.getKeywordName(),
                    keyword.getKeywordType(),
                    keyword.getParent() != null ? keyword.getParent().getId() : null,
                    keyword.getParent() != null ? keyword.getParent().getKeywordName() : null,
                    keyword.getLevel(),
                    keyword.getSortOrder(),
                    keyword.getIsSelectable()
            );
        }
    }
}
