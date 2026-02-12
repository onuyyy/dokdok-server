package com.dokdok.meeting.service;

import com.dokdok.meeting.entity.Meeting;
import com.dokdok.meeting.entity.MeetingMember;
import com.dokdok.meeting.entity.MeetingStatus;
import com.dokdok.meeting.exception.MeetingErrorCode;
import com.dokdok.meeting.exception.MeetingException;
import com.dokdok.meeting.repository.MeetingMemberRepository;
import com.dokdok.meeting.repository.MeetingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MeetingValidator {

    private final MeetingRepository meetingRepository;
    private final MeetingMemberRepository meetingMemberRepository;

    public void validateMeeting(Long meetingId) {
        boolean isMeeting = meetingRepository.existsById(meetingId);

        if (!isMeeting) {
            throw new MeetingException(MeetingErrorCode.MEETING_NOT_FOUND);
        }
    }

    /**
     * 약속이 모임에 속하는지 검증한다.
     */
    public void validateMeetingInGathering(Long meetingId, Long gatheringId) {
        boolean isMeetingInGathering = meetingRepository
                .existsByIdAndGatheringId(meetingId, gatheringId);

        if (!isMeetingInGathering) {
            throw new MeetingException(MeetingErrorCode.NOT_GATHERING_MEETING);
        }
    }

    /**
     * meetingId로 약속을 조회하고 없으면 예외를 던진다.
     */
    public Meeting findMeetingOrThrow(Long meetingId) {
        return meetingRepository.findById(meetingId)
                .orElseThrow(() -> new MeetingException(MeetingErrorCode.MEETING_NOT_FOUND));
    }

    /**
     * 약속 상태가 CONFIRMED인지 검증한다.
     */
    public void validateMeetingStatus(Long meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new MeetingException(MeetingErrorCode.MEETING_NOT_FOUND));

        if (meeting.getMeetingStatus() != MeetingStatus.CONFIRMED) {
            throw new MeetingException(MeetingErrorCode.MEETING_NOT_CONFIRMED);
        }
    }

    /**
     * 사용자의 약속 참여 여부를 확인한다.
     */
    public void validateMeetingMember(Long meetingId, Long userId) {
        boolean isMeetingMember = meetingMemberRepository.existsByMeetingIdAndUserId(meetingId, userId);

        if(!isMeetingMember) {
            throw new MeetingException(MeetingErrorCode.NOT_MEETING_MEMBER);
        }
    }

    /**
     * 약속 정원 마감 여부를 검증한다.
     */
    public void validateCapacity(Long meetingId, Integer maxParticipants) {
        if (maxParticipants == null) {
            return;
        }
        int currentCount = meetingMemberRepository.countActiveMembers(meetingId);
        if (currentCount >= maxParticipants) {
            throw new MeetingException(MeetingErrorCode.MEETING_FULL);
        }
    }

    public MeetingMember getMeetingMember(Long meetingId, Long userId) {
        return meetingMemberRepository.findByMeetingIdAndUserId(meetingId, userId)
                .orElseThrow(() -> new MeetingException(MeetingErrorCode.NOT_MEETING_MEMBER));
    }

    public MeetingMember getAnyMeetingMember(Long meetingId, Long userId) {
        return meetingMemberRepository.findAnyByMeetingIdAndUserId(meetingId, userId)
                .orElseThrow(() -> new MeetingException(MeetingErrorCode.NOT_MEETING_MEMBER));
    }

    /**
     * 요청한 사용자가 약속장인지 검증한다.
     */
    public void validateMeetingLeader(Meeting meeting, Long userId) {
        if (meeting.getMeetingLeader() == null
                || meeting.getMeetingLeader().getId() == null
                || !meeting.getMeetingLeader().getId().equals(userId)) {
            throw new MeetingException(MeetingErrorCode.NOT_MEETING_LEADER);
        }
    }

    /**
     * 약속에 현재 참가한 사람의 인원 수를 리턴한다.
     */
    public int countActiveMembers(Long meetingId) {
        return meetingMemberRepository.countActiveMembers(meetingId);
    }

    /**
     * 요청한 사용자가 약속장인지 검증한다.
     */
    public boolean isMeetingLeader(Long meetingId, Long userId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new MeetingException(MeetingErrorCode.MEETING_NOT_FOUND));

        return meeting.getMeetingLeader() != null
                && meeting.getMeetingLeader().getId() != null
                && meeting.getMeetingLeader().getId().equals(userId);
    }
}
