package com.dokdok.retrospective.dto.response;

import com.dokdok.global.response.CursorResponse;
import com.dokdok.retrospective.entity.PersonalMeetingRetrospective;

import java.util.List;

/**
 * 개인 회고 목록 페이지네이션 응답을 위한 헬퍼 레코드
 */
public record RetrospectiveRecordsPageResponse() {

    public static CursorResponse<RetrospectiveRecordResponse, RetrospectiveRecordsCursor> from(
            List<RetrospectiveRecordResponse> items,
            int pageSize,
            boolean hasNext,
            PersonalMeetingRetrospective lastRetrospective,
            Integer totalCount
    ) {
        RetrospectiveRecordsCursor cursor = null;
        if (hasNext && lastRetrospective != null) {
            cursor = RetrospectiveRecordsCursor.from(lastRetrospective);
        }

        return CursorResponse.of(items, pageSize, hasNext, cursor, totalCount);
    }
}
