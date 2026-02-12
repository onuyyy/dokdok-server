package com.dokdok.topic.dto.response;

import com.dokdok.topic.entity.Topic;
import com.dokdok.topic.entity.TopicType;
import com.dokdok.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "주제 제안 응답")
public record SuggestTopicResponse(
        @Schema(description = "주제 ID", example = "1")
        Long topicId,

        @Schema(description = "약속 ID", example = "1")
        Long meetingId,

        @Schema(description = "주제 제목", example = "책의 주요 메시지")
        String title,

        @Schema(description = "주제 설명", example = "이 책에서 전달하고자 하는 핵심 메시지는 무엇인가요?")
        String description,

        @Schema(description = "주제 타입", example = "DISCUSSION",
                allowableValues = {"FREE", "DISCUSSION", "EMOTION", "EXPERIENCE", "CHARACTER_ANALYSIS", "COMPARISON", "STRUCTURE", "IN_DEPTH", "CREATIVE", "CUSTOM"})
        TopicType topicType,

        @Schema(description = "주제 타입 라벨", example = "토론형")
        String topicTypeLabel,

        @Schema(description = "주제 타입 설명", example = "자유롭게 의견을 나누는 토론 주제입니다")
        String topicTypeDescription,

        @Schema(description = "작성자 정보")
        CreatedByInfo createdBy
) {

    public static SuggestTopicResponse from(Topic topic, User user) {
        return SuggestTopicResponse.builder()
                .topicId(topic.getId())
                .meetingId(topic.getMeeting().getId())
                .title(topic.getTitle())
                .description(topic.getDescription())
                .topicType(topic.getTopicType())
                .topicTypeLabel(topic.getTopicType().getDisplayName())
                .topicTypeDescription(topic.getTopicType().getDescription())
                .createdBy(CreatedByInfo.of(
                        user.getId(),
                        user.getNickname()
                ))
                .build();
    }
}