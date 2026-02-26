package com.dokdok.retrospective.dto.response;

import com.dokdok.meeting.entity.Meeting;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "약속 헤더 정보")
public record MeetingHeaderInfo(
        @Schema(description = "모임 이름", example = "독서 모임")
        String gatheringName,
        @Schema(description = "책 제목", example = "클린 코드")
        String bookTitle,
        @Schema(description = "저자", example = "로버트 마틴")
        String bookAuthor
) {
    public static MeetingHeaderInfo from(Meeting meeting) {
        return new MeetingHeaderInfo(
                meeting.getGathering().getGatheringName(),
                meeting.getBook().getBookName(),
                meeting.getBook().getAuthor()
        );
    }
}
