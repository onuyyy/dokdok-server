package com.dokdok.retrospective.dto.response;

import com.dokdok.book.entity.ReflectionRecordType;
import com.dokdok.retrospective.dto.projection.ChangedThoughtProjection;
import com.dokdok.retrospective.dto.projection.FreeTextProjection;
import com.dokdok.retrospective.dto.projection.OtherPerspectiveProjection;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "회고 기록 응답")
public record RetrospectiveRecordResponse(
        @Schema(description = "개인 회고 ID", example = "1")
        Long retrospectiveId,
        @Schema(description = "모임 이름", example = "독서 모임")
        String gatheringName,
        @Schema(description = "기록 유형", example = "개인 회고")
        ReflectionRecordType recordType,
        @Schema(description = "작성 일시", example = "2025-02-01T16:30:00")
        LocalDateTime createdAt,
        @Schema(description = "주제별 그룹 목록")
        List<TopicGroup> topicGroups,
        @Schema(description = "자유 서술 목록")
        List<FreeText> freeTexts
) {
    @Schema(description = "주제별 그룹")
    public record TopicGroup(
            @Schema(description = "주제 ID", example = "1")
            Long topicId,
            @Schema(description = "주제명", example = "1")
            String topicTitle,
            @Schema(description = "주제 확정 순서", example = "1")
            Integer confirmOrder,
            @Schema(description = "생각 변화 목록")
            ChangedThought changedThought,
            @Schema(description = "타인의 관점 목록")
            List<OthersPerspective> othersPerspectives
    ) {}

    @Schema(description = "생각 변화")
    public record ChangedThought(
            @Schema(description = "핵심 쟁점", example = "요약된 핵심 쟁점")
            String keyIssue,
            @Schema(description = "사후 의견", example = "토론 후 바뀐 생각")
            String postOpinion
    ) {
        public static ChangedThought from(ChangedThoughtProjection projection) {
            return new ChangedThought(
                    projection.keyIssue(),
                    projection.postOpinion()
            );
        }
    }

    @Schema(description = "타인의 관점")
    public record OthersPerspective(
            @Schema(description = "약속 멤버 ID", example = "10")
            Long meetingMemberId,
            @Schema(description = "멤버 닉네임", example = "독서왕")
            String memberNickname,
            @Schema(description = "의견 내용", example = "상대 의견이 인상적이었습니다.")
            String opinionContent,
            @Schema(description = "인상 깊었던 이유", example = "새로운 관점을 제공했기 때문입니다.")
            String impressiveReason
    ) {
        public static OthersPerspective from(OtherPerspectiveProjection projection) {
            return new OthersPerspective(
                    projection.meetingMemberId(),
                    projection.memberNickname(),
                    projection.opinionContent(),
                    projection.impressiveReason()
            );
        }
    }

    @Schema(description = "자유 서술")
    public record FreeText(
            @Schema(description = "제목", example = "오늘의 한 줄")
            String title,
            @Schema(description = "내용", example = "회고 내용을 작성합니다.")
            String content
    ) {
        public static FreeText from(FreeTextProjection freeText) {
            return new FreeText(
                    freeText.title(),
                    freeText.content()
            );
        }
    }

    public static RetrospectiveRecordResponse of(
            Long retrospectiveId,
            String gatheringName,
            ReflectionRecordType recordType,
            LocalDateTime createdAt,
            List<TopicGroup> topicGroups,
            List<FreeText> freeTexts
    ) {
        return new RetrospectiveRecordResponse(
                retrospectiveId,
                gatheringName,
                recordType,
                createdAt,
                topicGroups,
                freeTexts
        );
    }
}
