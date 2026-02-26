package com.dokdok.stt.dto;

import com.dokdok.stt.entity.SttJob;
import com.dokdok.stt.entity.SttJobStatus;
import com.dokdok.stt.entity.SttSummary;

import java.time.LocalDateTime;
import java.util.List;

public record SttJobResponse(
        Long jobId,
        Long meetingId,
        Long userId,
        SttJobStatus status,
        String summary,
        List<String> highlights,
        String errorMessage,
        LocalDateTime createdAt
) {
    public static SttJobResponse from(SttJob job, SttSummary summary) {
        return new SttJobResponse(
                job.getId(),
                job.getMeeting().getId(),
                job.getUser().getId(),
                job.getStatus(),
                summary != null ? summary.getSummary() : null,
                summary != null ? summary.getHighlights() : null,
                job.getErrorMessage(),
                job.getCreatedAt()
        );
    }
}
