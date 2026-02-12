package com.dokdok.retrospective.dto.response;

import com.dokdok.retrospective.entity.MeetingRetrospective;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "코멘트 커서")
public record CommentCursor(
        @Schema(description = "작성 일시", example = "2026-01-15T15:30:00")
        LocalDateTime createdAt,

        @Schema(description = "코멘트 ID", example = "10")
        Long commentId
) {
    public static CommentCursor from(MeetingRetrospective retrospective) {
        return new CommentCursor(retrospective.getCreatedAt(), retrospective.getId());
    }
}
