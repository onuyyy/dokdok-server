package com.dokdok.retrospective.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "개인 회고 작성/수정 요청")
public record PersonalRetrospectiveRequest(

        @Schema(description = "생각 변화 목록")
        @Valid
        List<ChangedThoughtRequest> changedThoughts,

        @Schema(description = "타인의 관점 목록")
        @Valid
        List<OthersPerspectiveRequest> othersPerspectives,

        @Schema(description = "자유 서술 목록 (최대 10개)")
        @Valid
        @Size(max = 10, message = "자유 서술은 최대 10개까지 가능합니다")
        List<FreeTextRequest> freeTexts
) {
    @Schema(description = "생각 변화 항목")
    public record ChangedThoughtRequest(
            @Schema(description = "주제 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull(message = "topicId는 필수입니다")
            Long topicId,

            @Schema(description = "핵심 쟁점", example = "요약된 핵심 쟁점")
            @Size(max = 200, message = "핵심 쟁점은 200자 이내여야 합니다")
            String keyIssue,

            @Schema(description = "사후 의견", example = "논의 후 이렇게 생각이 바뀌었습니다.")
            @Size(max = 10_000, message = "의견은 10,000자 이내여야 합니다")
            String postOpinion
    ) {}

    @Schema(description = "타인의 관점 항목")
    public record OthersPerspectiveRequest(
            @Schema(description = "주제 ID", example = "1")
            Long topicId,

            @Schema(description = "약속 멤버 ID", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull(message = "meetingMemberId는 필수입니다")
            Long meetingMemberId,

            @Schema(description = "의견 내용", example = "상대 의견이 인상적이었습니다.")
            @Size(max = 10_000, message = "의견 내용은 10,000자 이내여야 합니다")
            String opinionContent,

            @Schema(description = "인상 깊었던 이유", example = "새로운 관점을 제공했기 때문입니다.")
            @Size(max = 5_000, message = "인상 깊었던 이유는 5,000자 이내여야 합니다")
            String impressiveReason
    ) {}

    @Schema(description = "자유 서술 항목")
    public record FreeTextRequest(
            @Schema(description = "제목", example = "오늘의 한 줄")
            @Size(max = 40, message = "자유 서술 제목은 공백 포함 40자 이내여야 합니다.")
            String title,
            @Schema(description = "내용", example = "회고 내용을 작성합니다.")
            @Size(max = 100_000, message = "자유 서술 내용은 공백 포함 100,000자 이내여야 합니다.")
            String content
    ) {}
}
