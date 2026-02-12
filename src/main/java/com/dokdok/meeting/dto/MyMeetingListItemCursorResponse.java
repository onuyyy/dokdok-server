package com.dokdok.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "메인페이지 내 약속 커서 응답(문서용)")
public record MyMeetingListItemCursorResponse(
        @Schema(description = "아이템 목록")
        List<MyMeetingListItemResponse> items,

        @Schema(description = "전체 아이템 수 (첫 페이지 요청 시에만 제공)", example = "100")
        Integer totalCount,

        @Schema(description = "페이지 크기", example = "4")
        int pageSize,

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext,

        @Schema(description = "다음 커서")
        MeetingListCursor nextCursor
) {
}
