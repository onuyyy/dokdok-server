package com.dokdok.topic.dto.response;

import com.dokdok.topic.entity.TopicStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "주제 확정 응답")
public record ConfirmTopicsResponse(
        @Schema(description = "약속 ID", example = "1")
        Long meetingId,

        @Schema(description = "주제 상태 (PROPOSED: 제안됨, CONFIRMED: 확정됨)", example = "CONFIRMED",
                allowableValues = {"PROPOSED", "CONFIRMED"})
        TopicStatus topicStatus,

        @Schema(description = "확정된 주제 목록")
        List<ConfirmedTopicOrder> topics
) {
    @Schema(description = "확정된 주제 정보")
    public record ConfirmedTopicOrder(
            @Schema(description = "주제 ID", example = "10")
            Long topicId,
            @Schema(description = "확정 순서", example = "1")
            Integer confirmOrder
    ) {
        public static ConfirmedTopicOrder of(Long topicId, Integer confirmOrder) {
            return new ConfirmedTopicOrder(topicId, confirmOrder);
        }
    }

    public static ConfirmTopicsResponse from(
            Long meetingId,
            List<ConfirmedTopicOrder> topics
    ) {
        return new ConfirmTopicsResponse(meetingId, TopicStatus.CONFIRMED, topics);
    }
}
