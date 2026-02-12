package com.dokdok.retrospective.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Schema(description = "모임 회고 작성 요청")
@Builder
public record MeetingRetrospectiveRequest(

        @Schema(description = "주제 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Long topicId,

        @Schema(description = "회고 코멘트 (공백 포함 500자 이내)", example = "이번 모임에서 핵심 논의가 잘 정리되었습니다.", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 500, message = "회고 코멘트는 500자 이내로 작성해주세요.")
        String comment
) { }
