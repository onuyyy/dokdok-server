package com.dokdok.retrospective.dto.response;

import com.dokdok.topic.entity.Topic;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주제 정보")
public record TopicInfo(
        @Schema(description = "주제 ID", example = "1")
        Long topicId,
        @Schema(description = "주제 제목", example = "깨끗한 코드")
        String topicName,
        @Schema(description = "주제 순서", example = "1")
        Integer confirmOrder
) {
    public static TopicInfo from(Topic topic) {
        return new TopicInfo(
                topic.getId(),
                topic.getTitle(),
                topic.getConfirmOrder()
        );
    }
}
