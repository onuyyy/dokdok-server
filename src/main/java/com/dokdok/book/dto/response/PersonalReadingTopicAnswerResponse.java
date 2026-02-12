package com.dokdok.book.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "사전 의견 조회 응답")
public record PersonalReadingTopicAnswerResponse(
        @Schema(description = "응답 타입", example = "PRE_OPINION")
        String type,
        @Schema(description = "모임명", example = "책책책 책을 읽자")
        String gatheringName,
        @Schema(description = "공유일", example = "2026-01-05T21:38:00")
        LocalDateTime sharedAt,
        @Schema(description = "주제 목록")
        List<TopicAnswerInfo> topics
) {
    @Schema(description = "주제별 사전 의견")
    public record TopicAnswerInfo(
            @Schema(description = "주제명", example = "가짜 욕망, 유사 욕망")
            String topicTitle,
            @Schema(description = "주제 설명", example = "가짜 욕망, 유사 욕망에 대해 이야기해봅시다.")
            String topicDescription,
            @Schema(description = "확정 순서", example = "1")
            Integer confirmOrder,
            @Schema(description = "주제 답변", example = "가짜 욕망과 유사 욕망은 비슷해 보이지만 결이 다르다고 느꼈다.")
            String answer
    ) {
    }
}
