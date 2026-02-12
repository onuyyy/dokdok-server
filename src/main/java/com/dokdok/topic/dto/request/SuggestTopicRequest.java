package com.dokdok.topic.dto.request;

import com.dokdok.topic.entity.TopicType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "주제 제안 요청")
public record SuggestTopicRequest(

        @Schema(description = "주제 제목", example = "책의 주요 메시지", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "제목은 필수입니다")
        @Size(max = 100, message = "제목은 100자 이내여야 합니다")
        String title,

        @Schema(description = "주제 설명", example = "이 책에서 전달하고자 하는 핵심 메시지는 무엇인가요?", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "설명은 필수입니다")
        @Size(max = 1000, message = "설명은 1000자 이내여야 합니다")
        String description,

        @Schema(description = "주제 타입 (FREE: 자유형, DISCUSSION: 토론형, EMOTION: 감정 공유형, EXPERIENCE: 경험 연결형, CHARACTER_ANALYSIS: 인물 분석형, COMPARISON: 비교 분석형, STRUCTURE: 구조 분석형, IN_DEPTH: 심층 분석형, CREATIVE: 창작형, CUSTOM: 질문형)",
                example = "DISCUSSION", requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"FREE", "DISCUSSION", "EMOTION", "EXPERIENCE", "CHARACTER_ANALYSIS", "COMPARISON", "STRUCTURE", "IN_DEPTH", "CREATIVE", "CUSTOM"})
        @NotNull(message = "주제 타입은 필수입니다")
        TopicType topicType
) {
}