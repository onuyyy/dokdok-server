package com.dokdok.retrospective.dto.response;

import com.dokdok.retrospective.entity.RetrospectiveChangedThought;
import com.dokdok.retrospective.entity.RetrospectiveFreeText;
import com.dokdok.retrospective.entity.RetrospectiveOthersPerspective;
import com.dokdok.topic.entity.Topic;
import com.dokdok.topic.entity.TopicAnswer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "개인 회고 상세 응답")
public record PersonalRetrospectiveDetailResponse(
        @Schema(description = "개인 회고 ID", example = "1")
        Long retrospectiveId,
        @Schema(description = "약속 헤더 정보")
        MeetingHeaderInfo meetingHeaderInfo,
        @Schema(description = "개인 회고 데이터")
        RetrospectiveData retrospective
) {

    @Schema(description = "개인 회고 데이터")
    public record RetrospectiveData(
            @Schema(description = "생각 변화 목록")
            List<ChangedThought> changedThoughts,
            @Schema(description = "타인의 관점 목록")
            List<OthersPerspective> othersPerspectives,
            @Schema(description = "자유 서술 목록")
            List<FreeText> freeTexts
    ) {
    }

    @Schema(description = "생각 변화")
    public record ChangedThought(
            @Schema(description = "주제 ID", example = "1")
            Long topicId,
            @Schema(description = "주제 제목", example = "깨끗한 코드")
            String topicTitle,
            @Schema(description = "핵심 쟁점", example = "요약된 핵심 쟁점")
            String keyIssue,
            @Schema(description = "사전 의견", example = "토론 전 나의 생각")
            String preOpinion,
            @Schema(description = "사후 의견", example = "토론 후 바뀐 생각")
            String postOpinion
    ) {
        public static ChangedThought of(Topic topic, RetrospectiveChangedThought ct, TopicAnswer ta) {
            return new ChangedThought(
                    topic.getId(),
                    topic.getTitle(),
                    ct != null ? ct.getKeyIssue() : null,
                    ta != null ? ta.getContent() : null,
                    ct != null ? ct.getPostOpinion() : null
            );
        }
    }

    @Schema(description = "타인의 관점")
    public record OthersPerspective(
            @Schema(description = "주제 ID", example = "1")
            Long topicId,
            @Schema(description = "주제 제목", example = "깨끗한 코드")
            String topicTitle,
            @Schema(description = "약속 멤버 ID", example = "10")
            Long meetingMemberId,
            @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
            String profileImage,
            @Schema(description = "닉네임", example = "독서왕")
            String nickname,
            @Schema(description = "의견 내용", example = "상대 의견이 인상적이었습니다.")
            String opinionContent,
            @Schema(description = "인상 깊었던 이유", example = "새로운 관점을 제공했기 때문입니다.")
            String impressiveReason
    ) {
        public static OthersPerspective from(
                RetrospectiveOthersPerspective othersPerspective,
                String profileImage
        ) {
            Long topicId = othersPerspective.getTopic() != null
                    ? othersPerspective.getTopic().getId()
                    : null;

            String topicTitle = othersPerspective.getTopic() != null
                    ? othersPerspective.getTopic().getTitle()
                    : null;

            return new OthersPerspective(
                    topicId,
                    topicTitle,
                    othersPerspective.getMeetingMember().getId(),
                    profileImage,
                    othersPerspective.getMeetingMember().getUser().getNickname(),
                    othersPerspective.getOpinionContent(),
                    othersPerspective.getImpressiveReason()
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
        public static FreeText from(RetrospectiveFreeText freeText) {
            return new FreeText(
                    freeText.getTitle(),
                    freeText.getContent()
            );
        }
    }

    public static PersonalRetrospectiveDetailResponse from(
            Long retrospectiveId,
            MeetingHeaderInfo meetingHeaderInfo,
            List<ChangedThought> changedThoughts,
            List<OthersPerspective> othersPerspectives,
            List<FreeText> freeTexts
    ) {
        return new PersonalRetrospectiveDetailResponse(
                retrospectiveId,
                meetingHeaderInfo,
                new RetrospectiveData(changedThoughts, othersPerspectives, freeTexts)
        );
    }
}
