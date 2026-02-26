package com.dokdok.ai.dto;

import java.util.List;

public record SttRequest(
        Long jobId,
        Long meetingId,
        String filePath,
        String language,
        List<PreAnswer> preAnswers
) {
    public record PreAnswer(
            Long topicId,
            String topicTitle,
            Long userId,
            String content
    ) {
    }
}
