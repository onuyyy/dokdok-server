package com.dokdok.book.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "책의 약속 진행 상태")
public enum BookMeetingProgressStatus {
    BEFORE,
    AFTER
}
