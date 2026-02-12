package com.dokdok.retrospective.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;

@Schema(description = "AI 요약 수정 요청")
@Builder
public record RetrospectiveSummaryUpdateRequest(
        @Schema(description = "수정할 토픽 목록")
        @NotEmpty(message = "수정할 토픽 목록은 필수입니다.")
        @Valid
        List<TopicSummaryUpdateRequest> topics
) {
    @Schema(description = "토픽별 수정 요청")
    @Builder
    public record TopicSummaryUpdateRequest(
            @Schema(description = "토픽 ID", example = "1")
            @NotNull(message = "토픽 ID는 필수입니다.")
            Long topicId,

            @Schema(description = "핵심 요약", example = "수정된 핵심 요약...")
            @NotBlank(message = "핵심 요약은 필수입니다.")
            String summary,

            @Schema(description = "주요 포인트", example = "수정된 주요 포인트...")
            @NotEmpty(message = "주요 포인트는 필수입니다.")
            @Valid
            List<KeyPointUpdateRequest> keyPoints
    ){}

    @Schema(description = "주요 포인트 수정 요청")
    @Builder
    public record KeyPointUpdateRequest(
            @Schema(description = "포인트 제목", example = "사회가 만든 욕망의 구조")
            @NotBlank(message = "포인트 제목은 필수입니다.")
            String title,

            @Schema(description = "포인트 내용 목록")
            @NotEmpty(message = "포인트 내용은 필수입니다.")
            List<@NotBlank(message = "포인트 내용은 비어있을 수 없습니다.") String> details
    ) {}
}
