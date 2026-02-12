package com.dokdok.gathering.dto.request;

import com.dokdok.gathering.entity.GatheringMemberStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "모임 가입 승인 요청")
public record JoinGatheringMemberRequest(

        @Schema(description = "승인 상태", example = "ACTIVE", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "승인상태는 빈 값일 수 없습니다.")
        GatheringMemberStatus approve_type
) {
}
