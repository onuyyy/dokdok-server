package com.dokdok.topic.dto.request;

import com.dokdok.book.dto.request.BookReviewRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "사전 의견 일괄 저장 요청")
public record TopicAnswerBulkSaveRequest(
        @NotNull
        @Valid
        @Schema(description = "책 평가 정보")
        BookReviewRequest review,
        @NotEmpty
        @Valid
        @Schema(description = "답변 목록")
        List<AnswerItem> answers
) {
    public record AnswerItem(
            @NotNull
            @Schema(description = "토픽 ID", example = "1")
            Long topicId,
            @Size(max = 1000, message = "설명은 1000자 이내여야 합니다")
            @Schema(description = "답변 내용", example = "이 책은 작은 행동의 반복이 인생을 바꾼다고 생각합니다.", nullable = true)
            String content
    ) {
    }
}
