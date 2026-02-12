package com.dokdok.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        description = "내 역할 (LEADER: 약속장, GATHERING_LEADER: 모임장, MEMBER: 참여자, NONE: 미참여)",
        allowableValues = {"LEADER", "GATHERING_LEADER", "MEMBER", "NONE"}
)
public enum MeetingMyRole {
    LEADER, GATHERING_LEADER, MEMBER, NONE
}
