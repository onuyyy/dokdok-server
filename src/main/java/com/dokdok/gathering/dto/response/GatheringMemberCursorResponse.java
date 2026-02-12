package com.dokdok.gathering.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "모임 멤버 목록 커서 응답(문서용)")
public record GatheringMemberCursorResponse(
        @Schema(description = "아이템 목록")
        List<GatheringMemberResponse> items,

        @Schema(description = "페이지 크기", example = "10")
        int pageSize,

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext,

        @Schema(description = "다음 커서")
        GatheringMemberCursor nextCursor,

        @Schema(description = "전체 아이템 수 (첫 페이지 요청 시에만 제공)", example = "100")
        Integer totalCount
) {
}