package com.dokdok.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "약속 승인 리스트 페이지 응답(문서용)")
public record MeetingListItemPageResponse(
        @Schema(description = "아이템 목록")
        List<MeetingListItemResponse> items,

        @Schema(description = "전체 아이템 수", example = "100")
        int totalCount,

        @Schema(description = "현재 페이지 (0부터 시작)", example = "0")
        int currentPage,

        @Schema(description = "페이지 크기", example = "10")
        int pageSize,

        @Schema(description = "전체 페이지 수", example = "10")
        int totalPages
) {
}
