package com.dokdok.global.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Schema(description = "페이지네이션 응답")
@Builder
public record PageResponse<T>(
        @Schema(description = "아이템 목록")
        List<T> items,

        @Schema(description = "전체 아이템 수", example = "100")
        int totalCount,

        @Schema(description = "현재 페이지 (0부터 시작)", example = "0")
        int currentPage,

        @Schema(description = "페이지 크기", example = "10")
        int pageSize,

        @Schema(description = "전체 페이지 수", example = "10")
        int totalPages
) {
    public static <T> PageResponse<T> of(List<T> items, int totalCount, int currentPage, int pageSize) {
        int totalPages = (int) Math.ceil((double) totalCount / pageSize);
        return new PageResponse<>(items, totalCount, currentPage, pageSize, totalPages);
    }

    public static <T> PageResponse<T> of(List<T> items, long totalCount, int currentPage, int pageSize) {
        return of(items, (int) totalCount, currentPage, pageSize);
    }
}
