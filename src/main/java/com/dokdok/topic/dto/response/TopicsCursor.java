package com.dokdok.topic.dto.response;

import com.dokdok.topic.entity.Topic;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "주제 목록 조회를 위한 커서")
public record TopicsCursor(
        @Schema(description = "마지막 항목의 좋아요 수 (정렬 기준)", example = "5")
        Integer likeCount,

        @Schema(description = "마지막 항목의 주제 ID (동점 대비)", example = "10")
        Long topicId
) {
    public static TopicsCursor from(Topic topic) {
        return TopicsCursor.builder()
                .likeCount(topic.getLikeCount())
                .topicId(topic.getId())
                .build();
    }
}
