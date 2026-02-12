package com.dokdok.ai.dto;

import java.util.List;

public record TopicSummaryRequest(
        Long topicId,
        String topicTitle,
        List<Answer> answers
) {
    public record Answer(
            Long userId,
            String content
    ) {
    }
}
