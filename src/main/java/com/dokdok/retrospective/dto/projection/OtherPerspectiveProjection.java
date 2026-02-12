package com.dokdok.retrospective.dto.projection;

public record OtherPerspectiveProjection(
        Long retrospectiveId,
        Long topicId,
        String topicTitle,
        Integer confirmOrder,
        Long meetingMemberId,
        String memberNickname,
        String opinionContent,
        String impressiveReason
) {
}
