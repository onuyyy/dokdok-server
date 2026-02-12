package com.dokdok.retrospective.dto.response;

import com.dokdok.retrospective.entity.PersonalMeetingRetrospective;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "개인 회고 저장/수정 응답")
public record PersonalRetrospectiveResponse(
        @Schema(description = "개인 회고 ID", example = "1")
        Long personalMeetingRetrospectiveId,
        @Schema(description = "약속 ID", example = "1")
        Long meetingId,
        @Schema(description = "작성자 사용자 ID", example = "1")
        Long userId
) {
    public static PersonalRetrospectiveResponse from(PersonalMeetingRetrospective retrospective) {
        return new PersonalRetrospectiveResponse(
                retrospective.getId(),
                retrospective.getMeeting().getId(),
                retrospective.getUser().getId()
        );
    }
}
