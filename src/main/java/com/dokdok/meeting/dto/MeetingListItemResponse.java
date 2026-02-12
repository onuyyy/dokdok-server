package com.dokdok.meeting.dto;

import com.dokdok.meeting.entity.MeetingStatus;
import com.dokdok.topic.entity.TopicType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "약속 목록 아이템 응답")
@Builder
public record MeetingListItemResponse(
        @Schema(description = "약속 ID", example = "1")
        Long meetingId,

        @Schema(description = "약속 이름", example = "1월 독서 모임")
        String meetingName,

        @Schema(description = "약속장 이름", example = "독서왕")
        String meetingLeaderName,

        @Schema(description = "책 이름", example = "클린 코드")
        String bookName,

        @Schema(description = "시작 일시", example = "2025-02-01T14:00:00")
        LocalDateTime startDateTime,

        @Schema(description = "종료 일시", example = "2025-02-01T16:00:00")
        LocalDateTime endDateTime,

        @Schema(description = "주제 타입 목록", example = "[\"FREE\", \"CHAPTER\"]")
        List<TopicType> topicTypes,

        @Schema(description = "참가 여부", example = "true")
        boolean joined,

        @Schema(description = "내 역할 (LEADER: 약속장, GATHERING_LEADER: 모임장, MEMBER: 참여자, NONE: 미참여)", example = "LEADER")
        MeetingMyRole myRole,

        @Schema(description = "약속 상태", example = "CONFIRMED")
        MeetingStatus meetingStatus
) {
}
