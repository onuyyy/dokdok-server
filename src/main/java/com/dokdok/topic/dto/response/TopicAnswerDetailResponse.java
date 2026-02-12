package com.dokdok.topic.dto.response;

import com.dokdok.book.dto.response.BookReviewResponse;
import com.dokdok.book.entity.Book;
import com.dokdok.topic.entity.Topic;
import com.dokdok.topic.entity.TopicAnswer;
import com.dokdok.topic.entity.TopicType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "사전 의견 작성 화면 응답")
public record TopicAnswerDetailResponse(
        @Schema(description = "책 정보")
        BookInfo book,
        @Schema(description = "책 평가 정보")
        BookReviewResponse review,
        @Schema(description = "사전 의견 정보")
        PreOpinion preOpinion
) {
    public record BookInfo(
            @Schema(description = "책 ID", example = "10")
            Long bookId,
            @Schema(description = "책 제목", example = "아주 작은 습관의 힘")
            String title,
            @Schema(description = "저자", example = "제임스 클리어")
            String author
    ) {
        public static BookInfo from(Book book) {
            if (book == null) {
                return null;
            }
            return new BookInfo(book.getId(), book.getBookName(), book.getAuthor());
        }
    }

    public record PreOpinion(
            @Schema(description = "최근 저장 일시", example = "2026-02-06T09:12:30")
            LocalDateTime updatedAt,
            @Schema(description = "주제별 사전 의견 목록")
            List<PreOpinionTopic> topics
    ) {
    }

    public record PreOpinionTopic(
            @Schema(description = "주제 ID", example = "1")
            Long topicId,
            @Schema(description = "주제 제목", example = "책의 주요 메시지")
            String topicTitle,
            @Schema(description = "주제 설명", example = "이 책에서 전달하고자 하는 핵심 메시지는 무엇인가요?")
            String topicDescription,
            @Schema(description = "주제 타입", example = "DISCUSSION")
            TopicType topicType,
            @Schema(description = "주제 타입 라벨", example = "토론형")
            String topicTypeLabel,
            @Schema(description = "확정 순서", example = "1")
            Integer confirmOrder,
            @Schema(description = "사전 의견 내용", example = "이 책은 작은 행동의 반복이 인생을 바꾼다고 생각합니다.")
            String content
    ) {
        public static PreOpinionTopic of(Topic topic, TopicAnswer answer) {
            return new PreOpinionTopic(
                    topic.getId(),
                    topic.getTitle(),
                    topic.getDescription(),
                    topic.getTopicType(),
                    topic.getTopicType().getDisplayName(),
                    topic.getConfirmOrder(),
                    answer != null ? answer.getContent() : null
            );
        }
    }

    public static TopicAnswerDetailResponse of(
            BookInfo book,
            BookReviewResponse review,
            PreOpinion preOpinion
    ) {
        return new TopicAnswerDetailResponse(book, review, preOpinion);
    }
}
