package com.dokdok.meeting.service;

import com.dokdok.book.dto.request.BookCreateRequest;
import com.dokdok.book.entity.Book;
import com.dokdok.book.exception.BookErrorCode;
import com.dokdok.book.exception.BookException;
import com.dokdok.book.repository.BookRepository;
import com.dokdok.book.service.BookValidator;
import com.dokdok.book.service.PersonalBookService;
import com.dokdok.gathering.entity.Gathering;
import com.dokdok.gathering.exception.GatheringErrorCode;
import com.dokdok.gathering.exception.GatheringException;
import com.dokdok.gathering.service.GatheringValidator;
import com.dokdok.gathering.repository.GatheringMemberRepository;
import com.dokdok.gathering.repository.GatheringRepository;
import com.dokdok.global.response.CursorResponse;
import com.dokdok.global.response.PageResponse;
import com.dokdok.global.util.SecurityUtil;
import com.dokdok.meeting.dto.*;
import com.dokdok.meeting.entity.Meeting;
import com.dokdok.meeting.entity.MeetingMember;
import com.dokdok.meeting.entity.MeetingMemberRole;
import com.dokdok.meeting.entity.MeetingStatus;
import com.dokdok.meeting.exception.MeetingErrorCode;
import com.dokdok.meeting.exception.MeetingException;
import com.dokdok.meeting.repository.MeetingMemberRepository;
import com.dokdok.meeting.repository.MeetingRepository;
import com.dokdok.retrospective.repository.PersonalRetrospectiveRepository;
import com.dokdok.retrospective.repository.TopicRetrospectiveSummaryRepository;
import com.dokdok.topic.entity.Topic;
import com.dokdok.topic.entity.TopicStatus;
import com.dokdok.topic.entity.TopicType;
import com.dokdok.topic.repository.TopicAnswerRepository;
import com.dokdok.topic.repository.TopicRepository;
import com.dokdok.topic.service.TopicService;
import com.dokdok.storage.service.StorageService;
import com.dokdok.user.entity.User;
import com.dokdok.user.service.UserValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetingMemberRepository meetingMemberRepository;
    private final TopicRepository topicRepository;
    private final TopicAnswerRepository topicAnswerRepository;
    private final TopicService topicService;
    private final StorageService storageService;
    private final GatheringRepository gatheringRepository;
    private final GatheringMemberRepository gatheringMemberRepository;
    private final GatheringValidator gatheringValidator;
    private final MeetingValidator meetingValidator;
    private final BookRepository bookRepository;
    private final BookValidator bookValidator;
    private final UserValidator userValidator;
    private final PersonalBookService personalBookService;
    private final TopicRetrospectiveSummaryRepository topicRetrospectiveSummaryRepository;
    private final PersonalRetrospectiveRepository personalRetrospectiveRepository;

    /**
     * 특정 약속의 정보를 확인할 수 있다. 모임에 속한 사용자만 조회 가능
     * @param meetingId 약속 식별자
     * @return 약속 응답 정보
     */
    @Transactional(readOnly = true)
    public MeetingDetailResponse findMeeting(Long meetingId) {

        Long userId = SecurityUtil.getCurrentUserId();
        Meeting meeting = meetingValidator.findMeetingOrThrow(meetingId);

        // 모임 멤버만 약속 조회 가능
        gatheringValidator.validateMembership(meeting.getGathering().getId(), userId);

        List<MeetingMember> meetingMembers = meetingMemberRepository.findAllByMeetingId(meetingId);
        Map<Long, String> profileImageUrlMap = buildProfileImageUrlMap(meetingMembers);

        LocalDateTime confirmedTopicDate = topicRepository.findConfirmedTopicDateByMeetingId(
                meetingId,
                TopicStatus.CONFIRMED
        );
        boolean confirmedTopic = confirmedTopicDate != null;

        MeetingRetrospectiveStatus retrospectiveStatus = resolveMeetingRetrospectiveStatus(meetingId, meeting);

        // 개인 회고 작성 여부
        boolean personalRetrospectiveWritten = personalRetrospectiveRepository
                .existsByMeetingIdAndUserId(meetingId, userId);

        return MeetingDetailResponse.from(
                meeting,
                meetingMembers,
                userId,
                confirmedTopic,
                confirmedTopicDate,
                retrospectiveStatus,
                personalRetrospectiveWritten,
                profileImageUrlMap
        );
    }

    /**
     * 약속 회고 상태를 계산한다: FINAL_PUBLISHED > AI_SUMMARY_COMPLETED > NOT_CREATED 순으로 판단.
     *   - FINAL_PUBLISHED: meeting.isRetrospectivePublished()가 true
     *   - AI_SUMMARY_COMPLETED: 확정 토픽이 있고, 해당 토픽 중 하나라도 TopicRetrospectiveSummary가 존재
     *   - NOT_CREATED: 위 두 조건 모두 불충족
     **/
    private MeetingRetrospectiveStatus resolveMeetingRetrospectiveStatus(Long meetingId, Meeting meeting) {
        if (meeting.isRetrospectivePublished()) {
            return MeetingRetrospectiveStatus.FINAL_PUBLISHED;
        }

        List<Long> topicIds = topicRepository.findConfirmedTopics(meetingId).stream()
                .map(Topic::getId)
                .toList();
        if (topicIds.isEmpty()) {
            return MeetingRetrospectiveStatus.NOT_CREATED;
        }

        boolean hasSummary = !topicRetrospectiveSummaryRepository.findAllByTopicIdIn(topicIds).isEmpty();
        return hasSummary
                ? MeetingRetrospectiveStatus.AI_SUMMARY_COMPLETED
                : MeetingRetrospectiveStatus.NOT_CREATED;
    }

    /**
     * 약속 멤버들의 프로필 이미지 presigned URL Map 생성
     */
    private Map<Long, String> buildProfileImageUrlMap(List<MeetingMember> members) {
        Map<Long, String> profileImageUrlMap = new HashMap<>();

        members.forEach(member -> {
            User user = member.getUser();
            if (user == null || user.getId() == null) {
                return;
            }
            String profileImageUrl = user.getProfileImageUrl();
            String presignedUrl = storageService.getPresignedProfileImage(profileImageUrl);
            profileImageUrlMap.put(user.getId(), presignedUrl);
        });

        return profileImageUrlMap;
    }

    /**
     * 모임원이 약속 생성 신청을 할 수 있다. 모임에 속한 사용자만 생성 가능
     * @param request 약속 생성 신청 폼
     * @return 약속 응답 정보
     */
    @Transactional
    public MeetingResponse createMeeting(MeetingCreateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();

        // 모임 멤버만 약속 생성 가능
        gatheringValidator.validateMembership(request.gatheringId(), userId);

        Gathering gathering = gatheringRepository.findById(request.gatheringId())
                .orElseThrow(() -> new GatheringException(GatheringErrorCode.GATHERING_NOT_FOUND));

        Book book = bookRepository.findByIsbn(request.book().isbn())
                .orElseGet(() -> {
                    BookCreateRequest bookCreateRequest = new BookCreateRequest(
                            request.book().title(),
                            request.book().authors(),
                            request.book().publisher(),
                            request.book().isbn(),
                            request.book().thumbnail()
                    );
                    return bookRepository.save(bookCreateRequest.of());
                });

        User user = userValidator.findUserOrThrow(userId);

        Integer maxParticipants = request.maxParticipants();
        if (maxParticipants == null) {
            maxParticipants = gatheringMemberRepository
                    .countByGatheringIdAndRemovedAtIsNull(gathering.getId());
        }

        validateMeetingDatesRequired(request.meetingStartDate(), request.meetingEndDate());

        // 최대 참가 인원 검증
        validateMaxParticipants(maxParticipants, gathering.getId());

        Meeting meeting = Meeting.create(request, gathering, book, user, maxParticipants);
        Meeting savedMeeting = meetingRepository.save(meeting);

        return MeetingResponse.from(savedMeeting, List.of());
    }

    /**
     * 약속을 확정한다. 모임장만 확정 가능
     * @param meetingId 약속 식별자
     * @return 약속 상태 응답 정보
     */
    @Transactional
    public MeetingStatusResponse confirmMeeting(Long meetingId) {

        Long userId = SecurityUtil.getCurrentUserId();
        Meeting meeting = meetingValidator.findMeetingOrThrow(meetingId);

        // 모임장만 확정 가능
        gatheringValidator.validateLeader(meeting.getGathering().getId(), userId);

        validateConfirmable(meeting);
        ensureLeaderMember(meeting);

        meeting.changeStatus(MeetingStatus.CONFIRMED);
        saveMeetingBookForUser(meeting, meeting.getGathering(), meeting.getMeetingLeader().getId());
        topicService.createDefaultTopic(meeting);

        return MeetingStatusResponse.from(meeting);
    }

    /**
     * 약속을 거절한다. 모임장만 거절 가능
     * @param meetingId 약속 식별자
     * @return 약속 상태 응답 정보
     */
    @Transactional
    public MeetingStatusResponse rejectMeeting(Long meetingId) {

        Long userId = SecurityUtil.getCurrentUserId();
        Meeting meeting = meetingValidator.findMeetingOrThrow(meetingId);

        // 모임장만 거절 가능
        gatheringValidator.validateLeader(meeting.getGathering().getId(), userId);

        meeting.changeStatus(MeetingStatus.REJECTED);

        return MeetingStatusResponse.from(meeting);
    }

    /**
     * 최대 참가 인원의 유효성을 검증한다.
     * @param maxParticipants 최대 참가 인원
     * @param gatheringId 모임 식별자
     */
    private void validateMaxParticipants(Integer maxParticipants, Long gatheringId) {
        if (maxParticipants == null) {
            return;
        }
        if (maxParticipants < 1) {
            throw new MeetingException(MeetingErrorCode.INVALID_MAX_PARTICIPANTS);
        }

        int totalGatheringMembers = gatheringMemberRepository
                .countByGatheringIdAndRemovedAtIsNull(gatheringId);
        if (maxParticipants > totalGatheringMembers) {
            throw new MeetingException(MeetingErrorCode.INVALID_MAX_PARTICIPANTS);
        }
    }

    /**
     * 동일 모임 내 확정된 약속이 이미 존재하는지 검증한다.
     */
    private void validateConfirmable(Meeting meeting) {
        Long gatheringId = meeting.getGathering().getId();
        LocalDateTime startDate = meeting.getMeetingStartDate();
        LocalDateTime endDate = meeting.getMeetingEndDate();
        if (startDate == null || endDate == null) {
            throw new MeetingException(MeetingErrorCode.MEETING_DATE_REQUIRED,
                    "약속 시작/종료 일시는 필수입니다.");
        }
        boolean hasOverlappingMeeting = meetingRepository.existsOverlappingMeeting(
                gatheringId,
                MeetingStatus.CONFIRMED,
                meeting.getId(),
                startDate,
                endDate
        );
        if (hasOverlappingMeeting) {
            throw new MeetingException(MeetingErrorCode.INVALID_MEETING_STATUS_CHANGE,
                    "동일 시간에 확정된 약속이 존재합니다.");
        }
    }

    /**
     * 약속장을 미팅 멤버로 포함하고 역할을 LEADER로 설정한다.
     */
    private void ensureLeaderMember(Meeting meeting) {
        User leader = meeting.getMeetingLeader();
        if (leader == null) {
            throw new MeetingException(MeetingErrorCode.INVALID_MEETING_STATUS_CHANGE,
                    "약속장 정보를 찾을 수 없습니다.");
        }

        MeetingMember meetingMember = meetingMemberRepository
                .findByMeetingIdAndUserId(meeting.getId(), leader.getId())
                .orElseGet(() -> MeetingMember.builder()
                        .meeting(meeting)
                        .user(leader)
                        .build());

        meetingMember.changeRole(MeetingMemberRole.LEADER);
        meetingMemberRepository.save(meetingMember);
    }

    /**
     * 약속 참가를 신청한다.
     * @param meetingId 약속 식별자
     * @return 신청 완료된 약속 식별자
     */
    @Transactional
    public Long joinMeeting(Long meetingId) {

        Long userId = SecurityUtil.getCurrentUserId();

        Meeting meeting = meetingValidator.findMeetingOrThrow(meetingId);

        validateJoinableMeetingStartDate(meeting);

        gatheringValidator.validateMembership(meeting.getGathering().getId(), userId);

        if (restoreCanceledMemberIfExists(meetingId, userId, meeting.getMaxParticipants())) {
            saveMeetingBook(meeting, meeting.getGathering());
            return meetingId;
        }

        meetingValidator.validateCapacity(meetingId, meeting.getMaxParticipants());

        User user = userValidator.findUserOrThrow(userId);

        saveMeetingMember(meeting, user);
        saveMeetingBook(meeting, meeting.getGathering());

        return meetingId;
    }

    /**
     * 약속 참가 신청 성공 시 책장에 등록한다.
     */
    private void saveMeetingBook(Meeting meeting, Gathering gathering) {
        Long userId = SecurityUtil.getCurrentUserId();
        saveMeetingBookForUser(meeting, gathering, userId);
    }

    /**
     * 특정 사용자 책장에 약속 책을 등록한다.
     */
    private void saveMeetingBookForUser(Meeting meeting, Gathering gathering, Long userId) {
        Book book = meeting.getBook();
        if (book == null) {
            throw new BookException(BookErrorCode.BOOK_NOT_FOUND);
        }

        if (bookValidator.isDuplicatePersonalBook(userId, book.getId())) {
            return;
        }
        personalBookService.createBook(BookCreateRequest.from(book), gathering);

    }

    /**
     * 기존 취소 이력이 있으면 정원 검증 후 복구하고, 이미 참여 상태면 예외를 던진다.
     */
    private boolean restoreCanceledMemberIfExists(Long meetingId, Long userId, Integer maxParticipants) {
        MeetingMember existingMember = meetingMemberRepository.findAnyByMeetingIdAndUserId(meetingId, userId)
                .orElse(null);
        if (existingMember == null) {
            return false;
        }
        if (existingMember.getCanceledAt() == null) {
            throw new MeetingException(MeetingErrorCode.MEETING_ALREADY_JOINED);
        }

        // 복구 전 정원 검증
        meetingValidator.validateCapacity(meetingId, maxParticipants);

        existingMember.restore();
        return true;
    }

    /**
     * 약속 참여 멤버를 생성 후 저장한다.
     */
    private void saveMeetingMember(Meeting meeting, User user) {
        MeetingMember meetingMember = MeetingMember.builder()
                .meeting(meeting)
                .user(user)
                .build();

        meetingMemberRepository.save(meetingMember);
    }

    /**
     * 약속 참가 신청을 취소한다. 약속 시작까지 24시간 미만 남았으면 취소 불가
     * @param meetingId 약속 식별자
     * @return 취소한 약속 식별자
     */
    @Transactional
    public Long cancelMeeting(Long meetingId) {
        Long userId = SecurityUtil.getCurrentUserId();
        Meeting meeting = meetingValidator.findMeetingOrThrow(meetingId);

        // 약속 시간 24간 이내이면 취소 불가능
        LocalDateTime meetingStartDate = meeting.getMeetingStartDate();
        if (meetingStartDate != null
                && meetingStartDate.isBefore(LocalDateTime.now().plusHours(24))) {
            throw new MeetingException(MeetingErrorCode.MEETING_CANCEL_NOT_ALLOWED);
        }

        // 이미 취소한 약속인지 확인
        MeetingMember meetingMember = meetingValidator.getAnyMeetingMember(meetingId, userId);
        if (meetingMember.getCanceledAt() != null) {
            throw new MeetingException(MeetingErrorCode.MEETING_ALREADY_CANCELED);
        }

        meetingMember.cancel();
        // 참가 취소자가 주제까지 제안한 경우 주제 soft delete
        topicRepository.softDeleteByMeetingIdAndProposedById(meetingId, userId);
        // 개인 책장에서도 책 삭제
        personalBookService.deleteBookForMeeting(meeting.getBook().getId(), meeting.getGathering().getId());

        return meetingId;
    }

    /**
     * 약속을 삭제한다. 모임장만 삭제 가능
     * 진행 전 상태의 약속만 삭제 가능
     * 약속 시작 24시간 이내 삭제 불가
     * @param meetingId 약속 식별자
     */
    @Transactional
    public void deleteMeeting(Long meetingId) {
        Long userId = SecurityUtil.getCurrentUserId();
        Meeting meeting = meetingValidator.findMeetingOrThrow(meetingId);

        // 모임장만 약속 삭제 가능
        gatheringValidator.validateLeader(meeting.getGathering().getId(), userId);

        validateDeletableStatus(meeting);
        validateDeletableMeetingStartDate(meeting);

        topicAnswerRepository.softDeleteByMeetingId(meetingId);
        topicRepository.softDeleteByMeetingId(meetingId);
        meetingRepository.delete(meeting);
    }

    /**
     * 약속 삭제 가능 상태인지 확인한다.
     */
    private void validateDeletableStatus(Meeting meeting) {
        if (meeting.getMeetingStatus() == MeetingStatus.DONE) {
            throw new MeetingException(
                    MeetingErrorCode.INVALID_MEETING_STATUS_CHANGE,
                    "종료된 약속은 삭제할 수 없습니다."
            );
        }
    }

    /**
     * 약속 삭제 가능 시간이 지났는지 확인한다.
     */
    private void validateDeletableMeetingStartDate(Meeting meeting) {
        LocalDateTime meetingStartDate = meeting.getMeetingStartDate();
        if (meetingStartDate != null
                && meetingStartDate.isBefore(LocalDateTime.now().plusHours(24))) {
            throw new MeetingException(MeetingErrorCode.MEETING_DELETE_NOT_ALLOWED);
        }
    }

    /**
     * 약속 제안자가 약속을 수정한다.
     * 진행 전 상태의 약속만 수정 가능
     * 현재 참여 인원 수보다 작을 순 없음
     * maxParticipants가 null일 경우 기존 값 유지
     * endDate는 startDate 보다 이전일 수 없다
     * @param meetingId 약속 식별자
     * @param request 수정 요청 폼
     * @return 수정 완료된 응답
     */
    @Transactional
    public MeetingUpdateResponse updateMeeting(Long meetingId, MeetingUpdateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();

        Meeting meeting = meetingValidator.findMeetingOrThrow(meetingId);
        Long originMeetingId = meeting.getId();
        meetingValidator.validateMeetingLeader(meeting, userId);

        validateUpdatableStatus(meeting);
        validateUpdatableMeetingStartDate(meeting);

        Long gatheringId = meeting.getGathering().getId();
        validateMaxParticipants(request.maxParticipants(), gatheringId);

        int curMemberCount = meetingValidator.countActiveMembers(originMeetingId);
        validateMaxParticipantsNotLessThanCurrent(request.maxParticipants(), curMemberCount);

        validateMeetingDates(request, meeting);

        meeting.update(request);

        return MeetingUpdateResponse.from(meeting);
    }

    /**
     * 현재 모임 인원보다 약속 수정 요청에 대한 인원 수가 많지 않은지 확인한다.
     */
    private void validateMaxParticipantsNotLessThanCurrent(Integer maxParticipants, int currentParticipantCount) {
        if (maxParticipants != null && maxParticipants < currentParticipantCount) {
            throw new MeetingException(
                    MeetingErrorCode.MAX_PARTICIPANTS_LESS_THAN_CURRENT
            );
        }
    }

    /**
     * 수정 가능한 약속 상태인지 확인한다. (PENDING, CONFIRMED만 가능)
     */
    private void validateUpdatableStatus(Meeting meeting) {
        if (meeting.getMeetingStatus() == MeetingStatus.DONE) {
            throw new MeetingException(
                    MeetingErrorCode.INVALID_MEETING_STATUS_CHANGE,
                    "종료된 약속은 수정할 수 없습니다."
            );
        }
    }

    /**
     * 약속 시작 24시간 이내면 수정 불가
     */
    private void validateUpdatableMeetingStartDate(Meeting meeting) {
        LocalDateTime meetingStartDate = meeting.getMeetingStartDate();
        if (meetingStartDate != null
                && meetingStartDate.isBefore(LocalDateTime.now().plusHours(24))) {
            throw new MeetingException(MeetingErrorCode.MEETING_UPDATE_NOT_ALLOWED);
        }
    }

    /**
     * 약속 시작 24시간 이내면 참가 신청 불가
     */
    private void validateJoinableMeetingStartDate(Meeting meeting) {
        LocalDateTime meetingStartDate = meeting.getMeetingStartDate();
        if (meetingStartDate != null
                && meetingStartDate.isBefore(LocalDateTime.now().plusHours(24))) {
            throw new MeetingException(MeetingErrorCode.MEETING_JOIN_NOT_ALLOWED);
        }
    }

    /**
     * 종료 일시가 시작 일시보다 이전인지 확인한다.
     */
    private void validateMeetingDates(MeetingUpdateRequest request, Meeting meeting) {
        LocalDateTime startDate = request.startDate() != null
                ? request.startDate()
                : meeting.getMeetingStartDate();
        LocalDateTime endDate = request.endDate() != null
                ? request.endDate()
                : meeting.getMeetingEndDate();
        validateMeetingDatesRequired(startDate, endDate);
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new MeetingException(
                    MeetingErrorCode.INVALID_MEETING_STATUS_CHANGE,
                    "종료 일시는 시작 일시보다 이전일 수 없습니다."
            );
        }
    }

    private void validateMeetingDatesRequired(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            throw new MeetingException(
                    MeetingErrorCode.MEETING_DATE_REQUIRED,
                    "약속 시작/종료 일시는 필수입니다."
            );
        }
    }

    /**
     * 약속 리스트를 조회한다.
     * 약속 : 모임에서 약속이 확정된 전체 약속
     * 다가오는 약속 : 약속이 확정된 것들 중 3일 이내인 약속
     * 완료된 약속 : 약속이 완전히 끝난 약속
     * 내가 참여한 약속 : 완전히 끝난 약속 중 내가 참여한 약속
     * @param gatheringId 모임 식별자
     * @param filter 약속 리스트 필터
     * @param pageable 페이지 정보
     * @return PageResponse
     */
    public PageResponse<MeetingListItemResponse> meetingList(
            Long gatheringId,
            MeetingListFilter filter,
            Pageable pageable
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        gatheringValidator.validateMembership(gatheringId, userId);

        return switch (filter) {
            case ALL -> getAllMeetingsPage(gatheringId, pageable, userId);
            case UPCOMING -> getUpcomingMeetingsPage(gatheringId, pageable, userId);
            case DONE -> getDoneMeetingsPage(gatheringId, pageable, userId);
            case JOINED -> getJoinedMeetingsPage(gatheringId, pageable, userId);
        };

    }

    /**
     * 약속 탭별 뱃지 카운트 수를 반환한다.
     * @param gatheringId 모임 식별자
     * @return MeetingTabCountsResponse
     */
    public MeetingTabCountsResponse getMeetingTabCounts(Long gatheringId) {
        Long userId = SecurityUtil.getCurrentUserId();
        gatheringValidator.validateMembership(gatheringId, userId);

        int allCount = meetingRepository
                .countByGatheringIdAndMeetingStatus(gatheringId, MeetingStatus.CONFIRMED);
        int doneCount = meetingRepository
                .countByGatheringIdAndMeetingStatus(gatheringId, MeetingStatus.DONE);

        LocalDateTime now = LocalDateTime.now();
        int upcomingCount = meetingRepository.countUpcomingMeetings(
                gatheringId,
                MeetingStatus.CONFIRMED,
                now,
                now.plusDays(3)
        );

        int joinedCount = meetingMemberRepository.countMeetingsByUserIdAndStatus(
                userId,
                gatheringId,
                MeetingStatus.DONE
        );

        return MeetingTabCountsResponse.builder()
                .all(allCount)
                .upcoming(upcomingCount)
                .done(doneCount)
                .joined(joinedCount)
                .build();
    }

    /**
     * 모임장 약속 승인 리스트를 조회한다.
     * 확정 대기(PENDING)와 확정 완료(CONFIRMED)만 조회 가능
     */
    public PageResponse<MeetingListItemResponse> getApprovalMeetingList(
            Long gatheringId,
            MeetingStatus status,
            Pageable pageable
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        gatheringValidator.validateLeader(gatheringId, userId);

        if (status != MeetingStatus.PENDING && status != MeetingStatus.CONFIRMED) {
            throw new MeetingException(
                    MeetingErrorCode.INVALID_MEETING_STATUS_CHANGE,
                    "승인 리스트는 PENDING 또는 CONFIRMED만 조회할 수 있습니다."
            );
        }

        Page<Meeting> meetingPage = meetingRepository.findByGatheringIdAndMeetingStatus(
                gatheringId,
                status,
                pageable
        );

        List<MeetingListItemResponse> items = buildMeetingItems(meetingPage.getContent(), userId, gatheringId);
        return PageResponse.of(
                items,
                meetingPage.getTotalElements(),
                meetingPage.getNumber(),
                meetingPage.getSize()
        );
    }

    /**
     * 메인페이지 내 약속 리스트를 조회한다.
     * @param filter 필터(ALL, UPCOMING, DONE)
     * @param size 페이지 크기
     * @param cursor 커서
     * @return 약속 리스트
     */
    @Transactional(readOnly = true)
    public CursorResponse<MyMeetingListItemResponse, MeetingListCursor> getMyMeetingList(
            MyMeetingListFilter filter,
            int size,
            MeetingListCursor cursor
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        Pageable pageable = cursorPageable(size);
        LocalDateTime now = LocalDateTime.now();

        MyMeetingListFilter safeFilter = filter == null ? MyMeetingListFilter.ALL : filter;
        List<Meeting> meetings = switch (safeFilter) {
            case UPCOMING -> meetingMemberRepository.findMyUpcomingMeetingsAfterCursor(
                    userId,
                    MeetingStatus.CONFIRMED,
                    now,
                    now.plusDays(3),
                    cursorStartDateTime(cursor),
                    cursorMeetingId(cursor),
                    pageable
            );
            case DONE -> meetingMemberRepository.findMyDoneMeetingsWithoutPersonalRetrospectiveAfterCursor(
                    userId,
                    MeetingStatus.DONE,
                    cursorStartDateTime(cursor),
                    cursorMeetingId(cursor),
                    pageable
            );
            case ALL -> meetingMemberRepository.findMyMeetingsByStatusesAfterCursor(
                    userId,
                    List.of(MeetingStatus.CONFIRMED, MeetingStatus.DONE),
                    cursorStartDateTime(cursor),
                    cursorMeetingId(cursor),
                    pageable
            );
        };

        Integer totalCount = null;
        if (cursor == null) {
            totalCount = switch (safeFilter) {
                case UPCOMING -> meetingMemberRepository.countMyUpcomingMeetings(
                        userId,
                        MeetingStatus.CONFIRMED,
                        now,
                        now.plusDays(3)
                );
                case DONE -> meetingMemberRepository.countMyMeetingsByStatusWithoutPersonalRetrospective(
                        userId,
                        MeetingStatus.DONE
                );
                case ALL -> meetingMemberRepository.countMyMeetingsByStatuses(
                        userId,
                        List.of(MeetingStatus.CONFIRMED, MeetingStatus.DONE)
                );
            };
        }

        return buildMyMeetingListResponse(meetings, size, userId, totalCount);
    }

    /**
     * 메인페이지 내 약속 탭 카운트를 조회한다.
     * @return 탭별 카운트 응답
     */
    @Transactional(readOnly = true)
    public MyMeetingTabCountsResponse getMyMeetingTabCounts() {
        Long userId = SecurityUtil.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();

        int allCount = meetingMemberRepository.countMyMeetingsByStatuses(
                userId,
                List.of(MeetingStatus.CONFIRMED, MeetingStatus.DONE)
        );
        int upcomingCount = meetingMemberRepository.countMyUpcomingMeetings(
                userId,
                MeetingStatus.CONFIRMED,
                now,
                now.plusDays(3)
        );
        int doneCount = meetingMemberRepository.countMyMeetingsByStatusWithoutPersonalRetrospective(
                userId,
                MeetingStatus.DONE
        );

        return MyMeetingTabCountsResponse.builder()
                .all(allCount)
                .upcoming(upcomingCount)
                .done(doneCount)
                .build();
    }

    /**
     * 모임의 약속 중 확정된 리스트를 페이지로 반환한다.
     */
    private PageResponse<MeetingListItemResponse> getAllMeetingsPage(
            Long gatheringId,
            Pageable pageable,
            Long userId
    ) {
        Page<Meeting> meetingPage = meetingRepository.findByGatheringIdAndMeetingStatus(
                gatheringId,
                MeetingStatus.CONFIRMED,
                pageable
        );
        return buildMeetingPageResponse(meetingPage, userId, gatheringId);
    }

    /**
     * 다가오는 약속 리스트를 페이지로 반환한다.
     */
    private PageResponse<MeetingListItemResponse> getUpcomingMeetingsPage(
            Long gatheringId,
            Pageable pageable,
            Long userId
    ) {
        LocalDateTime now = LocalDateTime.now();
        Page<Meeting> meetingPage = meetingRepository.findByGatheringIdAndMeetingStatusAndMeetingStartDateBetween(
                gatheringId,
                MeetingStatus.CONFIRMED,
                now,
                now.plusDays(3),
                pageable
        );
        return buildMeetingPageResponse(meetingPage, userId, gatheringId);
    }

    /**
     * 완료된 약속 리스트를 페이지로 반환한다.
     */
    private PageResponse<MeetingListItemResponse> getDoneMeetingsPage(
            Long gatheringId,
            Pageable pageable,
            Long userId
    ) {
        Page<Meeting> meetingPage = meetingRepository.findByGatheringIdAndMeetingStatus(
                gatheringId,
                MeetingStatus.DONE,
                pageable
        );
        return buildMeetingPageResponse(meetingPage, userId, gatheringId);
    }

    /**
     * 완료된 약속 중 내가 참여했던 약속 리스트를 페이지로 반환한다.
     */
    private PageResponse<MeetingListItemResponse> getJoinedMeetingsPage(
            Long gatheringId,
            Pageable pageable,
            Long userId
    ) {
        Page<Meeting> meetingPage = meetingMemberRepository.findMeetingsByUserIdAndStatus(
                userId,
                gatheringId,
                MeetingStatus.DONE,
                pageable
        );
        return buildMeetingPageResponse(meetingPage, userId, gatheringId);
    }

    /**
     * 약속 리스트 페이지 응답을 구성한다.
     */
    private PageResponse<MeetingListItemResponse> buildMeetingPageResponse(
            Page<Meeting> meetingPage,
            Long userId,
            Long gatheringId
    ) {
        List<MeetingListItemResponse> items = buildMeetingItems(meetingPage.getContent(), userId, gatheringId);
        return PageResponse.of(
                items,
                meetingPage.getTotalElements(),
                meetingPage.getNumber(),
                meetingPage.getSize()
        );
    }

    /**
     * 내 약속 리스트 커서 응답을 구성한다.
     */
    private CursorResponse<MyMeetingListItemResponse, MeetingListCursor> buildMyMeetingListResponse(
            List<Meeting> meetingCandidates,
            int size,
            Long userId,
            Integer totalCount
    ) {
        boolean hasNext = meetingCandidates.size() > size;
        List<Meeting> meetings = hasNext ? meetingCandidates.subList(0, size) : meetingCandidates;
        if (meetings.isEmpty()) {
            return CursorResponse.of(List.of(), size, false, null, totalCount);
        }

        List<MyMeetingListItemResponse> items = buildMyMeetingItems(meetings, userId);

        MeetingListCursor nextCursor = null;
        if (hasNext) {
            Meeting last = meetings.get(meetings.size() - 1);
            nextCursor = new MeetingListCursor(last.getMeetingStartDate(), last.getId());
        }

        return CursorResponse.of(items, size, hasNext, nextCursor, totalCount);
    }

    /**
     * 약속 리스트 아이템을 생성한다.
     */
    private List<MeetingListItemResponse> buildMeetingItems(
            List<Meeting> meetings,
            Long userId,
            Long gatheringId
    ) {
        if (meetings.isEmpty()) {
            return List.of();
        }

        List<Long> meetingIds = meetings.stream()
                .map(Meeting::getId)
                .toList();

        Map<Long, List<TopicType>> topicTypesByMeetingId = findTopicTypes(meetingIds);
        Set<Long> joinedMeetingIds = new HashSet<>(
                meetingMemberRepository.findActiveMeetingIdsByUserIdAndGatheringId(userId, gatheringId)
        );

        List<MeetingListItemResponse> items = new ArrayList<>();
        for (Meeting meeting : meetings) {
            List<TopicType> topicTypes = topicTypesByMeetingId.getOrDefault(meeting.getId(), List.of());
            boolean joined = joinedMeetingIds.contains(meeting.getId());
            MeetingMyRole myRole = resolveMyRole(meeting, userId, joined);

            items.add(MeetingListItemResponse.builder()
                    .meetingId(meeting.getId())
                    .meetingName(meeting.getMeetingName())
                    .meetingLeaderName(meeting.getMeetingLeader() != null
                            ? meeting.getMeetingLeader().getNickname()
                            : null)
                    .bookName(meeting.getBook().getBookName())
                    .startDateTime(meeting.getMeetingStartDate())
                    .endDateTime(meeting.getMeetingEndDate())
                    .topicTypes(topicTypes)
                    .joined(joined)
                    .myRole(myRole)
                    .meetingStatus(meeting.getMeetingStatus())
                    .build());
        }
        return items;
    }

    /**
     * 내 약속 리스트 아이템을 생성한다.
     */
    private List<MyMeetingListItemResponse> buildMyMeetingItems(List<Meeting> meetings, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        List<MyMeetingListItemResponse> items = new ArrayList<>();

        List<Long> meetingIds = meetings.stream()
                .map(Meeting::getId)
                .toList();
        Set<Long> meetingIdsWithConfirmedTopics = new HashSet<>(
                topicRepository.findMeetingIdsWithConfirmedTopics(meetingIds)
        );

        for (Meeting meeting : meetings) {
            MeetingProgressStatus progressStatus = resolveProgressStatus(
                    meeting.getMeetingStartDate(),
                    meeting.getMeetingEndDate(),
                    now
            );
            MeetingMyRole myRole = resolveMyMeetingRole(meeting, userId);
            boolean preOpinionTemplateConfirmed = meetingIdsWithConfirmedTopics.contains(meeting.getId());

            items.add(new MyMeetingListItemResponse(
                    meeting.getId(),
                    meeting.getMeetingName(),
                    meeting.getGathering().getId(),
                    meeting.getGathering().getGatheringName(),
                    meeting.getMeetingLeader() != null ? meeting.getMeetingLeader().getNickname() : null,
                    meeting.getBook().getBookName(),
                    meeting.getMeetingStartDate(),
                    meeting.getMeetingEndDate(),
                    meeting.getMeetingStatus(),
                    myRole,
                    progressStatus,
                    preOpinionTemplateConfirmed
            ));
        }
        return items;
    }

    /**
     * 내 역할을 계산한다.
     */
    private MeetingMyRole resolveMyMeetingRole(Meeting meeting, Long userId) {
        User leader = meeting.getMeetingLeader();
        if (leader != null && leader.getId().equals(userId)) {
            return MeetingMyRole.LEADER;
        }
        User gatheringLeader = meeting.getGathering().getGatheringLeader();
        if (gatheringLeader != null && gatheringLeader.getId().equals(userId)) {
            return MeetingMyRole.GATHERING_LEADER;
        }
        return MeetingMyRole.MEMBER;
    }

    /**
     * 약속 진행 상태를 계산한다.
     */
    private MeetingProgressStatus resolveProgressStatus(
            LocalDateTime meetingStartDate,
            LocalDateTime meetingEndDate,
            LocalDateTime now
    ) {
        if (meetingStartDate == null || meetingEndDate == null) {
            return MeetingProgressStatus.UNKNOWN;
        }
        if (now.isBefore(meetingStartDate)) {
            return MeetingProgressStatus.UPCOMING;
        }
        if (!now.isAfter(meetingEndDate)) {
            return MeetingProgressStatus.ONGOING;
        }
        return MeetingProgressStatus.DONE;
    }

    /**
     * 리스트에서 내 역할을 계산한다.
     */
    private MeetingMyRole resolveMyRole(Meeting meeting, Long userId, boolean joined) {
        User leader = meeting.getMeetingLeader();
        if (leader != null && leader.getId().equals(userId)) {
            return MeetingMyRole.LEADER;
        }
        User gatheringLeader = meeting.getGathering().getGatheringLeader();
        if (gatheringLeader != null && gatheringLeader.getId().equals(userId)) {
            return MeetingMyRole.GATHERING_LEADER;
        }
        if (joined) {
            return MeetingMyRole.MEMBER;
        }
        return MeetingMyRole.NONE;
    }

    /**
     * 커서 페이지네이션을 위한 Pageable을 생성한다.
     */
    private Pageable cursorPageable(int size) {
        return PageRequest.of(0, size + 1);
    }

    /**
     * 커서의 시작 시간을 반환한다.
     */
    private LocalDateTime cursorStartDateTime(MeetingListCursor cursor) {
        return cursor == null ? null : cursor.startDateTime();
    }

    /**
     * 커서의 약속 ID를 반환한다.
     */
    private Long cursorMeetingId(MeetingListCursor cursor) {
        return cursor == null ? null : cursor.meetingId();
    }

    /**
     * 약속의 주제 타입 리스트를 반환한다.
     */
    private Map<Long, List<TopicType>> findTopicTypes(List<Long> meetingIds) {
        List<Object[]> rows = topicRepository.findTopicTypesByMeetingIds(meetingIds);
        Map<Long, Set<TopicType>> result = new HashMap<>();

        for (Object[] row : rows) {
            Long meetingId = (Long) row[0];
            TopicType topicType = (TopicType) row[1];
            result.computeIfAbsent(meetingId, key -> new LinkedHashSet<>()).add(topicType);
        }

        Map<Long, List<TopicType>> listResult = new HashMap<>();
        for (Map.Entry<Long, Set<TopicType>> entry : result.entrySet()) {
            listResult.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return listResult;
    }
}
