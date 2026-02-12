package com.dokdok.retrospective.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "수집된 사전 의견 커서")
public record CollectedAnswersCursor(
        @Schema(description = "마지막 멤버의 사용자 ID", example = "3")
        Long userId
) {
    public static CollectedAnswersCursor from(Long userId) {
        return new CollectedAnswersCursor(userId);
    }
}
