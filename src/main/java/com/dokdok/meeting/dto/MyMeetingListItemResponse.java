package com.dokdok.meeting.dto;

import com.dokdok.meeting.entity.MeetingStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "메인페이지 내 약속 목록 아이템 응답")
public record MyMeetingListItemResponse(
        @Schema(description = "약속 ID", example = "1")
        Long meetingId,

        @Schema(description = "약속 이름", example = "1월 독서 모임")
        String meetingName,

        @Schema(description = "모임 ID", example = "10")
        Long gatheringId,

        @Schema(description = "모임 이름", example = "독서 모임")
        String gatheringName,

        @Schema(description = "약속장 이름", example = "독서왕")
        String meetingLeaderName,

        @Schema(description = "책 이름", example = "클린 코드")
        String bookName,

        @Schema(description = "시작 일시", example = "2025-02-01T14:00:00")
        LocalDateTime startDateTime,

        @Schema(description = "종료 일시", example = "2025-02-01T16:00:00")
        LocalDateTime endDateTime,

        @Schema(description = "약속 상태", example = "CONFIRMED")
        MeetingStatus meetingStatus,

        @Schema(description = "내 역할 (LEADER: 약속장, GATHERING_LEADER: 모임장, MEMBER: 참여자, NONE: 미참여)", example = "LEADER")
        MeetingMyRole myRole,

        @Schema(description = "약속 진행 상태(시간 기준)", example = "UPCOMING")
        MeetingProgressStatus progressStatus,

        @Schema(description = "사전 의견 템플릿 확정 여부", example = "true")
        boolean preOpinionTemplateConfirmed
) {
}
