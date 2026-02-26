package com.dokdok.retrospective.service;

import com.dokdok.global.util.SecurityUtil;
import com.dokdok.meeting.entity.Meeting;
import com.dokdok.meeting.service.MeetingValidator;
import com.dokdok.retrospective.dto.request.RetrospectiveSummaryUpdateRequest;
import com.dokdok.retrospective.dto.response.RetrospectiveSummaryResponse;
import com.dokdok.retrospective.entity.TopicRetrospectiveSummary;
import com.dokdok.retrospective.exception.RetrospectiveErrorCode;
import com.dokdok.retrospective.exception.RetrospectiveException;
import com.dokdok.retrospective.repository.TopicRetrospectiveSummaryRepository;
import com.dokdok.topic.entity.Topic;
import com.dokdok.topic.entity.TopicStatus;
import com.dokdok.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RetrospectiveSummaryService {

    private final TopicRepository topicRepository;
    private final TopicRetrospectiveSummaryRepository topicRetrospectiveSummaryRepository;
    private final MeetingValidator meetingValidator;
    private final RetrospectiveValidator retrospectiveValidator;

    public RetrospectiveSummaryResponse getRetrospectiveSummary(Long meetingId) {
        Long userId = SecurityUtil.getCurrentUserId();

        Meeting meeting = meetingValidator.findMeetingOrThrow(meetingId);

        // 모임장/약속장만 조회 가능
        retrospectiveValidator.validateSummaryUpdatePermission(meeting.getGathering().getId(), meetingId, userId);

        // 확정된 토픽 조회
        List<Topic> topics = topicRepository.findByMeetingIdAndTopicStatusOrderByConfirmOrderAsc(
                meetingId,
                TopicStatus.CONFIRMED
        );

        // 토픽별 요약 조회
        List<Long> topicIds = topics.stream()
                .map(Topic::getId)
                .toList();

        Map<Long, TopicRetrospectiveSummary> summaryMap = topicRetrospectiveSummaryRepository
                .findAllByTopicIdIn(topicIds)
                .stream()
                .collect(Collectors.toMap(s -> s.getTopic().getId(), s-> s));

        // Response 생성
        List<RetrospectiveSummaryResponse.TopicSummaryResponse> topicResponses = topics.stream()
                .map(topic -> RetrospectiveSummaryResponse.TopicSummaryResponse.from(
                        topic,
                        summaryMap.get(topic.getId())
                ))
                .toList();

        return RetrospectiveSummaryResponse.from(meeting, topicResponses);
    }

    @Transactional
    public RetrospectiveSummaryResponse updateRetrospectiveSummary(
            Long meetingId,
            RetrospectiveSummaryUpdateRequest request
    ) {
        Long userId = SecurityUtil.getCurrentUserId();

        Meeting meeting = meetingValidator.findMeetingOrThrow(meetingId);

        // 권한 검증 (모임장/약속장만 수정 가능)
        retrospectiveValidator.validateSummaryUpdatePermission(
                meeting.getGathering().getId(),
                meetingId,
                userId
        );

        // 각 토픽별 요약 수정
        for (RetrospectiveSummaryUpdateRequest.TopicSummaryUpdateRequest topicRequest : request.topics()) {
            TopicRetrospectiveSummary summary = topicRetrospectiveSummaryRepository
                    .findByTopicId(topicRequest.topicId())
                    .orElseThrow(() -> new RetrospectiveException(RetrospectiveErrorCode.SUMMARY_NOT_FOUND));

            // KeyPointUpdateRequest -> KeyPoint 변환
            List<TopicRetrospectiveSummary.KeyPoint> keyPoints = topicRequest.keyPoints().stream()
                    .map(kp -> new TopicRetrospectiveSummary.KeyPoint(kp.title(), kp.details()))
                    .toList();

            summary.update(topicRequest.summary(), keyPoints);
        }

        // 수정된 결과 반환
        return getRetrospectiveSummary(meetingId);
    }

    @Transactional
    public RetrospectiveSummaryResponse publishRetrospective(Long meetingId) {
        Long userId = SecurityUtil.getCurrentUserId();

        Meeting meeting = meetingValidator.findMeetingOrThrow(meetingId);

        // 권한 검증 (모임장/약속장만 생성 가능)
        retrospectiveValidator.validateSummaryUpdatePermission(
                meeting.getGathering().getId(),
                meetingId,
                userId
        );

        // 이미 생성된 경우 예외
        if (meeting.isRetrospectivePublished()) {
            throw new RetrospectiveException(RetrospectiveErrorCode.RETROSPECTIVE_ALREADY_PUBLISHED);
        }

        // 약속 회고 생성 (퍼블리시)
        meeting.publishRetrospective();

        return getRetrospectiveSummary(meetingId);
    }
}
