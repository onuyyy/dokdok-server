package com.dokdok.book.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@JsonPropertyOrder({"items", "pageSize", "hasNext", "nextCursor", "totalCount"})
public class CursorPageResponse<T, C> {

    @Schema(description = "아이템 목록")
    private List<T> items;
    @Schema(hidden = true)
    private int pageSize;
    @Schema(hidden = true)
    private boolean hasNext;
    @Schema(hidden = true)
    private C nextCursor;
    @Schema(description = "전체 아이템 수")
    private long totalCount;

    public static <T, C> CursorPageResponse<T, C> of(
            List<T> items,
            int pageSize,
            boolean hasNext,
            C nextCursor,
            long totalCount
    ) {
        return new CursorPageResponse<>(items, pageSize, hasNext, nextCursor, totalCount);
    }
}
