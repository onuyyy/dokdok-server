package com.dokdok.topic.dto.response;

import com.dokdok.topic.entity.Topic;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "확정된 주제 목록 조회를 위한 커서")
public record ConfirmedTopicsCursor(
        @Schema(description = "마지막 항목의 확정 순서 (정렬 기준)", example = "3")
        Integer confirmOrder,

        @Schema(description = "마지막 항목의 주제 ID (동점 대비)", example = "10")
        Long topicId
) {
    public static ConfirmedTopicsCursor from(Topic topic) {
        return ConfirmedTopicsCursor.builder()
                .confirmOrder(topic.getConfirmOrder())
                .topicId(topic.getId())
                .build();
    }
}
