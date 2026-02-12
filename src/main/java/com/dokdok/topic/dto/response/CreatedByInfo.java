package com.dokdok.topic.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "작성자 정보")
public record CreatedByInfo(
        @Schema(description = "사용자 ID", example = "1")
        Long userId,

        @Schema(description = "닉네임", example = "독서왕")
        String nickname
) {

    public static CreatedByInfo of(Long userId, String nickname) {
        return new CreatedByInfo(userId, nickname);
    }

}
