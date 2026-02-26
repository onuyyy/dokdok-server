package com.dokdok.meeting.dto;

import com.dokdok.book.entity.Book;
import com.dokdok.gathering.entity.Gathering;
import com.dokdok.meeting.entity.Meeting;
import com.dokdok.meeting.entity.MeetingMember;
import com.dokdok.meeting.entity.MeetingStatus;
import com.dokdok.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Schema(description = "약속 응답")
public record MeetingResponse(
        @Schema(description = "약속 ID", example = "1")
        Long meetingId,

        @Schema(description = "약속 이름", example = "1월 독서 모임")
        String meetingName,

        @Schema(description = "약속 상태", example = "CONFIRMED")
        MeetingStatus meetingStatus,

        @Schema(description = "모임 정보")
        GatheringInfo gathering,

        @Schema(description = "책 정보")
        BookInfo book,

        @Schema(description = "일정 정보")
        ScheduleInfo schedule,

        @Schema(description = "장소 정보")
        MeetingLocationDto location,

        @Schema(description = "참가자 정보")
        ParticipantsInfo participants
) {

    public static MeetingResponse from(Meeting meeting, List<MeetingMember> meetingMembers) {
        List<MeetingMember> safeMembers = meetingMembers == null ? Collections.emptyList() : meetingMembers;

        return new MeetingResponse(
                meeting.getId(),
                meeting.getMeetingName(),
                meeting.getMeetingStatus(),
                GatheringInfo.from(meeting.getGathering()),
                BookInfo.from(meeting.getBook()),
                ScheduleInfo.from(meeting.getMeetingStartDate(), meeting.getMeetingEndDate()),
                MeetingLocationDto.from(meeting.getLocation()),
                ParticipantsInfo.from(safeMembers, meeting.getMaxParticipants(), null)
        );
    }

    @Schema(description = "모임 정보")
    public record GatheringInfo(
            @Schema(description = "모임 ID", example = "1")
            Long gatheringId,

            @Schema(description = "모임 이름", example = "독서 모임")
            String gatheringName
    ) {
        public static GatheringInfo from(Gathering gathering) {
            if (gathering == null) {
                return null;
            }
            return new GatheringInfo(gathering.getId(), gathering.getGatheringName());
        }
    }

    @Schema(description = "책 정보")
    public record BookInfo(
            @Schema(description = "책 ID", example = "1")
            Long bookId,

            @Schema(description = "책 이름", example = "클린 코드")
            String bookName,

            @Schema(description = "책 썸네일 URL", example = "https://example.com/thumb.jpg")
            String thumbnail
    ) {
        public static BookInfo from(Book book) {
            if (book == null) {
                return null;
            }
            return new BookInfo(book.getId(), book.getBookName(), book.getThumbnail());
        }
    }

    @Schema(description = "일정 정보")
    public record ScheduleInfo(
            @Schema(description = "약속 날짜", example = "2025-02-01")
            LocalDate date,

            @Schema(description = "약속 시간", example = "14:00:00")
            LocalTime time,

            @Schema(description = "시작 일시", example = "2025-02-01T14:00:00")
            LocalDateTime startDateTime,

            @Schema(description = "종료 일시", example = "2025-02-01T16:00:00")
            LocalDateTime endDateTime
    ) {
        public static ScheduleInfo from(LocalDateTime start, LocalDateTime end) {
            if (start == null && end == null) {
                return null;
            }
            LocalDate date = start != null ? start.toLocalDate() : null;
            LocalTime time = start != null ? start.toLocalTime() : null;
            return new ScheduleInfo(date, time, start, end);
        }
    }

    @Schema(description = "참가자 정보")
    public record ParticipantsInfo(
            @Schema(description = "현재 참가자 수", example = "5")
            Integer currentCount,

            @Schema(description = "최대 참가자 수", example = "10")
            Integer maxCount,

            @Schema(description = "참가자 목록")
            List<MemberInfo> members
    ) {
        public static ParticipantsInfo from(
                List<MeetingMember> meetingMembers,
                Integer maxCount,
                Map<Long, String> profileImageUrlMap
        ) {
            List<MemberInfo> members = meetingMembers.stream()
                    .filter(member -> member.getCanceledAt() == null)
                    .map(member -> MemberInfo.from(member, profileImageUrlMap))
                    .toList();

            return new ParticipantsInfo(members.size(), maxCount, members);
        }
    }

    @Schema(description = "참가자 정보")
    public record MemberInfo(
            @Schema(description = "사용자 ID", example = "1")
            Long userId,

            @Schema(description = "닉네임", example = "독서왕")
            String nickname,

            @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
            String profileImageUrl
    ) {
        public static MemberInfo from(MeetingMember meetingMember, Map<Long, String> profileImageUrlMap) {
            User user = meetingMember.getUser();
            String profileImageUrl = resolveProfileImageUrl(user, profileImageUrlMap);
            return new MemberInfo(user.getId(), user.getNickname(), profileImageUrl);
        }
    }

    private static String resolveProfileImageUrl(User user, Map<Long, String> profileImageUrlMap) {
        if (user == null) {
            return null;
        }
        if (profileImageUrlMap == null) {
            return user.getProfileImageUrl();
        }
        String presignedUrl = profileImageUrlMap.get(user.getId());
        return presignedUrl != null ? presignedUrl : user.getProfileImageUrl();
    }
}
