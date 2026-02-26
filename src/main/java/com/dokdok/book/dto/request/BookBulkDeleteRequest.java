package com.dokdok.book.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record BookBulkDeleteRequest(
        @NotEmpty(message = "bookIds는 필수입니다.")
        List<
                @NotNull(message = "bookIds의 각 값은 필수입니다.")
                @Positive(message = "bookIds의 각 값은 양수여야 합니다.")
                Long> bookIds
) {
}
