package com.dokdok.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Schema(description = "커서 기반 무한스크롤 응답")
@Builder
public record CursorResponse<T, C>(
        @Schema(description = "아이템 목록")
        List<T> items,

        @Schema(description = "페이지 크기", example = "10")
        int pageSize,

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext,

        @Schema(description = "다음 페이지 커서 (hasNext가 false면 null)")
        C nextCursor,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(description = "전체 아이템 수 (첫 페이지 요청 시에만 제공)", example = "100")
        Integer totalCount
) {
    public static <T, C> CursorResponse<T, C> of(List<T> items, int pageSize, boolean hasNext, C nextCursor) {
        return new CursorResponse<>(items, pageSize, hasNext, hasNext ? nextCursor : null, null);
    }

    public static <T, C> CursorResponse<T, C> of(List<T> items, int pageSize, C nextCursor) {
        boolean hasNext = nextCursor != null;
        return new CursorResponse<>(items, pageSize, hasNext, nextCursor, null);
    }

    public static <T, C> CursorResponse<T, C> of(
            List<T> items,
            int pageSize,
            boolean hasNext,
            C nextCursor,
            Integer totalCount
    ) {
        return new CursorResponse<>(items, pageSize, hasNext, hasNext ? nextCursor : null, totalCount);
    }
}
