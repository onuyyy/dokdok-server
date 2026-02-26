package com.dokdok.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "약속 회고 상태")
public enum MeetingRetrospectiveStatus {
    @Schema(description = "회고 생성 전")
    NOT_CREATED,
    @Schema(description = "AI 요약 완료")
    AI_SUMMARY_COMPLETED,
    @Schema(description = "최종 생성 완료")
    FINAL_PUBLISHED
}
