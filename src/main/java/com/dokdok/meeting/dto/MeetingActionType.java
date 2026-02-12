package com.dokdok.meeting.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MeetingActionType {
    CAN_EDIT("수정하기", true),
    EDIT_TIME_EXPIRED("수정 가능 시간이 지났어요", false),
    CAN_JOIN("참가 신청하기", true),
    JOIN_TIME_EXPIRED("참가 신청하기", false),
    CAN_CANCEL("참가 신청 취소하기", true),
    CANCEL_TIME_EXPIRED("참가 신청 취소하기", false),
    RECRUITMENT_CLOSED("모집인원이 마감되었어요", false),
    DONE("약속이 끝났어요", false),
    REJECTED("약속 신청이 거절됐어요", false);

    private final String buttonLabel;
    private final boolean enabled;
}
