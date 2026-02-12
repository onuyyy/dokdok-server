package com.dokdok.topic.service;

import com.dokdok.book.entity.BookReview;
import com.dokdok.book.entity.BookReviewKeyword;
import com.dokdok.book.repository.BookReviewKeywordRepository;
import com.dokdok.book.repository.BookReviewRepository;
import com.dokdok.gathering.entity.GatheringMember;
import com.dokdok.gathering.entity.GatheringRole;
import com.dokdok.gathering.repository.GatheringMemberRepository;
import com.dokdok.gathering.service.GatheringValidator;
import com.dokdok.global.util.SecurityUtil;
import com.dokdok.meeting.entity.MeetingMember;
import com.dokdok.meeting.entity.MeetingMemberRole;
import com.dokdok.meeting.repository.MeetingMemberRepository;
import com.dokdok.meeting.service.MeetingValidator;
import com.dokdok.storage.service.StorageService;
import com.dokdok.topic.dto.response.PreOpinionResponse;
import com.dokdok.topic.dto.response.PreOpinionResponse.BookReviewInfo;
import com.dokdok.topic.entity.TopicAnswer;
import com.dokdok.topic.repository.TopicAnswerRepository;
import com.dokdok.topic.repository.TopicRepository;
import com.dokdok.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PreOpinionService {

    private final GatheringMemberRepository gatheringMemberRepository;
    private final GatheringValidator gatheringValidator;
    private final MeetingValidator meetingValidator;
    private final TopicValidator topicValidator;
    private final TopicRepository topicRepository;
    private final MeetingMemberRepository meetingMemberRepository;
    private final TopicAnswerRepository topicAnswerRepository;
    private final BookReviewRepository bookReviewRepository;
    private final BookReviewKeywordRepository bookReviewKeywordRepository;
    private final StorageService storageService;

    @Transactional(readOnly = true)
    public PreOpinionResponse findPreOpinions(Long gatheringId, Long meetingId) {
        Long userId = SecurityUtil.getCurrentUserId();
        validateAccess(gatheringId, meetingId, userId);

        List<PreOpinionResponse.TopicInfo> topicInfos = buildTopicInfos(meetingId);
        List<MeetingMember> meetingMembers = meetingMemberRepository.findAllByMeetingId(meetingId);

        List<PreOpinionResponse.MemberPreOpinion> preOpinionData = buildPreOpinionData(gatheringId, meetingId, meetingMembers);

        return new PreOpinionResponse(topicInfos, preOpinionData);
    }

    @Transactional
    public void deleteMyAnswer(
            Long gatheringId,
            Long meetingId
    ) {
        Long userId = SecurityUtil.getCurrentUserId();

        gatheringValidator.validateGathering(gatheringId);
        meetingValidator.validateMeetingInGathering(meetingId, gatheringId);
        meetingValidator.validateMeetingMember(meetingId, userId);

        List<TopicAnswer> topicAnswers = topicValidator.getTopicAnswers(meetingId, userId);

        topicAnswers.forEach(TopicAnswer::softDelete);
    }

    private void validateAccess(Long gatheringId, Long meetingId, Long userId) {
        gatheringValidator.validateGathering(gatheringId);
        meetingValidator.validateMeetingInGathering(meetingId, gatheringId);
        meetingValidator.validateMeetingMember(meetingId, userId);
        topicValidator.validateUserHasWrittenAnswer(meetingId, userId);
    }

    private List<PreOpinionResponse.TopicInfo> buildTopicInfos(Long meetingId) {
        return topicRepository.findConfirmedTopics(meetingId).stream()
                .map(PreOpinionResponse.TopicInfo::from)
                .toList();
    }

    private List<PreOpinionResponse.MemberPreOpinion> buildPreOpinionData(Long gatheringId, Long meetingId, List<MeetingMember> meetingMembers) {
        PreOpinionMaps maps = fetchPreOpinionMaps(gatheringId, meetingId, meetingMembers);
        return assembleMembers(meetingMembers, maps);
    }

    private record PreOpinionMaps(
            Map<Long, GatheringRole> gatheringRoleByUserId,
            Map<Long, BookReview> bookReviewByUserId,
            Map<Long, List<BookReviewKeyword>> keywordsByReviewId,
            Map<Long, List<PreOpinionResponse.TopicOpinion>> topicAnswersByUserId,
            Map<Long, LocalDateTime> earliestAnswerByUserId
    ) {}

    private PreOpinionMaps fetchPreOpinionMaps(Long gatheringId, Long meetingId, List<MeetingMember> meetingMembers) {
        List<Long> userIds = meetingMembers.stream()
                .map(mm -> mm.getUser().getId())
                .toList();

        Map<Long, GatheringRole> gatheringRoleByUserId = gatheringMemberRepository
                .findAllMembersByGatheringId(gatheringId).stream()
                .collect(Collectors.toMap(
                        gm -> gm.getUser().getId(),
                        GatheringMember::getRole,
                        (existing, replacement) -> existing
                ));

        Map<Long, BookReview> bookReviewByUserId = bookReviewRepository.findByUserIdIn(userIds, meetingId).stream()
                .collect(Collectors.toMap(
                        br -> br.getUser().getId(),
                        br -> br,
                        (existing, replacement) -> existing
                ));

        List<Long> bookReviewIds = bookReviewByUserId.values().stream()
                .map(BookReview::getId)
                .toList();
        Map<Long, List<BookReviewKeyword>> keywordsByReviewId = bookReviewKeywordRepository
                .findByBookReviewIds(bookReviewIds).stream()
                .collect(Collectors.groupingBy(k -> k.getBookReview().getId()));

        List<TopicAnswer> allTopicAnswers = topicAnswerRepository.findByMeetingId(meetingId);

        Map<Long, List<PreOpinionResponse.TopicOpinion>> topicAnswersByUserId =
                allTopicAnswers.stream()
                        .collect(Collectors.groupingBy(
                                ta -> ta.getUser().getId(),
                                Collectors.mapping(
                                        PreOpinionResponse.TopicOpinion::of,
                                        Collectors.toList()
                                )
                        ));

        Map<Long, LocalDateTime> earliestAnswerByUserId = allTopicAnswers.stream()
                .collect(Collectors.toMap(
                        ta -> ta.getUser().getId(),
                        TopicAnswer::getCreatedAt,
                        (a, b) -> a.isBefore(b) ? a : b
                ));

        return new PreOpinionMaps(
                gatheringRoleByUserId,
                bookReviewByUserId,
                keywordsByReviewId,
                topicAnswersByUserId,
                earliestAnswerByUserId
        );
    }

    private List<PreOpinionResponse.MemberPreOpinion> assembleMembers(List<MeetingMember> meetingMembers, PreOpinionMaps maps) {
        return meetingMembers.stream()
                .sorted(Comparator.comparing(
                        (MeetingMember mm) -> maps.earliestAnswerByUserId().getOrDefault(
                                mm.getUser().getId(), LocalDateTime.MAX)
                ))
                .map(mm -> toMemberPreOpinion(mm, maps))
                .toList();
    }

    private PreOpinionResponse.MemberPreOpinion toMemberPreOpinion(MeetingMember mm, PreOpinionMaps maps) {
        User user = mm.getUser();
        Long memberId = user.getId();

        String presignedUrl = storageService.getPresignedProfileImage(user.getProfileImageUrl());
        String role = resolveRole(mm, maps.gatheringRoleByUserId());

        PreOpinionResponse.MemberInfo memberInfo
                = PreOpinionResponse.MemberInfo.of(user.getId(), user.getNickname(), presignedUrl, role);

        BookReview review = maps.bookReviewByUserId().get(memberId);
        BookReviewInfo bookReviewInfo = review != null
                ? toBookReviewInfo(review, maps.keywordsByReviewId())
                : null;

        List<PreOpinionResponse.TopicOpinion> topicAnswers = maps.topicAnswersByUserId().getOrDefault(memberId, List.of());

        return new PreOpinionResponse.MemberPreOpinion(memberInfo, bookReviewInfo, topicAnswers, maps.topicAnswersByUserId().containsKey(memberId));
    }

    /**
     * 모임장 / 약속장 구별을 위한 메서드
     */
    private String resolveRole(MeetingMember mm, Map<Long, GatheringRole> gatheringRoleByUserId) {
        Long userId = mm.getUser().getId();
        GatheringRole gatheringRole = gatheringRoleByUserId.get(userId);

        if (gatheringRole == GatheringRole.LEADER) {
            return "GATHERING_LEADER";
        }
        if (mm.getMeetingRole() == MeetingMemberRole.LEADER) {
            return "MEETING_LEADER";
        }
        return "MEMBER";
    }

    /**
     * 멤버들의 책 리뷰 조회
     * - 사전의견을 발행하지 않은 사용자는 책 평가도 반환하지 않음
     */
    private BookReviewInfo toBookReviewInfo(
            BookReview bookReview,
            Map<Long, List<BookReviewKeyword>> keywordsByReviewId
    ) {
        List<BookReviewKeyword> reviewKeywords =
                keywordsByReviewId.getOrDefault(bookReview.getId(), List.of());

        List<PreOpinionResponse.KeywordInfo> keywordInfos = reviewKeywords.stream()
                .map(rk -> PreOpinionResponse.KeywordInfo.of(
                        rk.getKeyword().getId(),
                        rk.getKeyword().getKeywordName(),
                        rk.getKeyword().getKeywordType()
                ))
                .toList();

        return BookReviewInfo.of(
                bookReview.getRating(),
                keywordInfos
        );
    }
}