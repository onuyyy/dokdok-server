package com.dokdok.book.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@JsonPropertyOrder({"items", "statusCounts", "pageSize", "hasNext", "nextCursor", "totalCount"})
public class PersonalBookCursorPageResponse {

    @Schema(description = "아이템 목록")
    private List<PersonalBookListResponse> items;

    @Schema(description = "상태별 개수")
    private PersonalBookStatusCountsResponse statusCounts;

    @Schema(hidden = true)
    private int pageSize;

    @Schema(hidden = true)
    private boolean hasNext;

    @Schema(hidden = true)
    private BookListCursor nextCursor;

    @Schema(description = "현재 필터 기준 전체 아이템 수")
    private long totalCount;

    public static PersonalBookCursorPageResponse of(
            List<PersonalBookListResponse> items,
            PersonalBookStatusCountsResponse statusCounts,
            int pageSize,
            boolean hasNext,
            BookListCursor nextCursor,
            long totalCount
    ) {
        return new PersonalBookCursorPageResponse(items, statusCounts, pageSize, hasNext, nextCursor, totalCount);
    }
}
