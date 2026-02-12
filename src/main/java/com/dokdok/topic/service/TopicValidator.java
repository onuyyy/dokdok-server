package com.dokdok.topic.service;

import com.dokdok.book.exception.BookErrorCode;
import com.dokdok.book.exception.BookException;
import com.dokdok.topic.entity.TopicAnswer;
import com.dokdok.topic.entity.Topic;
import com.dokdok.topic.exception.TopicErrorCode;
import com.dokdok.topic.exception.TopicException;
import com.dokdok.topic.repository.TopicAnswerRepository;
import com.dokdok.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@RequiredArgsConstructor
public class TopicValidator {

    private final TopicRepository topicRepository;
    private final TopicAnswerRepository topicAnswerRepository;

    public Topic getTopic(Long topicId) {
        return topicRepository.findDetailById(topicId)
                .orElseThrow(() -> new TopicException(TopicErrorCode.TOPIC_NOT_FOUND));
    }

    public List<Topic> getConfirmedTopics(Long meetingId) {
        List<Topic> topics = topicRepository.findTopicsInfoByMeetingId(meetingId);

        if (topics.isEmpty()) {
            throw new TopicException(TopicErrorCode.TOPIC_NOT_FOUND);
        }

        return topics;
    }

    /**
     * 해당 약속에 속한 주제인지 검증하고 Topic을 반환한다.
     */
    public Topic getTopicInMeeting(Long topicId, Long meetingId) {
        Topic topic = topicRepository.findDetailById(topicId)
                .orElseThrow(() -> new TopicException(TopicErrorCode.TOPIC_NOT_FOUND));

        if (topic.isDeleted()) {
            throw new TopicException(TopicErrorCode.TOPIC_ALREADY_DELETED);
        }

        if (!topic.getMeeting().getId().equals(meetingId)) {
            throw new TopicException(TopicErrorCode.TOPIC_NOT_IN_MEETING);
        }

        return topic;
    }

    /**
     * 해당 약속에 속한 주제인지 검증한다.
     */
    public void validateTopicInMeeting(Long topicId, Long meetingId) {
        getTopicInMeeting(topicId, meetingId);
    }

    public TopicAnswer getTopicAnswer(Long topicId, Long userId) {
        return topicAnswerRepository.findByTopicIdAndUserId(topicId, userId)
                .orElseThrow(() -> new TopicException(TopicErrorCode.TOPIC_ANSWER_NOT_FOUND));
    }

    public List<TopicAnswer> getTopicAnswers(Long meetingId, Long userId) {
        List<TopicAnswer> topicAnswers = topicAnswerRepository.findByTopicAnswers(meetingId, userId);

        if(topicAnswers.isEmpty()) {
            throw new TopicException(TopicErrorCode.TOPIC_ANSWER_NOT_FOUND);
        }

        boolean allDeleted = topicAnswers.stream().allMatch(TopicAnswer::isDeleted);

        if (allDeleted) {
            throw new TopicException(TopicErrorCode.TOPIC_ANSWER_ALREADY_DELETED);
        }

        return topicAnswers;
    }

    /**
     * 주제에 대한 삭제 권한 검증한다
     * 권한 소유 : 모임장, 약속장, 주제 제안자
     */
    public Topic getDeletableTopic(
            Long topicId,
            Long userId
    ) {

        return topicRepository.findTopicWithDeletePermission(topicId, userId)
                .orElseThrow(() -> new TopicException(TopicErrorCode.TOPIC_USER_CANNOT_DELETE));
    }

    /**
     * 사전 의견을 발행한 사용자인지 검증한다.
     */
    public void validateUserHasWrittenAnswer(Long meetingId, Long userId) {

        boolean exists = topicAnswerRepository.existsByMeetingIdAndUserId(meetingId, userId);

        if(!exists) {
            throw new BookException(BookErrorCode.BOOK_REVIEW_ACCESS_DENIED_NOT_WRITTEN);
        }
    }
}
