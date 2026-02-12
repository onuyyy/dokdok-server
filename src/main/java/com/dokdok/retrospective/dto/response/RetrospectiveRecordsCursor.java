package com.dokdok.retrospective.dto.response;

import com.dokdok.retrospective.entity.PersonalMeetingRetrospective;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@Schema(description = "개인 회고 목록 조회를 위한 커서")
public record RetrospectiveRecordsCursor(
        @Schema(description = "마지막 항목의 생성 시간 (정렬 기준)", example = "2025-02-01T16:30:00")
        LocalDateTime createdAt,

        @Schema(description = "마지막 항목의 회고 ID (동점 대비)", example = "10")
        Long retrospectiveId
) {
    public static RetrospectiveRecordsCursor from(PersonalMeetingRetrospective retrospective) {
        return RetrospectiveRecordsCursor.builder()
                .createdAt(retrospective.getCreatedAt())
                .retrospectiveId(retrospective.getId())
                .build();
    }
}
