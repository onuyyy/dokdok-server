package com.dokdok.gathering.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import org.hibernate.validator.constraints.Length;

@Schema(description = "모임 수정 요청")
@Builder
public record GatheringUpdateRequest(
        @Schema(description = "모임 이름", example = "독서 모임 (수정)", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "모임명은 필수입니다")
        @Pattern(regexp = "^(?!\\s*$).{0,12}$",
                message = "모임명은 공백만 포함할 수 없으며 12자를 초과할 수 없습니다")
        String gatheringName,
        @Schema(description = "모임 설명", example = "매주 함께 책을 읽는 모임입니다.")
        @Length(max=150, message = "모임 설명은 150자를 초과할 수 없습니다.")
        String description
) {
}
