package com.dokdok.topic.service;

import com.dokdok.book.dto.request.BookReviewRequest;
import com.dokdok.book.dto.response.BookReviewResponse;
import com.dokdok.book.exception.BookErrorCode;
import com.dokdok.book.exception.BookException;
import com.dokdok.book.repository.BookReviewRepository;
import com.dokdok.book.service.BookReviewService;
import com.dokdok.gathering.service.GatheringValidator;
import com.dokdok.global.util.SecurityUtil;
import com.dokdok.meeting.service.MeetingValidator;
import com.dokdok.topic.dto.request.TopicAnswerBulkSaveRequest;
import com.dokdok.topic.dto.request.TopicAnswerBulkSubmitRequest;
import com.dokdok.topic.dto.response.PreOpinionSaveResponse;
import com.dokdok.topic.dto.response.PreOpinionSubmitResponse;
import com.dokdok.topic.dto.response.TopicAnswerDetailResponse;
import com.dokdok.topic.dto.response.TopicAnswerResponse;
import com.dokdok.topic.dto.response.TopicAnswerSubmitResponse;
import com.dokdok.topic.entity.Topic;
import com.dokdok.topic.entity.TopicAnswer;
import com.dokdok.topic.exception.TopicErrorCode;
import com.dokdok.topic.exception.TopicException;
import com.dokdok.topic.repository.TopicAnswerRepository;
import com.dokdok.topic.repository.TopicRepository;
import com.dokdok.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TopicAnswerService {

    private final TopicAnswerRepository topicAnswerRepository;
    private final TopicRepository topicRepository;
    private final BookReviewRepository bookReviewRepository;
    private final BookReviewService bookReviewService;
    private final GatheringValidator gatheringValidator;
    private final MeetingValidator meetingValidator;

    @Transactional
    public PreOpinionSaveResponse createAnswer(
            Long gatheringId,
            Long meetingId,
            TopicAnswerBulkSaveRequest request
    ) {
        return saveAnswersBulk(gatheringId, meetingId, request);
    }

    @Transactional(readOnly = true)
    public TopicAnswerDetailResponse getMyAnswer(
            Long gatheringId,
            Long meetingId
    ) {
        Long userId = SecurityUtil.getCurrentUserId();

        gatheringValidator.validateMembership(gatheringId, userId);
        meetingValidator.validateMeetingInGathering(meetingId, gatheringId);
        meetingValidator.validateMeetingMember(meetingId, userId);

        var meeting = meetingValidator.findMeetingOrThrow(meetingId);

        List<Topic> topics = topicRepository.findTopicsInfoByMeetingId(meetingId);
        Map<Long, TopicAnswer> answersByTopicId = topicAnswerRepository.findByMeetingIdUserId(meetingId, userId)
                .stream()
                .collect(Collectors.toMap(
                        answer -> answer.getTopic().getId(),
                        answer -> answer
                ));

        LocalDateTime latestUpdatedAt = answersByTopicId.values().stream()
                .map(TopicAnswer::getUpdatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        List<TopicAnswerDetailResponse.PreOpinionTopic> topicInfos = topics.stream()
                .map(topic -> TopicAnswerDetailResponse.PreOpinionTopic.of(
                        topic,
                        answersByTopicId.get(topic.getId())
                ))
                .toList();

        TopicAnswerDetailResponse.BookInfo bookInfo = TopicAnswerDetailResponse.BookInfo.from(meeting.getBook());
        BookReviewResponse reviewResponse = null;
        if (meeting.getBook() != null) {
            Long bookId = meeting.getBook().getId();
            reviewResponse = bookReviewRepository.findByBookIdAndUserId(bookId, userId)
                    .map(BookReviewResponse::from)
                    .orElse(null);
        }
        TopicAnswerDetailResponse.PreOpinion preOpinion =
                new TopicAnswerDetailResponse.PreOpinion(latestUpdatedAt, topicInfos);

        return TopicAnswerDetailResponse.of(bookInfo, reviewResponse, preOpinion);
    }

    @Transactional
    public PreOpinionSaveResponse updateMyAnswer(
            Long gatheringId,
            Long meetingId,
            TopicAnswerBulkSaveRequest request
    ) {
        return saveAnswersBulk(gatheringId, meetingId, request);
    }

    @Transactional
    public PreOpinionSubmitResponse submitMyAnswer(
            Long gatheringId,
            Long meetingId,
            TopicAnswerBulkSubmitRequest request
    ) {
        return submitAnswersBulk(gatheringId, meetingId, request);
    }

    private PreOpinionSaveResponse saveAnswersBulk(
            Long gatheringId,
            Long meetingId,
            TopicAnswerBulkSaveRequest request
    ) {
        Long userId = SecurityUtil.getCurrentUserId();

        gatheringValidator.validateMembership(gatheringId, userId);
        meetingValidator.validateMeetingInGathering(meetingId, gatheringId);
        meetingValidator.validateMeetingMember(meetingId, userId);

        Map<Long, String> contentsByTopicId = new LinkedHashMap<>();
        for (TopicAnswerBulkSaveRequest.AnswerItem item : request.answers()) {
            contentsByTopicId.put(item.topicId(), item.content());
        }

        List<Long> topicIds = List.copyOf(contentsByTopicId.keySet());
        List<Topic> topics = topicRepository.findAllByIdInAndMeetingId(topicIds, meetingId);
        if (topics.size() != topicIds.size()) {
            throw new TopicException(TopicErrorCode.TOPIC_NOT_FOUND);
        }

        Map<Long, Topic> topicsById = topics.stream()
                .collect(Collectors.toMap(Topic::getId, topic -> topic));

        Map<Long, TopicAnswer> existingAnswers = topicAnswerRepository.findByMeetingIdUserId(meetingId, userId)
                .stream()
                .collect(Collectors.toMap(answer -> answer.getTopic().getId(), answer -> answer));

        User user = SecurityUtil.getCurrentUserEntity();

        BookReviewResponse reviewResponse = upsertReview(meetingId, request.review());

        List<TopicAnswerResponse> responses = topicIds.stream()
                .map(topicId -> {
                    String content = contentsByTopicId.get(topicId);
                    TopicAnswer answer = existingAnswers.get(topicId);
                    if (answer != null) {
                        if (Boolean.TRUE.equals(answer.getIsSubmitted())) {
                            throw new TopicException(TopicErrorCode.TOPIC_ANSWER_ALREADY_SUBMITTED);
                        }
                        answer.updateContent(content);
                        return TopicAnswerResponse.from(answer);
                    }

                    Topic topic = topicsById.get(topicId);
                    TopicAnswer saved = topicAnswerRepository.save(TopicAnswer.create(topic, user, content));
                    return TopicAnswerResponse.from(saved);
                })
                .toList();

        return PreOpinionSaveResponse.of(reviewResponse, responses);
    }

    private PreOpinionSubmitResponse submitAnswersBulk(
            Long gatheringId,
            Long meetingId,
            TopicAnswerBulkSubmitRequest request
    ) {
        Long userId = SecurityUtil.getCurrentUserId();

        gatheringValidator.validateMembership(gatheringId, userId);
        meetingValidator.validateMeetingInGathering(meetingId, gatheringId);
        meetingValidator.validateMeetingMember(meetingId, userId);

        List<Long> topicIds = request.topicIds();
        List<Topic> topics = topicRepository.findAllByIdInAndMeetingId(topicIds, meetingId);
        if (topics.size() != topicIds.size()) {
            throw new TopicException(TopicErrorCode.TOPIC_NOT_FOUND);
        }

        Map<Long, TopicAnswer> existingAnswers = topicAnswerRepository.findByMeetingIdUserId(meetingId, userId)
                .stream()
                .collect(Collectors.toMap(answer -> answer.getTopic().getId(), answer -> answer));

        for (Long topicId : topicIds) {
            TopicAnswer answer = existingAnswers.get(topicId);
            if (answer == null) {
                throw new TopicException(TopicErrorCode.TOPIC_ANSWER_NOT_FOUND);
            }
            if (Boolean.TRUE.equals(answer.getIsSubmitted())) {
                throw new TopicException(TopicErrorCode.TOPIC_ANSWER_ALREADY_SUBMITTED);
            }
        }

        BookReviewResponse reviewResponse = upsertReview(meetingId, request.review());

        List<TopicAnswerSubmitResponse> responses = topicIds.stream()
                .map(topicId -> {
                    TopicAnswer answer = existingAnswers.get(topicId);
                    answer.submit();
                    return TopicAnswerSubmitResponse.from(answer);
                })
                .toList();

        return PreOpinionSubmitResponse.of(reviewResponse, responses);
    }

    private BookReviewResponse upsertReview(Long meetingId, BookReviewRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        var meeting = meetingValidator.findMeetingOrThrow(meetingId);
        Long bookId = meeting.getBook() != null ? meeting.getBook().getId() : null;
        if (bookId == null) {
            throw new BookException(BookErrorCode.BOOK_NOT_FOUND);
        }

        boolean exists = bookReviewRepository.findByBookIdAndUserId(bookId, userId).isPresent();
        return exists
                ? bookReviewService.updateMyReview(bookId, request)
                : bookReviewService.createReview(bookId, request);
    }

}
