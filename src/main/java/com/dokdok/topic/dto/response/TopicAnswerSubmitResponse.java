package com.dokdok.topic.dto.response;

import com.dokdok.topic.entity.TopicAnswer;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "토픽 답변 제출 응답")
public record TopicAnswerSubmitResponse(
        @Schema(description = "토픽 ID", example = "12")
        Long topicId,
        @Schema(description = "제출 여부", example = "true")
        boolean isSubmitted,
        @Schema(description = "제출 일시", example = "2026-01-19T20:01:37.105545")
        LocalDateTime submittedAt
) {
    public static TopicAnswerSubmitResponse from(TopicAnswer answer) {
        return new TopicAnswerSubmitResponse(
                answer.getTopic().getId(),
                Boolean.TRUE.equals(answer.getIsSubmitted()),
                answer.getUpdatedAt()
        );
    }
}
