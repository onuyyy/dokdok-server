package com.dokdok.retrospective.dto.response;

import com.dokdok.retrospective.entity.TopicRetrospectiveSummary;
import com.dokdok.topic.entity.Topic;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Schema(description = "AI 요약 조회 응답")
@Builder
public record RetrospectiveSummaryResponse(
        @Schema(description = "약속 ID", example = "1")
        Long meetingId,

        @Schema(description = "토픽 목록")
        List<TopicSummaryResponse> topics
) {
    public static RetrospectiveSummaryResponse from(Long meetingId, List<TopicSummaryResponse> topics) {
        return RetrospectiveSummaryResponse.builder()
                .meetingId(meetingId)
                .topics(topics)
                .build();
    }

    @Schema(description = "토픽별 AI 요약")
    @Builder
    public record TopicSummaryResponse(
            @Schema(description = "토픽 ID", example = "1")
            Long topicId,

            @Schema(description = "주제 순번", example = "1")
            Integer confirmOrder,

            @Schema(description = "토픽 제목", example = "가짜 욕망, 유사 욕망")
            String topicTitle,

            @Schema(description = "토픽 설명", example = "가짜욕망, 유사욕망에 대해 이야기해봅시다.")
            String topicDescription,

            @Schema(description = "핵심 요약", example = "참여자들은 『데미안』 속 싱클레어가...")
            String summary,

            @Schema(description = "주요 포인트 목록")
            List<KeyPointResponse> keyPoints
    ) {
        public static TopicSummaryResponse from(Topic topic, TopicRetrospectiveSummary summary) {
            return TopicSummaryResponse.builder()
                    .topicId(topic.getId())
                    .confirmOrder(topic.getConfirmOrder())
                    .topicTitle(topic.getTitle())
                    .topicDescription(topic.getDescription())
                    .summary(summary != null ? summary.getSummary() : null)
                    .keyPoints(summary != null && summary.getKeyPoints() != null
                            ? summary.getKeyPoints().stream()
                            .map(KeyPointResponse::from)
                            .toList()
                            : null)
                    .build();
        }
    }

    @Schema(description = "주요 포인트")
    @Builder
    public record KeyPointResponse(
            @Schema(description = "포인트 제목", example = "사회가 만든 욕망의 구조")
            String title,

            @Schema(description = "포인트 내용 목록")
            List<String> details
    ) {
        public static KeyPointResponse from(TopicRetrospectiveSummary.KeyPoint keyPoint) {
            return KeyPointResponse.builder()
                    .title(keyPoint.getTitle())
                    .details(keyPoint.getDetails())
                    .build();
        }
    }
}
