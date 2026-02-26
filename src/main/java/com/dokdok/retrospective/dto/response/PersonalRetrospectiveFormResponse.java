package com.dokdok.retrospective.dto.response;

import com.dokdok.topic.entity.Topic;
import com.dokdok.topic.entity.TopicAnswer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "개인 회고 작성 폼 응답")
public record PersonalRetrospectiveFormResponse(
        @Schema(description = "약속 ID", example = "1")
        Long meetingId,
        @Schema(description = "약속 헤더 정보")
        MeetingHeaderInfo meetingHeaderInfo,
        @Schema(description = "사전 의견 목록")
        List<PreOpinions> preOpinions,
        @Schema(description = "확정된 주제 목록")
        List<TopicInfo> topics,
        @Schema(description = "약속 멤버 목록")
        List<MemberInfo> meetingMembers
) {
    @Schema(description = "사전 의견")
    public record PreOpinions(
        @Schema(description = "주제 ID", example = "1")
        Long topicId,
        @Schema(description = "주제 제목", example = "깨끗한 코드")
        String topicName,
        @Schema(description = "사전 의견 내용", example = "사전 의견 내용을 작성합니다.")
        String content
    ) {

        public static PreOpinions from(Topic topic, TopicAnswer topicAnswer) {
            return new PreOpinions(
                    topic.getId(),
                    topic.getTitle(),
                    topicAnswer.getContent()
            );
        }
    }

    public static PersonalRetrospectiveFormResponse of(
            Long meetingId,
            MeetingHeaderInfo meetingHeaderInfo,
            List<PreOpinions> preOpinions,
            List<TopicInfo> topics,
            List<MemberInfo> meetingMembers
    ) {
        return new PersonalRetrospectiveFormResponse(
                meetingId,
                meetingHeaderInfo,
                preOpinions,
                topics,
                meetingMembers
        );
    }
}
