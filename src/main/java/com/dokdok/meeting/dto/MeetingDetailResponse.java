package com.dokdok.meeting.dto;

import com.dokdok.book.entity.Book;
import com.dokdok.gathering.entity.Gathering;
import com.dokdok.meeting.entity.Meeting;
import com.dokdok.meeting.entity.MeetingMember;
import com.dokdok.meeting.entity.MeetingMemberRole;
import com.dokdok.meeting.entity.MeetingStatus;
import com.dokdok.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Schema(description = "약속 상세 응답")
public record MeetingDetailResponse(
        @Schema(description = "약속 ID", example = "1")
        Long meetingId,

        @Schema(description = "약속 이름", example = "1월 독서 모임")
        String meetingName,

        @Schema(description = "약속 상태", example = "CONFIRMED")
        MeetingStatus meetingStatus,

        @Schema(description = "약속 진행 상태(시간 기준)", example = "PRE")
        MeetingDetailProgressStatus progressStatus,

        @Schema(description = "주제 확정 여부", example = "true")
        Boolean confirmedTopic,

        @Schema(description = "주제 확정 날짜", example = "2026-01-30T10:00:00")
        LocalDateTime confirmedTopicDate,

        @Schema(description = "모임 정보")
        GatheringInfo gathering,

        @Schema(description = "책 정보")
        BookInfo book,

        @Schema(description = "일정 정보")
        ScheduleInfo schedule,

        @Schema(description = "장소 정보")
        MeetingLocationDto location,

        @Schema(description = "참가자 정보")
        ParticipantsInfo participants,

        @Schema(description = "약속 회고 상태", example = "AI_SUMMARY_COMPLETED")
        MeetingRetrospectiveStatus retrospectiveStatus,

        @Schema(description = "개인 회고 작성 여부", example = "true")
        Boolean personalRetrospectiveWritten,

        @Schema(description = "화면 버튼 상태")
        ActionState actionState
) {

    public static MeetingDetailResponse from(
            Meeting meeting,
            List<MeetingMember> meetingMembers,
            Long requestUserId,
            Boolean confirmedTopic,
            LocalDateTime confirmedTopicDate,
            MeetingRetrospectiveStatus retrospectiveStatus,
            Boolean personalRetrospectiveWritten,
            Map<Long, String> profileImageUrlMap
    ) {
        List<MeetingMember> safeMembers = meetingMembers == null ? Collections.emptyList() : meetingMembers;
        List<MeetingMember> activeMembers = safeMembers.stream()
                .filter(member -> member.getCanceledAt() == null)
                .toList();

        ParticipantsInfo participantsInfo = ParticipantsInfo.from(
                activeMembers,
                meeting.getMaxParticipants(),
                profileImageUrlMap
        );

        ActionState actionState = calculateActionState(
                meeting,
                requestUserId,
                activeMembers,
                participantsInfo.currentCount(),
                participantsInfo.maxCount()
        );

        MeetingDetailProgressStatus progressStatus = resolveProgressStatus(
                meeting.getMeetingStartDate(),
                meeting.getMeetingEndDate(),
                LocalDateTime.now()
        );

        return new MeetingDetailResponse(
                meeting.getId(),
                meeting.getMeetingName(),
                meeting.getMeetingStatus(),
                progressStatus,
                confirmedTopic,
                confirmedTopicDate,
                GatheringInfo.from(meeting.getGathering()),
                BookInfo.from(meeting.getBook()),
                ScheduleInfo.from(meeting.getMeetingStartDate(), meeting.getMeetingEndDate()),
                MeetingLocationDto.from(meeting.getLocation()),
                participantsInfo,
                retrospectiveStatus,
                personalRetrospectiveWritten,
                actionState
        );
    }

    private static MeetingDetailProgressStatus resolveProgressStatus(
            LocalDateTime meetingStartDate,
            LocalDateTime meetingEndDate,
            LocalDateTime now
    ) {
        if (meetingStartDate == null || meetingEndDate == null) {
            throw new IllegalStateException("meetingStartDate/meetingEndDate must be non-null");
        }
        if (now.isBefore(meetingStartDate)) {
            return MeetingDetailProgressStatus.PRE;
        }
        if (!now.isAfter(meetingEndDate)) {
            return MeetingDetailProgressStatus.ONGOING;
        }
        return MeetingDetailProgressStatus.POST;
    }

    private static ActionState calculateActionState(
            Meeting meeting,
            Long requestUserId,
            List<MeetingMember> activeMembers,
            int currentCount,
            Integer maxCount
    ) {
        if (meeting.getMeetingStatus() == MeetingStatus.DONE) {
            return ActionState.done();
        }
        if (meeting.getMeetingStatus() == MeetingStatus.REJECTED) {
            return ActionState.rejected();
        }

        User meetingLeader = meeting.getMeetingLeader();
        if (meetingLeader != null && meetingLeader.getId().equals(requestUserId)) {
            if (isActionTimeExpired(meeting.getMeetingStartDate())) {
                return ActionState.editTimeExpired();
            }
            return ActionState.canEdit();
        }

        boolean isParticipant = activeMembers.stream()
                .anyMatch(member -> member.getUser().getId().equals(requestUserId));
        if (isParticipant) {
            if (isActionTimeExpired(meeting.getMeetingStartDate())) {
                return ActionState.cancelTimeExpired();
            }
            return ActionState.canCancel();
        }

        if (isActionTimeExpired(meeting.getMeetingStartDate())) {
            return ActionState.joinTimeExpired();
        }

        if (isRecruitmentClosed(currentCount, maxCount)) {
            return ActionState.recruitmentClosed();
        }

        return ActionState.canJoin();
    }

    private static boolean isRecruitmentClosed(int currentCount, Integer maxCount) {
        return maxCount != null && currentCount >= maxCount;
    }

    private static boolean isActionTimeExpired(LocalDateTime meetingStartDate) {
        return meetingStartDate != null
                && meetingStartDate.isBefore(LocalDateTime.now().plusHours(24));
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

            @Schema(description = "저자", example = "로버트 C. 마틴")
            String authors,

            @Schema(description = "책 썸네일 URL", example = "https://example.com/thumb.jpg")
            String thumbnail
    ) {
        public static BookInfo from(Book book) {
            if (book == null) {
                return null;
            }
            return new BookInfo(book.getId(), book.getBookName(), book.getAuthor(), book.getThumbnail());
        }
    }

    @Schema(description = "일정 정보")
    public record ScheduleInfo(
            @Schema(description = "시작 일시", example = "2025-02-01T14:00:00")
            LocalDateTime startDateTime,

            @Schema(description = "종료 일시", example = "2025-02-01T16:00:00")
            LocalDateTime endDateTime,

            @Schema(description = "표시 문자열", example = "2026.01.01(목) 11:00 ~ 2026.01.01(목) 11:30")
            String displayDate
    ) {
        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
        private static final Map<DayOfWeek, String> DAY_OF_WEEK_KR = Map.of(
                DayOfWeek.MONDAY, "월",
                DayOfWeek.TUESDAY, "화",
                DayOfWeek.WEDNESDAY, "수",
                DayOfWeek.THURSDAY, "목",
                DayOfWeek.FRIDAY, "금",
                DayOfWeek.SATURDAY, "토",
                DayOfWeek.SUNDAY, "일"
        );

        public static ScheduleInfo from(LocalDateTime start, LocalDateTime end) {
            if (start == null && end == null) {
                return null;
            }
            String displayDate = formatDisplayDate(start, end);
            return new ScheduleInfo(start, end, displayDate);
        }

        private static String formatDisplayDate(LocalDateTime start, LocalDateTime end) {
            if (start == null) {
                return null;
            }

            String startDateStr = start.format(DATE_FORMATTER);
            String startDayOfWeek = DAY_OF_WEEK_KR.get(start.getDayOfWeek());
            String startTimeStr = start.format(TIME_FORMATTER);

            if (end == null) {
                return String.format("%s(%s) %s", startDateStr, startDayOfWeek, startTimeStr);
            }

            String endDateStr = end.format(DATE_FORMATTER);
            String endDayOfWeek = DAY_OF_WEEK_KR.get(end.getDayOfWeek());
            String endTimeStr = end.format(TIME_FORMATTER);

            return String.format("%s(%s) %s ~ %s(%s) %s",
                    startDateStr, startDayOfWeek, startTimeStr,
                    endDateStr, endDayOfWeek, endTimeStr);
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
                List<MeetingMember> activeMembers,
                Integer maxCount,
                Map<Long, String> profileImageUrlMap
        ) {
            List<MemberInfo> members = activeMembers.stream()
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
            String profileImageUrl,

            @Schema(description = "참가자 역할", example = "LEADER")
            MeetingMemberRole role
    ) {
        public static MemberInfo from(MeetingMember meetingMember, Map<Long, String> profileImageUrlMap) {
            User user = meetingMember.getUser();
            String profileImageUrl = resolveProfileImageUrl(user, profileImageUrlMap);
            return new MemberInfo(
                    user.getId(),
                    user.getNickname(),
                    profileImageUrl,
                    meetingMember.getMeetingRole()
            );
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

    @Schema(description = "화면 버튼 상태")
    public record ActionState(
            @Schema(description = """
                    버튼 타입
                    - 약속장: CAN_EDIT (enabled=true, buttonLabel=수정하기)
                    - 약속장(24시간 전): EDIT_TIME_EXPIRED (enabled=false, buttonLabel=수정 가능 시간이 지났어요)
                    - 모임원 + 미참가: CAN_JOIN (enabled=true, buttonLabel=참가 신청하기)
                    - 모임원 + 미참가(24시간 전): JOIN_TIME_EXPIRED (enabled=false, buttonLabel=참가 신청하기)
                    - 모임원 + 참가: CAN_CANCEL (enabled=true, buttonLabel=참가 신청 취소하기)
                    - 모임원 + 참가(24시간 전): CANCEL_TIME_EXPIRED (enabled=false, buttonLabel=참가 신청 취소하기)
                    - 모임원 + 모집 마감: RECRUITMENT_CLOSED (enabled=false, buttonLabel=모집인원이 마감되었어요)
                    - 약속 종료(DONE): DONE (enabled=false, buttonLabel=약속이 끝났어요)
                    - 약속 거절(REJECTED): REJECTED (enabled=false, buttonLabel=약속 신청이 거절됐어요)
                    """, example = "CAN_EDIT")
            MeetingActionType type,

            @Schema(description = "버튼 라벨", example = "수정하기")
            String buttonLabel,

            @Schema(description = "버튼 활성 여부", example = "true")
            boolean enabled
    ) {
        public static ActionState canEdit() {
            return fromType(MeetingActionType.CAN_EDIT);
        }

        public static ActionState editTimeExpired() {
            return fromType(MeetingActionType.EDIT_TIME_EXPIRED);
        }

        public static ActionState canJoin() {
            return fromType(MeetingActionType.CAN_JOIN);
        }

        public static ActionState joinTimeExpired() {
            return fromType(MeetingActionType.JOIN_TIME_EXPIRED);
        }

        public static ActionState canCancel() {
            return fromType(MeetingActionType.CAN_CANCEL);
        }

        public static ActionState cancelTimeExpired() {
            return fromType(MeetingActionType.CANCEL_TIME_EXPIRED);
        }

        public static ActionState recruitmentClosed() {
            return fromType(MeetingActionType.RECRUITMENT_CLOSED);
        }

        public static ActionState done() {
            return fromType(MeetingActionType.DONE);
        }

        public static ActionState rejected() {
            return fromType(MeetingActionType.REJECTED);
        }

        private static ActionState fromType(MeetingActionType type) {
            return new ActionState(type, type.getButtonLabel(), type.isEnabled());
        }
    }
}
