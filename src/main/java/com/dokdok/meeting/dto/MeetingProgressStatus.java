package com.dokdok.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "약속 진행 상태 (시간 기준)")
public enum MeetingProgressStatus {
    UPCOMING,
    ONGOING,
    DONE,
    UNKNOWN
}
