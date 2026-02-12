package com.dokdok.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "사용자 정보 수정 요청")
public record UpdateUserInfoRequest(
        @Schema(description = "닉네임", example = "독서왕")
        @NotBlank(message = "닉네임은 필수 입력 항목입니다.")
        String nickname
) {
}
