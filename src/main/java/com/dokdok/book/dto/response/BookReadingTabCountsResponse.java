package com.dokdok.book.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "홈화면 읽고 있는 책 탭별 카운트 응답")
@Builder
public record BookReadingTabCountsResponse(
        @Schema(description = "전체 책 수 (읽는 중 + 읽기 전 + 완독)", example = "5")
        long all,

        @Schema(description = "약속 전 책 수 (읽는 중 + 읽기 전)", example = "3")
        long before,

        @Schema(description = "약속 후 책 수 (완독)", example = "2")
        long after
) {
}
