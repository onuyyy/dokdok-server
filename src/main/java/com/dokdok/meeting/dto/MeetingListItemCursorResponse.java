package com.dokdok.meeting.dto;

import com.dokdok.global.response.CursorResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "약속 리스트 커서 응답(문서용)")
public record MeetingListItemCursorResponse(
        @Schema(description = "아이템 목록")
        List<MeetingListItemResponse> items,

        @Schema(description = "전체 아이템 수 (첫 페이지 요청 시에만 제공)", example = "100")
        Integer totalCount,

        @Schema(description = "페이지 크기", example = "4")
        int pageSize,

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext,

        @Schema(description = "다음 커서")
        MeetingListCursor nextCursor
) {
    public static MeetingListItemCursorResponse from(CursorResponse<MeetingListItemResponse, MeetingListCursor> response) {
        return new MeetingListItemCursorResponse(
                response.items(),
                response.totalCount(),
                response.pageSize(),
                response.hasNext(),
                response.nextCursor()
        );
    }
}
