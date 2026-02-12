package com.dokdok.topic.dto.response;

import com.dokdok.topic.entity.TopicMessage;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주제 좋아요 응답")
public record TopicLikeResponse(
        @Schema(description = "주제 ID", example = "1")
        Long topicId,

        @Schema(description = "좋아요 상태 (true: 좋아요 성공, false: 좋아요 취소)", example = "true")
        Boolean liked,

        @Schema(description = "변경 후 좋아요 수", example = "10")
        int newCount
) {

    public static TopicLikeResponse from(Long topicId, TopicMessage result, int newCount) {
        return new TopicLikeResponse(
                topicId,
                result == TopicMessage.LIKE_SUCCESS,
                newCount
        );
    }

}
