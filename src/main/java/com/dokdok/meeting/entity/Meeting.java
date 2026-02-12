package com.dokdok.meeting.entity;

import com.dokdok.book.entity.Book;
import com.dokdok.gathering.entity.Gathering;
import com.dokdok.global.BaseTimeEntity;
import com.dokdok.meeting.dto.MeetingCreateRequest;
import com.dokdok.meeting.dto.MeetingUpdateRequest;
import com.dokdok.meeting.exception.MeetingErrorCode;
import com.dokdok.meeting.exception.MeetingException;
import com.dokdok.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "meeting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE meeting SET deleted_at = CURRENT_TIMESTAMP WHERE meeting_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Meeting extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "meeting_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gathering_id", nullable = false)
    private Gathering gathering;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_leader_id")
    private User meetingLeader;

    @Column(name = "meeting_name", length = 24)
    private String meetingName;

    @Embedded
    private MeetingLocation location;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "meeting_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MeetingStatus meetingStatus = MeetingStatus.PENDING;

    @Column(name = "meeting_start_date")
    private LocalDateTime meetingStartDate;

    @Column(name = "meeting_end_date")
    private LocalDateTime meetingEndDate;

    public static Meeting create(MeetingCreateRequest request, Gathering gathering, Book book, User user,
                                 Integer maxParticipants) {
        String meetingName = request.meetingName();
        if (meetingName == null || meetingName.isBlank()) {
            meetingName = book.getBookName();
        }

        return Meeting.builder()
                .gathering(gathering)
                .book(book)
                .meetingLeader(user)
                .meetingName(meetingName)
                .location(request.toLocationEntity())
                .maxParticipants(maxParticipants)
                .meetingStartDate(request.meetingStartDate())
                .meetingEndDate(request.meetingEndDate())
                .build();
    }

    public void changeStatus(MeetingStatus targetStatus) {

        // DONE/REJECTED 이후는 어떤 변경도 불가
        if (this.meetingStatus == MeetingStatus.DONE
                || this.meetingStatus == MeetingStatus.REJECTED) {
            throw new MeetingException(MeetingErrorCode.INVALID_MEETING_STATUS_CHANGE,
                    "종료된 약속의 상태는 변경할 수 없습니다.");
        }

        // CONFIRMED 이후에는 이전 상태로 되돌릴 수 없음
        if (this.meetingStatus == MeetingStatus.CONFIRMED
                && (targetStatus == MeetingStatus.PENDING || targetStatus == MeetingStatus.REJECTED)) {
            throw new MeetingException(MeetingErrorCode.INVALID_MEETING_STATUS_CHANGE,
                    "확정된 약속은 다시 신청 상태로 되돌릴 수 없습니다.");
        }

        // CONFIRMED는 PENDING에서만 가능
        if (targetStatus == MeetingStatus.CONFIRMED
                && this.meetingStatus != MeetingStatus.PENDING) {
            throw new MeetingException(MeetingErrorCode.INVALID_MEETING_STATUS_CHANGE,
                    "신청된 약속만 확정할 수 있습니다.");
        }

        // REJECTED는 PENDING에서만 가능
        if (targetStatus == MeetingStatus.REJECTED
                && this.meetingStatus != MeetingStatus.PENDING) {
            throw new MeetingException(MeetingErrorCode.INVALID_MEETING_STATUS_CHANGE,
                    "신청된 약속만 거절할 수 있습니다.");
        }

        this.meetingStatus = targetStatus;
    }

    public void update(MeetingUpdateRequest request) {

        if (request.meetingName() != null) {
            this.meetingName = request.meetingName();
        }

        if (request.startDate() != null) {
            this.meetingStartDate = request.startDate();
        }

        if (request.endDate() != null) {
            this.meetingEndDate = request.endDate();
        }
        if (request.location() != null) {
            this.location = request.toLocationEntity();
        }

        if (request.maxParticipants() != null) {
            this.maxParticipants = request.maxParticipants();
        }
    }

    public String getFormattedTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return meetingStartDate.format(formatter) + "-" + meetingEndDate.format(formatter);
    }

}
