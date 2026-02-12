package com.dokdok.topic.service;

import com.dokdok.ai.client.AiSummaryClient;
import com.dokdok.ai.dto.TopicSummaryRequest;
import com.dokdok.gathering.service.GatheringValidator;
import com.dokdok.global.util.SecurityUtil;
import com.dokdok.meeting.service.MeetingValidator;
import com.dokdok.topic.entity.Topic;
import com.dokdok.topic.entity.TopicAnswer;
import com.dokdok.topic.repository.TopicAnswerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TopicSummaryService {

    private final GatheringValidator gatheringValidator;
    private final MeetingValidator meetingValidator;
    private final TopicValidator topicValidator;
    private final TopicAnswerRepository topicAnswerRepository;
    private final AiSummaryClient aiSummaryClient;

    public String requestTopicSummary(Long gatheringId, Long meetingId, Long topicId) {
        Long userId = SecurityUtil.getCurrentUserId();
        gatheringValidator.validateGathering(gatheringId);
        meetingValidator.validateMeetingInGathering(meetingId, gatheringId);
        meetingValidator.validateMeetingMember(meetingId, userId);

        Topic topic = topicValidator.getTopicInMeeting(topicId, meetingId);
        List<TopicAnswer> answers = topicAnswerRepository.findSubmittedByTopicId(topicId);

        List<TopicSummaryRequest.Answer> answerDtos = answers.stream()
                .map(answer -> new TopicSummaryRequest.Answer(
                        answer.getUser().getId(),
                        answer.getContent()
                ))
                .filter(answer -> answer.content() != null && !answer.content().isBlank())
                .toList();

        TopicSummaryRequest request = new TopicSummaryRequest(
                topic.getId(),
                topic.getTitle(),
                answerDtos
        );

        return aiSummaryClient.requestTopicSummary(request);
    }
}
