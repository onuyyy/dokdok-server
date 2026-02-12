package com.dokdok.retrospective.service;

import com.dokdok.gathering.entity.GatheringMember;
import com.dokdok.gathering.entity.GatheringRole;
import com.dokdok.gathering.repository.GatheringMemberRepository;
import com.dokdok.gathering.service.GatheringValidator;
import com.dokdok.meeting.repository.MeetingMemberRepository;
import com.dokdok.meeting.service.MeetingValidator;
import com.dokdok.retrospective.entity.MeetingRetrospective;
import com.dokdok.retrospective.entity.PersonalMeetingRetrospective;
import com.dokdok.retrospective.exception.RetrospectiveErrorCode;
import com.dokdok.retrospective.exception.RetrospectiveException;
import com.dokdok.retrospective.repository.PersonalRetrospectiveRepository;
import com.dokdok.retrospective.repository.RetrospectiveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RetrospectiveValidator {

    private final PersonalRetrospectiveRepository personalRetrospectiveRepository;
    private final GatheringValidator gatheringValidator;
    private final MeetingValidator meetingValidator;
    private final MeetingMemberRepository meetingMemberRepository;
    private final GatheringMemberRepository gatheringMemberRepository;

    public void validateRetrospective(Long meetingId, Long userId) {
        boolean exists = personalRetrospectiveRepository.existsByMeetingIdAndUserId(meetingId, userId);

        if (exists) {
            throw new RetrospectiveException(RetrospectiveErrorCode.RETROSPECTIVE_ALREADY_EXISTS);
        }
    }

    public void validateRetrospectiveByUser(Long retrospectiveId, Long userId) {
        boolean exists = personalRetrospectiveRepository.existsByIdAndUserId(retrospectiveId, userId);

        if (exists) {
            throw new RetrospectiveException(RetrospectiveErrorCode.NOT_AUTHOR_OF_RETROSPECTIVE);
        }
    }

    public void validateMeetingRetrospectiveAccess(Long gatheringId, Long meetingId, Long userId) {
        meetingValidator.validateMeetingInGathering(meetingId, gatheringId);

        // 약속 참여자인지 확인
        boolean isMeetingMember = meetingMemberRepository.existsByMeetingIdAndUserId(meetingId, userId);
        if (isMeetingMember) {
            return;
        }

        // 모임장인지 확인
        GatheringMember member = gatheringMemberRepository.findByGatheringIdAndUserId(gatheringId, userId)
                .orElseThrow(() -> new RetrospectiveException(RetrospectiveErrorCode.NO_ACCESS_RETROSPECTIVE));

        if (member.getRole() != GatheringRole.LEADER) {
            throw new RetrospectiveException(RetrospectiveErrorCode.NO_ACCESS_RETROSPECTIVE);
        }
    }

    public void validateRetrospective(Long retrospectiveId) {
        boolean exists = personalRetrospectiveRepository.existsById(retrospectiveId);

        if (!exists) {
            throw new RetrospectiveException(RetrospectiveErrorCode.RETROSPECTIVE_NOT_FOUND);
        }
    }

    public PersonalMeetingRetrospective getRetrospective(Long retrospectiveId, Long userId) {

        return personalRetrospectiveRepository.findByIdAndUser_Id(retrospectiveId, userId)
                .orElseThrow(() -> new RetrospectiveException(RetrospectiveErrorCode.RETROSPECTIVE_NOT_FOUND));

    }

    public void validateMeetingRetrospectiveDeletePermission(MeetingRetrospective retrospective, Long userId) {
        // 삭제하는 사람이 작성자 본인인지 확인
        if (retrospective.getCreatedBy().getId().equals(userId)) {
            return;
        }

        Long gatheringId = retrospective.getMeeting().getGathering().getId();
        // 삭제하는 사람이 모임장인지 확인
        gatheringValidator.validateLeader(gatheringId, userId);
    }

    public List<PersonalMeetingRetrospective> getRetrospectives(Long bookId, Long userId) {
        List<PersonalMeetingRetrospective> retrospectives
                = personalRetrospectiveRepository.findByBookAndUser(bookId, userId);

        if (retrospectives.isEmpty()) {
            throw new RetrospectiveException(RetrospectiveErrorCode.RETROSPECTIVE_NOT_FOUND);
        }

        return retrospectives;
    }

    public void validateSummaryUpdatePermission(Long gatheringId, Long meetingId, Long userId) {
        meetingValidator.validateMeetingInGathering(meetingId, gatheringId);

        // 약속장인지 확인
        boolean isMeetingLeader = meetingValidator.isMeetingLeader(meetingId, userId);
        if (isMeetingLeader) {
            return;
        }

        // 모임장인지 확인
        GatheringMember member = gatheringMemberRepository.findByGatheringIdAndUserId(gatheringId, userId)
                .orElseThrow(() -> new RetrospectiveException(RetrospectiveErrorCode.NO_ACCESS_RETROSPECTIVE));

        if (member.getRole() != GatheringRole.LEADER) {
            throw new RetrospectiveException(RetrospectiveErrorCode.NO_ACCESS_RETROSPECTIVE);
        }
    }
}
