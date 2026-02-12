package com.dokdok.gathering.service;

import com.dokdok.book.repository.BookReviewRepository;
import com.dokdok.gathering.dto.request.GatheringCreateRequest;
import com.dokdok.gathering.dto.request.GatheringUpdateRequest;
import com.dokdok.gathering.dto.request.JoinGatheringMemberRequest;
import com.dokdok.gathering.dto.response.*;
import com.dokdok.gathering.entity.*;
import com.dokdok.gathering.exception.GatheringErrorCode;
import com.dokdok.gathering.exception.GatheringException;
import com.dokdok.gathering.repository.GatheringMemberRepository;
import com.dokdok.gathering.repository.GatheringRepository;
import com.dokdok.global.response.CursorResponse;
import com.dokdok.global.response.PageResponse;
import com.dokdok.global.util.SecurityUtil;
import com.dokdok.gathering.util.InvitationCodeGenerator;
import com.dokdok.meeting.entity.MeetingStatus;
import com.dokdok.meeting.repository.MeetingMemberRepository;
import com.dokdok.meeting.repository.MeetingRepository;
import com.dokdok.storage.service.StorageService;
import com.dokdok.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GatheringService {

    private final GatheringRepository gatheringRepository;
    private final GatheringMemberRepository gatheringMemberRepository;
    private final GatheringValidator gatheringValidator;
    private final MeetingRepository meetingRepository;
    private final GatheringBookRepository gatheringBookRepository;
    private final BookReviewRepository bookReviewRepository;
    private final MeetingMemberRepository meetingMemberRepository;
    private final StorageService storageService;

    /**
     * 모임을 생성합니다.
     * 모임 생성을 요청하는 사용자가 해당 모임의 모임장이 됩니다.
     */
    @Transactional
    public GatheringCreateResponse createGathering(GatheringCreateRequest request) {

        User user = SecurityUtil.getCurrentUserEntity();
        String invitationLink = generateUniqueInvitationCode();

        Gathering gathering = Gathering.of(request.gatheringName(), request.gatheringDescription(), invitationLink, user);
        Gathering savedGathering = gatheringRepository.save(gathering);

        saveGatheringMember(savedGathering, user, GatheringRole.LEADER, GatheringMemberStatus.ACTIVE, LocalDateTime.now());

        return GatheringCreateResponse.from(
                savedGathering,
                getActiveMemberCount(savedGathering.getId()),
                getMeetingCount(savedGathering.getId())
        );
    }

    /**
     * 초대링크로 진입한 모임의 정보를 Summery정보를 보여줍니다.
     */
    public GatheringCreateResponse getJoinGatheringInfo(String invitationLink) {

        Gathering gathering = gatheringValidator.validateInvitationLink(invitationLink);
        return GatheringCreateResponse.from(
                gathering,
                getActiveMemberCount(gathering.getId()),
                getMeetingCount(gathering.getId())
        );
    }

    /**
     * 초대링크를 통해 들어온 사용자가 모임에 가입 요청을 합니다.
     */
    @Transactional
    public GatheringJoinResponse joinGathering(String invitationLink) {

        User user = SecurityUtil.getCurrentUserEntity();

        Gathering gathering = gatheringValidator.validateInvitationLink(invitationLink);
        gatheringValidator.validateJoinedGathering(gathering.getId(), user.getId());

        GatheringMember member = saveGatheringMember(gathering, user, GatheringRole.MEMBER, GatheringMemberStatus.PENDING, null);

        return GatheringJoinResponse.from(member);
    }

    /**
     * 모임장이 가입 요청을 한 멤버에 대해 승인|거절을 처리합니다.
     */
    @Transactional
    public void handleJoinRequest(Long gatheringId, Long memberId, JoinGatheringMemberRequest request) {

        User user = SecurityUtil.getCurrentUserEntity();
        gatheringValidator.validateLeader(gatheringId, user.getId());

        GatheringMemberStatus approveType = request.approve_type();
        if (approveType == GatheringMemberStatus.PENDING) {
            throw new GatheringException(GatheringErrorCode.INVALID_APPROVE_TYPE);
        }

        GatheringMember gatheringMember = gatheringMemberRepository.findByGatheringIdAndUserId(gatheringId, memberId)
                .orElseThrow(() -> new GatheringException(GatheringErrorCode.NOT_GATHERING_MEMBER));

        if (gatheringMember.getMemberStatus() != GatheringMemberStatus.PENDING) {
            throw new GatheringException(GatheringErrorCode.NOT_PENDING_STATUS);
        }

        gatheringMember.handleJoinRequest(approveType);
    }

    /**
     * 즐겨찾기 한 모임들을 조회합니다.
     */
    public FavoriteGatheringListResponse getFavoriteGatherings() {
        Long userId = SecurityUtil.getCurrentUserId();

        List<GatheringMember> favoriteMembers = gatheringMemberRepository.findFavoriteGatheringsByUserId(userId);

        List<GatheringListItemResponse> gatheringResponses = favoriteMembers.stream()
                .map(gatheringMember -> {
                    Gathering gathering = gatheringMember.getGathering();
                    int totalMembers = getActiveMemberCount(gathering.getId());
                    int totalMeetings = getMeetingCount(gathering.getId());
                    return GatheringListItemResponse.from(gatheringMember, totalMembers, totalMeetings, gatheringMember.getRole());
                })
                .toList();

        return FavoriteGatheringListResponse.from(gatheringResponses);
    }

    /**
     * 내 모임 전체 목록을 조회합니다. (페이지네이션)
     */
    public CursorResponse<GatheringListItemResponse, MyGatheringCursor> getMyGatherings(int pageSize, LocalDateTime cursorJoinedAt, Long cursorId) {
        Long userId = SecurityUtil.getCurrentUserId();

        Pageable pageable = PageRequest.of(0, pageSize + 1);

        List<GatheringMember> members;
        Integer totalCount = null;
        if (cursorJoinedAt == null || cursorId == null) {
            // 첫 페이지
            members = gatheringMemberRepository.findMyGatheringsFirstPage(userId, pageable);
            totalCount = gatheringMemberRepository.countMyGatherings(userId);
        } else {
            // 다음 페이지
            members = gatheringMemberRepository.findMyGatheringsAfterCursor(userId, cursorJoinedAt, cursorId, pageable);
        }

        boolean hasNext = members.size() > pageSize;
        List<GatheringMember> pageMembers = hasNext ? members.subList(0, pageSize) : members;

        List<GatheringListItemResponse> items = pageMembers.stream()
                .map(gm -> GatheringListItemResponse.from(
                        gm,
                        getActiveMemberCount(gm.getGathering().getId()),
                        getMeetingCount(gm.getGathering().getId()),
                        gm.getRole()))
                .toList();

        GatheringMember lastMember = pageMembers.isEmpty() ? null : pageMembers.get(pageMembers.size() - 1);
        MyGatheringCursor nextCursor = hasNext && lastMember != null ? MyGatheringCursor.from(lastMember) : null;

        return CursorResponse.of(items, pageSize, hasNext, nextCursor, totalCount);
    }

    /**
     * 모임 상세 정보 조회 - 모임 멤버만 조회 가능
     */
    public GatheringDetailResponse getGatheringDetail(Long gatheringId) {
        Long userId = SecurityUtil.getCurrentUserId();

        // 모임 존재 여부 및 멤버십 검증
        Gathering gathering = gatheringValidator.validateAndGetGathering(gatheringId);
        GatheringMember currentMember = gatheringValidator.validateAndGetMember(gatheringId, userId);

        // 모임의 모든 멤버 조회
        List<GatheringMember> allMember = gatheringMemberRepository.findAllMembersByGatheringId(gatheringId);

        // 총 약속 수
        int totalMeetings = meetingRepository.countByGatheringIdAndMeetingStatus(gathering.getId(), MeetingStatus.DONE);

        Map<Long, String> profileImageUrlMap = buildProfileImageUrlMap(allMember);

        return GatheringDetailResponse.from(
                currentMember,
                allMember,
                totalMeetings,
                profileImageUrlMap
        );
    }

    /**
     * 멤버들의 프로필 이미지 presigned URL Map 생성
     */
    private Map<Long, String> buildProfileImageUrlMap(List<GatheringMember> members) {
        Map<Long, String> profileImageUrlMap = new HashMap<>();

        members.forEach(member -> {
            String profileImageUrl = member.getUser().getProfileImageUrl();
            String presignedUrl = storageService.getPresignedProfileImage(profileImageUrl);
            profileImageUrlMap.put(member.getUser().getId(), presignedUrl);
        });

        return profileImageUrlMap;
    }

    /**
     * 모임 정보 수정 - 리더만 가능
     */
    @Transactional
    public GatheringUpdateResponse updateGathering(Long gatheringId, GatheringUpdateRequest request) {
        Long currentUserId = SecurityUtil.getCurrentUserId();

        // 모임 존재 여부 및 리더 권한 검증
        Gathering gathering = gatheringValidator.validateAndGetGathering(gatheringId);
        gatheringValidator.validateLeader(gatheringId, currentUserId);

        // 모임 정보 수정
        gathering.updateGatheringInfo(request.gatheringName(), request.description());

        return GatheringUpdateResponse.from(gathering);
    }

    // 모임 삭제 - 리더만 가능
    @Transactional
    public void deleteGathering(Long gatheringId) {
        Long userId = SecurityUtil.getCurrentUserId();

        // 모임 존재 여부 & 리더 권한 검증
        Gathering gathering = gatheringValidator.validateAndGetGathering(gatheringId);
        gatheringValidator.validateLeader(gatheringId, userId);

        if (gathering.getGatheringStatus().equals(GatheringStatus.INACTIVE)) {
            throw new GatheringException(GatheringErrorCode.ALREADY_INACTIVE);
        }
        gathering.deleteGathering();
    }

    // 모임원 탈퇴 - 리더만 가능
    @Transactional
    public void removeMember(Long gatheringId, Long targetUserId) {
        Long requestId = SecurityUtil.getCurrentUserId();

        // 모임 존재 여부
        gatheringValidator.validateAndGetGathering(gatheringId);

        // 권한이 리더인지 검증
        gatheringValidator.validateLeader(gatheringId, requestId);

        // 강퇴 대상 멤버 조회 & 존재여부
        GatheringMember targetMember = gatheringValidator.validateAndGetMember(gatheringId, targetUserId);

        // 강퇴 대상이 리더인지 확인
        if (targetMember.getRole() == GatheringRole.LEADER) {
            throw new GatheringException(GatheringErrorCode.CANNOT_REMOVE_LEADER);
        }

        targetMember.remove();
    }

    public PageResponse<GatheringBookListResponse> getGatheringBooks(Long gatheringId, int page, int size) {
        Long userId = SecurityUtil.getCurrentUserId();

        gatheringValidator.validateAndGetGathering(gatheringId);
        gatheringValidator.validateMembership(gatheringId, userId);

        Pageable pageable = PageRequest.of(page, size);
        Page<GatheringBook> gatheringBookPage = gatheringBookRepository.findGatheringBooks(gatheringId, pageable);

        if (gatheringBookPage.isEmpty()) {
            return PageResponse.of(List.of(), 0, page, size);
        }

        List<GatheringBook> gatheringBooks = gatheringBookPage.getContent();
        Map<Long, Double> ratingMap = getBookRatingMap(gatheringId, gatheringBooks);

        List<GatheringBookListResponse> responses = gatheringBooks.stream()
                .map(gb -> GatheringBookListResponse.from(gb, ratingMap.get(gb.getBook().getId())))
                .toList();

        return PageResponse.of(responses, gatheringBookPage.getTotalElements(), page, size);
    }

    private Map<Long, Double> getBookRatingMap(Long gatheringId, List<GatheringBook> gatheringBooks) {
        List<Long> meetingMemberIds = meetingMemberRepository.findByGatheringId(gatheringId);

        if (meetingMemberIds.isEmpty()) {
            return Map.of();
        }

        List<Long> bookIds = gatheringBooks.stream()
                .map(gb -> gb.getBook().getId())
                .toList();

        return bookReviewRepository.findMeetingBookReviews(bookIds, meetingMemberIds).stream()
                .collect(Collectors.toMap(
                        BookRatingAverage::bookId,
                        BookRatingAverage::averageRating
                ));
    }

    /**
     * 중복되지 않는 초대 코드를 생성합니다.
     * 최대 10번 재시도하며, 실패 시 예외를 발생시킵니다.
     */
    private String generateUniqueInvitationCode() {
        int maxRetries = 10;

        for (int i = 0; i < maxRetries; i++) {
            String code = InvitationCodeGenerator.generate();
            if (!gatheringRepository.existsByInvitationLink(code)) {
                return code;
            }
        }

        throw new GatheringException(GatheringErrorCode.INVITATION_CODE_GENERATION_FAILED);
    }

    @Transactional
    public void updateFavorite(Long gatheringId) {
        Long userId = SecurityUtil.getCurrentUserId();
        GatheringMember member = gatheringValidator.validateAndGetMember(gatheringId, userId);

        // 즐겨찾기 추가시 4개 제한 검증
        if (!member.getIsFavorite()) {
            gatheringValidator.validateFavoriteLimit(userId);
        }

        member.updateFavorite();
    }

    /**
     * 모임 멤버 관리 목록을 상태별로 조회합니다. (커시 기반 무한 스크롤)
     */
    public CursorResponse<GatheringMemberResponse, GatheringMemberCursor> getGatheringMembers(
            Long gatheringId,
            GatheringMemberStatus status,
            int pageSize,
            Long cursorId
    ) {
        Long userId = SecurityUtil.getCurrentUserId();

        // 모임 존재 여부 및 리더 권한 검증
        gatheringValidator.validateAndGetGathering(gatheringId);
        gatheringValidator.validateLeader(gatheringId, userId);

        Pageable pageable = PageRequest.of(0, pageSize +1);

        List<GatheringMember> members;
        Integer totalCount = null;

        if (cursorId == null) {
            // 첫 페이지
            members = gatheringMemberRepository.findMembersByStatusFirstPage(gatheringId, status, pageable);
            totalCount = gatheringMemberRepository.countMembersByStatus(gatheringId, status);
        } else {
            // 다음 페이지
            members = gatheringMemberRepository.findMembersByStatusAfterCursor(gatheringId, status, cursorId, pageable);
        }

        boolean hasNext = members.size() > pageSize;
        List<GatheringMember> pageMembers = hasNext ? members.subList(0, pageSize) : members;

        List<GatheringMemberResponse> items = pageMembers.stream()
                .map(member -> {
                    String presignedUrl = storageService.getPresignedProfileImage(
                            member.getUser().getProfileImageUrl()
                    );
                    return GatheringMemberResponse.from(member, presignedUrl);
                })
                .toList();

        GatheringMember lastMember = pageMembers.isEmpty() ? null : pageMembers.get(pageMembers.size() - 1);
        GatheringMemberCursor nextCursor = hasNext && lastMember != null
                ? GatheringMemberCursor.from(lastMember)
                : null;

        return CursorResponse.of(items, pageSize, hasNext, nextCursor, totalCount);
    }

    /**
     * 공통 메서드
     * 모임 멤버를 추가합니다.
     */

    private GatheringMember saveGatheringMember(Gathering gathering, User user, GatheringRole role, GatheringMemberStatus status, LocalDateTime joinedAt) {

        GatheringMember gatheringMember = GatheringMember.of(gathering, user, role, status, joinedAt);
        return gatheringMemberRepository.save(gatheringMember);
    }

    /**
     * 공통 메서드
     * ACTIVE 상태인 모임 멤버 수를 조회합니다.
     */
    private int getActiveMemberCount(Long gatheringId) {
        return gatheringMemberRepository.countActiveMembersByStatus(gatheringId);
    }

    /**
     * 공통 메서드
     * 완료된 모임(미팅) 수를 조회합니다.
     */
    private int getMeetingCount(Long gatheringId) {
        return meetingRepository.countByGatheringIdAndMeetingStatus(gatheringId, MeetingStatus.DONE);
    }

}
