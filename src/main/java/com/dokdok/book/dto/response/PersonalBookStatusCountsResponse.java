package com.dokdok.book.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "책장 상태별 개수")
@Builder
public record PersonalBookStatusCountsResponse(
        @Schema(description = "읽는 중(READING) 개수", example = "12")
        long reading,

        @Schema(description = "완독(COMPLETED) 개수", example = "7")
        long completed,

        @Schema(description = "읽기 전(PENDING) 개수", example = "3")
        long pending,

        @Schema(description = "전체 개수", example = "22")
        long total
) {
}
