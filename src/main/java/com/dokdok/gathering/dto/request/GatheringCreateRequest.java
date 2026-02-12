package com.dokdok.gathering.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "모임 생성 요청")
public record GatheringCreateRequest(
        @Schema(description = "모임 이름", example = "독서 모임", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "모임의 이름은 필수입력입니다.")
        @Size(max = 12, message = "모임 이름은 12자 이내로 입력해주세요.")
        String gatheringName,

        @Schema(description = "모임 설명", example = "매주 함께 책을 읽는 모임입니다.")
        @Size(max = 150, message = "모임 설명은 150자 이내로 입력해주세요.")
        String gatheringDescription
) {
}
