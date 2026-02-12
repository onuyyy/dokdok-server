package com.dokdok.topic.dto.response;

import com.dokdok.global.response.CursorResponse;
import com.dokdok.topic.entity.Topic;
import com.dokdok.topic.entity.TopicType;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@JsonPropertyOrder({"items", "pageSize", "hasNext", "nextCursor", "totalCount", "actions"})
@Schema(description = "확정된 주제 목록 응답")
public record ConfirmedTopicsResponse(
        @JsonUnwrapped
        CursorResponse<ConfirmedTopicDto, ConfirmedTopicsCursor> page,
        @Schema(description = "사전 의견 관련 권한 정보")
        Actions actions
) {
    public record Actions(
            @Schema(description = "사전 의견 확인 가능 여부", example = "true")
            Boolean canViewPreOpinions,
            @Schema(description = "사전 의견 작성 가능 여부", example = "false")
            Boolean canWritePreOpinions
    ) {
        public static Actions of(Boolean canViewPreOpinions, Boolean canWritePreOpinions) {
            return new Actions(canViewPreOpinions, canWritePreOpinions);
        }
    }

    @Builder
    @Schema(description = "확정된 주제 정보")
    public record ConfirmedTopicDto(
            @Schema(description = "주제 ID", example = "10")
            Long topicId,
            @Schema(description = "주제 제목", example = "데미안에서 '자기 자신'이란?")
            String title,
            @Schema(description = "주제 설명", example = "주제에 대한 간단한 설명입니다.")
            String description,
            @Schema(description = "주제 타입", example = "DISCUSSION")
            TopicType topicType,
            @Schema(description = "주제 타입 라벨", example = "토론형")
            String topicTypeLabel,
            @Schema(description = "좋아요 수", example = "5")
            Integer likeCount,
            @Schema(description = "확정 순서", example = "1")
            Integer confirmOrder,
            @Schema(description = "주제 제안자 정보")
            CreatedByInfo createdByInfo
    ) {
        public static ConfirmedTopicDto from(Topic topic) {
            return ConfirmedTopicDto.builder()
                    .topicId(topic.getId())
                    .title(topic.getTitle())
                    .description(topic.getDescription())
                    .topicType(topic.getTopicType())
                    .topicTypeLabel(topic.getTopicType().getDisplayName())
                    .likeCount(topic.getLikeCount())
                    .confirmOrder(topic.getConfirmOrder())
                    .createdByInfo(
                            CreatedByInfo.of(
                                    topic.getProposedBy().getId(),
                                    topic.getProposedBy().getNickname()
                            )
                    )
                    .build();
        }
    }

    public static ConfirmedTopicsResponse from(
            CursorResponse<ConfirmedTopicDto, ConfirmedTopicsCursor> page,
            Actions actions
    ) {
        return new ConfirmedTopicsResponse(page, actions);
    }
}
