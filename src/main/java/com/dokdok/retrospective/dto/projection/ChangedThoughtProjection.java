package com.dokdok.retrospective.dto.projection;

public record ChangedThoughtProjection(
        Long retrospectiveId,
        Long topicId,
        String topicTitle,
        Integer confirmOrder,
        String keyIssue,
        String postOpinion
) {
}
