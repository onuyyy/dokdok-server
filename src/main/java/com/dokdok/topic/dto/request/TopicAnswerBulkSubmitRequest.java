package com.dokdok.topic.dto.request;

import com.dokdok.book.dto.request.BookReviewRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "사전 의견 일괄 제출 요청")
public record TopicAnswerBulkSubmitRequest(
        @NotNull
        @Valid
        @Schema(description = "책 평가 정보")
        BookReviewRequest review,
        @NotEmpty
        @Schema(description = "제출할 토픽 ID 목록", example = "[1,2,3]")
        List<@NotNull Long> topicIds
) {
}
