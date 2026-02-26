package com.dokdok.topic.service;

import com.dokdok.gathering.service.GatheringValidator;
import com.dokdok.global.util.SecurityUtil;
import com.dokdok.meeting.entity.Meeting;
import com.dokdok.meeting.entity.MeetingMember;
import com.dokdok.meeting.entity.MeetingStatus;
import com.dokdok.global.response.CursorResponse;
import com.dokdok.meeting.service.MeetingValidator;
import com.dokdok.topic.dto.request.ConfirmTopicsRequest;
import com.dokdok.topic.dto.request.SuggestTopicRequest;
import com.dokdok.topic.dto.response.ConfirmTopicsResponse;
import com.dokdok.topic.dto.response.ConfirmedTopicsCursor;
import com.dokdok.topic.dto.response.ConfirmedTopicsResponse;
import com.dokdok.topic.dto.response.SuggestTopicResponse;
import com.dokdok.topic.dto.response.TopicLikeResponse;
import com.dokdok.topic.dto.response.TopicsWithActionsResponse;
import com.dokdok.topic.entity.Topic;
import com.dokdok.topic.entity.TopicLike;
import com.dokdok.topic.entity.TopicMessage;
import com.dokdok.topic.entity.TopicStatus;
import com.dokdok.topic.entity.TopicType;
import com.dokdok.topic.exception.TopicErrorCode;
import com.dokdok.topic.exception.TopicException;
import com.dokdok.topic.repository.TopicAnswerRepository;
import com.dokdok.topic.repository.TopicLikeRepository;
import com.dokdok.topic.repository.TopicRepository;
import com.dokdok.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@RequestMapping("/api/gatherings/{gatheringId}/meetings/{meetingId}")
public class TopicService {

    private final TopicRepository topicRepository;
    private final TopicLikeRepository topicLikeRepository;
    private final TopicAnswerRepository topicAnswerRepository;
    private final GatheringValidator gatheringValidator;
    private final MeetingValidator meetingValidator;
    private final TopicValidator topicValidator;

    @Transactional
    public void createDefaultTopic(Meeting meeting) {
        if (meeting == null || meeting.getId() == null) {
            return;
        }
        if (topicRepository.countByMeetingIdAndDeletedAtIsNull(meeting.getId()) > 0) {
            return;
        }

        User leader = meeting.getMeetingLeader();
        if (leader == null) {
            return;
        }
        TopicType type = TopicType.FREE;

        Topic topic = Topic.create(
                meeting,
                leader,
                type.getDisplayName(),
                type.getDescription(),
                type
        );

        topicRepository.save(topic);
    }

    @Transactional
    public SuggestTopicResponse createTopic(
            Long gatheringId,
            Long meetingId,
            SuggestTopicRequest request
    ) {
        Long userId = SecurityUtil.getCurrentUserId();

        gatheringValidator.validateGathering(gatheringId);
        meetingValidator.validateMeetingInGathering(meetingId, gatheringId);
        meetingValidator.validateMeetingStatus(meetingId);

        MeetingMember meetingMember = meetingValidator.getMeetingMember(meetingId, userId);

        Meeting meeting = meetingMember.getMeeting();
        User user = meetingMember.getUser();

        Topic topic = Topic.create(
                meeting,
                user,
                request.title(),
                request.description(),
                request.topicType()
        );

        topicRepository.save(topic);

        return SuggestTopicResponse.from(topic, user);
    }

    @Transactional(readOnly = true)
    public TopicsWithActionsResponse getTopics(
            Long gatheringId,
            Long meetingId,
            int pageSize,
            Integer cursorLikeCount,
            Long cursorTopicId
    ) {
        Long userId = SecurityUtil.getCurrentUserId();

        gatheringValidator.validateGathering(gatheringId);
        meetingValidator.validateMeetingInGathering(meetingId, gatheringId);

        boolean canConfirm = topicRepository.canConfirmTopic(meetingId, userId);
        boolean canSuggest = topicRepository.canSuggestTopic(meetingId, userId);

        TopicsWithActionsResponse.Actions actions = TopicsWithActionsResponse.Actions.of(canConfirm, canSuggest);

        // pageSize + 1개를 조회하여 다음 페이지 존재 여부 판단
        PageRequest pageable = PageRequest.of(0, pageSize + 1);

        List<Topic> topics;
        boolean hasCursor = cursorLikeCount != null && cursorTopicId != null;

        topics = hasCursor
                ? topicRepository.findTopicsAfterCursor(meetingId, cursorLikeCount, cursorTopicId, pageable)
                : topicRepository.findTopicsFirstPage(meetingId, pageable);

        // hasNext 판단: pageSize + 1개를 조회했으므로 초과 시 다음 페이지 존재
        boolean hasNext = topics.size() > pageSize;

        // 실제 반환할 목록은 pageSize만큼만
        if (hasNext) {
            topics = topics.subList(0, pageSize);
        }

        Set<Long> deletableTopicIds = Set.of();
        Set<Long> likedTopicIds = Set.of();

        if (userId != null && !topics.isEmpty()) {
            List<Long> topicIds = topics.stream()
                    .map(Topic::getId)
                    .toList();
            deletableTopicIds = topicRepository.findDeletableTopicIds(topicIds, userId);
            likedTopicIds = topicLikeRepository.findLikedTopicIds(topicIds, userId);
        }

        Long totalCount = null;
        if (!hasCursor) {
            totalCount = topicRepository.countByMeetingIdAndDeletedAtIsNull(meetingId);
        }

        return TopicsWithActionsResponse.from(topics, pageSize, hasNext, deletableTopicIds, likedTopicIds, actions, totalCount);
    }

    @Transactional
    public ConfirmTopicsResponse confirmTopics(
            Long gatheringId,
            Long meetingId,
            ConfirmTopicsRequest request
    ) {
        Long userId = SecurityUtil.getCurrentUserId();

        gatheringValidator.validateMembership(gatheringId, userId);
        meetingValidator.validateMeetingInGathering(meetingId, gatheringId);

        List<Long> topicIds = request.topicIds();

        List<Topic> topics = topicRepository.findAllByIdInAndMeetingId(topicIds, meetingId);
        if (topics.size() != topicIds.size()) {
            throw new TopicException(TopicErrorCode.TOPIC_NOT_FOUND);
        }

        Map<Long, Topic> topicMap =
                topics.stream()
                        .collect(Collectors.toMap(Topic::getId, Function.identity()));

        List<ConfirmTopicsResponse.ConfirmedTopicOrder> confirmedTopics = new ArrayList<>(topicIds.size());
        for (int i = 0; i < topicIds.size(); i++) {
            Long topicId = topicIds.get(i);
            Topic topic = topicMap.get(topicId);
            if (topic == null) {
                throw new TopicException(TopicErrorCode.TOPIC_NOT_FOUND);
            }
            topic.updateStatus(TopicStatus.CONFIRMED);
            topic.updateConfirmOrder(i + 1);
            confirmedTopics.add(ConfirmTopicsResponse.ConfirmedTopicOrder.of(topicId, i + 1));
        }

        return ConfirmTopicsResponse.from(meetingId, confirmedTopics);
    }

    @Transactional(readOnly = true)
    public ConfirmedTopicsResponse getConfirmedTopics(
            Long gatheringId,
            Long meetingId,
            int pageSize,
            Integer cursorConfirmOrder,
            Long cursorTopicId
    ) {
        Long userId = SecurityUtil.getCurrentUserId();

        gatheringValidator.validateMembership(gatheringId, userId);
        meetingValidator.validateMeetingInGathering(meetingId, gatheringId);

        PageRequest pageable = PageRequest.of(0, pageSize + 1);
        boolean hasCursor = cursorConfirmOrder != null && cursorTopicId != null;

        List<Topic> topics = hasCursor
                ? topicRepository.findConfirmedTopicsAfterCursor(meetingId, cursorConfirmOrder, cursorTopicId, pageable)
                : topicRepository.findConfirmedTopicsFirstPage(meetingId, pageable);

        boolean hasNext = topics.size() > pageSize;
        if (hasNext) {
            topics = topics.subList(0, pageSize);
        }

        List<ConfirmedTopicsResponse.ConfirmedTopicDto> topicDtos = topics.stream()
                .map(ConfirmedTopicsResponse.ConfirmedTopicDto::from)
                .toList();

        ConfirmedTopicsCursor nextCursor = null;
        if (hasNext && !topics.isEmpty()) {
            Topic lastTopic = topics.get(topics.size() - 1);
            nextCursor = ConfirmedTopicsCursor.from(lastTopic);
        }

        Integer totalCount = null;
        if (!hasCursor) {
            totalCount = (int) topicRepository.countByMeetingIdAndTopicStatusAndDeletedAtIsNull(
                    meetingId, TopicStatus.CONFIRMED
            );
        }

        boolean hasSubmitted = topicAnswerRepository.existsByMeetingIdAndUserId(meetingId, userId);
        boolean isMeetingConfirmed = meetingValidator.findMeetingOrThrow(meetingId).getMeetingStatus()
                == MeetingStatus.CONFIRMED;
        Integer confirmedTopicsCount = totalCount;
        if (confirmedTopicsCount == null) {
            confirmedTopicsCount = (int) topicRepository.countByMeetingIdAndTopicStatusAndDeletedAtIsNull(
                    meetingId, TopicStatus.CONFIRMED
            );
        }
        boolean hasConfirmedTopics = confirmedTopicsCount > 0;

        ConfirmedTopicsResponse.Actions actions = ConfirmedTopicsResponse.Actions.of(
                hasSubmitted,
                isMeetingConfirmed && hasConfirmedTopics && !hasSubmitted
        );

        CursorResponse<ConfirmedTopicsResponse.ConfirmedTopicDto, ConfirmedTopicsCursor> page =
                CursorResponse.of(topicDtos, pageSize, hasNext, nextCursor, totalCount);

        return ConfirmedTopicsResponse.from(page, actions);
    }

    @Transactional
    public void deleteTopic(
            Long gatheringId,
            Long meetingId,
            Long topicId
    ) {
        Long userId = SecurityUtil.getCurrentUserId();

        gatheringValidator.validateGathering(gatheringId);
        meetingValidator.validateMeetingInGathering(meetingId, gatheringId);
        meetingValidator.validateMeetingMember(meetingId, userId);
        topicValidator.validateTopicInMeeting(topicId, meetingId);

        Topic topic = topicValidator.getDeletableTopic(topicId, userId);

        topic.softDelete();
    }

    @Transactional
    public TopicLikeResponse toggleTopicLike(
            Long gatheringId,
            Long meetingId,
            Long topicId
    ) {
        User user = SecurityUtil.getCurrentUserEntity();

        gatheringValidator.validateGathering(gatheringId);
        meetingValidator.validateMeetingInGathering(meetingId, gatheringId);
        meetingValidator.validateMeetingMember(meetingId, user.getId());

        Topic topic = topicValidator.getTopicInMeeting(topicId, meetingId);

        boolean exists = topicLikeRepository.existsByTopicId(topicId);

        TopicMessage message;
        int newCount;

        if (exists) {
            topicLikeRepository.deleteByTopicIdAndUserId(topicId, user.getId());
            topicRepository.decreaseLikeCount(topicId);
            message = TopicMessage.LIKE_CANCEL;
            newCount = topic.getLikeCount() - 1;
        } else {
            topicLikeRepository.save(TopicLike.create(topic, user));
            topicRepository.increaseLikeCount(topicId);
            message = TopicMessage.LIKE_SUCCESS;
            newCount = topic.getLikeCount() + 1;
        }

        return TopicLikeResponse.from(topicId, message, newCount);
    }
}
