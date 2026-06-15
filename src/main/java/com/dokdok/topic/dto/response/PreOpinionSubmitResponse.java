package com.dokdok.topic.dto.response;

import com.dokdok.book.dto.response.BookReviewResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "사전 의견 일괄 제출 응답")
public record PreOpinionSubmitResponse(
        @Schema(description = "사전의견 전용 책 평가 응답. 책장 리뷰에도 같은 내용이 반영됩니다.")
        BookReviewResponse review,
        @Schema(description = "토픽 답변 제출 결과")
        List<TopicAnswerSubmitResponse> answers
) {
    public static PreOpinionSubmitResponse of(BookReviewResponse review, List<TopicAnswerSubmitResponse> answers) {
        return new PreOpinionSubmitResponse(review, answers);
    }
}
