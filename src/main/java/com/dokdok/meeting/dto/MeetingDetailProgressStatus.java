package com.dokdok.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "약속 상세 진행 상태 (시간 기준) - PRE(약속 전), ONGOING(약속 중), POST(약속 후)")
public enum MeetingDetailProgressStatus {
    PRE,
    ONGOING,
    POST
}
