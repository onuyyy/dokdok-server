package com.dokdok.topic.dto.response;

import com.dokdok.book.dto.response.BookReviewResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "사전 의견 일괄 저장 응답")
public record PreOpinionSaveResponse(
        @Schema(description = "책 평가 응답")
        BookReviewResponse review,
        @Schema(description = "토픽 답변 저장 결과")
        List<TopicAnswerResponse> answers
) {
    public static PreOpinionSaveResponse of(BookReviewResponse review, List<TopicAnswerResponse> answers) {
        return new PreOpinionSaveResponse(review, answers);
    }
}
