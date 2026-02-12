package com.dokdok.topic.dto.response;

import com.dokdok.book.entity.KeywordType;
import com.dokdok.topic.entity.Topic;
import com.dokdok.topic.entity.TopicAnswer;
import com.dokdok.topic.entity.TopicType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "사전 의견 조회 응답")
public record PreOpinionResponse(
        @Schema(description = "주제 목록")
        List<TopicInfo> topics,

        @Schema(description = "멤버별 사전 의견 목록")
        List<MemberPreOpinion> members
) {

    @Schema(description = "주제 정보")
    public record TopicInfo(
            @Schema(description = "주제 ID", example = "1")
            Long topicId,

            @Schema(description = "주제명", example = "책의 주요 메시지")
            String title,

            @Schema(description = "주제 설명", example = "이 책에서 전달하고자 하는 핵심 메시지는 무엇인가요?")
            String description,

            @Schema(description = "주제 타입", example = "DISCUSSION",
                    allowableValues = {"FREE", "DISCUSSION", "EMOTION", "EXPERIENCE", "CHARACTER_ANALYSIS", "COMPARISON", "STRUCTURE", "IN_DEPTH", "CREATIVE", "CUSTOM"})
            TopicType topicType,

            @Schema(description = "주제 타입 라벨", example = "토론형")
            String topicTypeLabel,

            @Schema(description = "주제 순서", example = "1")
            Integer confirmOrder
    ) {
        public static TopicInfo from(Topic topic) {
            return new TopicInfo(
                    topic.getId(),
                    topic.getTitle(),
                    topic.getDescription(),
                    topic.getTopicType(),
                    topic.getTopicType().getDisplayName(),
                    topic.getConfirmOrder()
            );
        }
    }

    @Schema(description = "멤버 사전 의견")
    public record MemberPreOpinion(
            @Schema(description = "멤버 정보")
            MemberInfo memberInfo,

            @Schema(description = "독서 리뷰 정보")
            BookReviewInfo bookReview,

            @Schema(description = "주제별 의견 목록")
            List<TopicOpinion> topicOpinions,

            @Schema(description = "답변 제출 여부")
            Boolean isSubmitted
    ) {
    }

    @Schema(description = "멤버 정보")
    public record MemberInfo(
            @Schema(description = "사용자 ID", example = "1")
            Long userId,

            @Schema(description = "닉네임", example = "독서왕")
            String nickname,

            @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
            String profileImage,

            @Schema(description = "역할", example = "MEMBER")
            String role
    ) {
        public static MemberInfo of(
                Long userId,
                String nickname,
                String profileImage,
                String role
        ) {
            return new MemberInfo(userId, nickname, profileImage, role);
        }
    }

    @Schema(description = "독서 리뷰 정보")
    public record BookReviewInfo(
            @Schema(description = "평점", example = "4.5")
            BigDecimal rating,

            @Schema(description = "책 키워드 목록", example = "[\"성장\", \"관계\"]")
            List<KeywordInfo> keywordInfo
    ) {
        public static BookReviewInfo of(
                BigDecimal rating,
                List<KeywordInfo> keywordInfo
        ) {
            return new BookReviewInfo(rating, keywordInfo);
        }
    }

    @Schema(description = "리뷰 키워드 정보")
    public record KeywordInfo(
            @Schema(description = "키워드 ID", example = "3")
            Long id,
            @Schema(description = "키워드 이름", example = "판타지")
            String name,
            @Schema(description = "키워드 타입", example = "BOOK")
            KeywordType type
    ) {

        public static KeywordInfo of(
                Long id,
                String name,
                KeywordType type
        ) {
            return new KeywordInfo(id, name, type);
        }
    }

    @Schema(description = "주제별 의견")
    public record TopicOpinion(
            @Schema(description = "주제 ID", example = "1")
            Long topicId,

            @Schema(description = "의견 내용", example = "저는 이 책의 핵심 메시지가 자기 성찰이라고 생각합니다.")
            String content
    ) {
        public static TopicOpinion of(TopicAnswer topicAnswer) {
            return new TopicOpinion(
                    topicAnswer.getTopic().getId(),
                    topicAnswer.getContent()
            );
        }
    }

    public static PreOpinionResponse of(
            List<TopicInfo> topics,
            List<MemberPreOpinion> members
    ) {
        return new PreOpinionResponse(topics, members);
    }
}