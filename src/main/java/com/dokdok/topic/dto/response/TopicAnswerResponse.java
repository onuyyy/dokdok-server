package com.dokdok.topic.dto.response;

import com.dokdok.topic.entity.TopicAnswer;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "토픽 답변 저장/수정 응답")
public record TopicAnswerResponse(
        @Schema(description = "토픽 ID", example = "12")
        Long topicId,
        @Schema(description = "제출 여부", example = "false")
        boolean isSubmitted,
        @Schema(description = "수정 일시", example = "2026-01-19T20:01:37.105545")
        LocalDateTime updatedAt
) {
    public static TopicAnswerResponse from(TopicAnswer answer) {
        return new TopicAnswerResponse(
                answer.getTopic().getId(),
                Boolean.TRUE.equals(answer.getIsSubmitted()),
                answer.getUpdatedAt()
        );
    }
}
