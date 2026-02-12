package com.dokdok.book.service;

import com.dokdok.book.dto.request.PersonalReadingRecordCreateRequest;
import com.dokdok.book.dto.response.CursorPageResponse;
import com.dokdok.book.dto.response.PersonalReadingRecordCreateResponse;
import com.dokdok.book.dto.response.PersonalReadingRecordListResponse;
import com.dokdok.book.dto.response.PersonalReadingTopicAnswerResponse;
import com.dokdok.book.dto.response.ReadingRecordCursor;
import com.dokdok.book.entity.PersonalBook;
import com.dokdok.book.entity.PersonalReadingRecord;
import com.dokdok.book.entity.RecordType;
import com.dokdok.book.exception.RecordErrorCode;
import com.dokdok.book.exception.RecordException;
import com.dokdok.book.dto.request.PersonalReadingRecordUpdateRequest;
import com.dokdok.book.repository.PersonalReadingRecordRepository;
import com.dokdok.global.util.SecurityUtil;
import com.dokdok.meeting.entity.Meeting;
import com.dokdok.meeting.entity.MeetingStatus;
import com.dokdok.meeting.exception.MeetingErrorCode;
import com.dokdok.meeting.exception.MeetingException;
import com.dokdok.meeting.repository.MeetingRepository;
import com.dokdok.topic.entity.Topic;
import com.dokdok.topic.entity.TopicAnswer;
import com.dokdok.topic.repository.TopicAnswerRepository;
import com.dokdok.topic.repository.TopicRepository;
import com.dokdok.user.entity.User;
import com.dokdok.user.service.UserValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PersonalReadingRecordService {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final PersonalReadingRecordRepository personalReadingRecordRepository;
    private final UserValidator userValidator;
    private final BookValidator bookValidator;
    private final MeetingRepository meetingRepository;
    private final TopicRepository topicRepository;
    private final TopicAnswerRepository topicAnswerRepository;

    @Transactional
    public PersonalReadingRecordCreateResponse create(Long personalBookId, PersonalReadingRecordCreateRequest request) {
        User userEntity = userValidator.findUserOrThrow(SecurityUtil.getCurrentUserId());
        PersonalBook personalBookEntity = bookValidator.validatePersonalBook(userEntity.getId(), personalBookId);

        Map<String, Object> normalizedMeta = normalizeMeta(request.recordType(), request.meta());

        PersonalReadingRecord personalReadingRecordEntity =
                PersonalReadingRecord.create(
                        personalBookEntity,
                        userEntity,
                        request.recordType(),
                        request.recordContent(),
                        normalizedMeta
                        );
        personalReadingRecordRepository.save(personalReadingRecordEntity);

        return PersonalReadingRecordCreateResponse.from(personalReadingRecordEntity);
    }

    @Transactional
    public PersonalReadingRecordCreateResponse update(Long personalBookId, Long recordId, PersonalReadingRecordUpdateRequest request) {
        User userEntity = userValidator.findUserOrThrow(SecurityUtil.getCurrentUserId());
        PersonalBook personalBookEntity = bookValidator.validatePersonalBook(userEntity.getId(), personalBookId);

        PersonalReadingRecord personalReadingRecord = personalReadingRecordRepository
                .findByIdAndPersonalBook_IdAndUserId(recordId, personalBookEntity.getId(), userEntity.getId())
                .orElseThrow(() -> new RecordException(RecordErrorCode.RECORD_NOT_FOUND));

        Map<String, Object> normalizedMeta = normalizeMeta(request.recordType(), request.meta());

        personalReadingRecord.update(
                request.recordType(),
                request.recordContent(),
                normalizedMeta
        );

        return PersonalReadingRecordCreateResponse.from(personalReadingRecord);
    }

    @Transactional
    public void delete(Long personalBookId, Long recordId) {
        User userEntity = userValidator.findUserOrThrow(SecurityUtil.getCurrentUserId());
        PersonalBook personalBookEntity = bookValidator.validatePersonalBook(userEntity.getId(), personalBookId);

        PersonalReadingRecord personalReadingRecord = personalReadingRecordRepository
                .findByIdAndPersonalBook_IdAndUserId(recordId, personalBookEntity.getId(), userEntity.getId())
                .orElseThrow(() -> new RecordException(RecordErrorCode.RECORD_NOT_FOUND));

        if (personalReadingRecord.isDeleted()) {
            throw new RecordException(RecordErrorCode.RECORD_ALREADY_DELETED);
        }

        personalReadingRecord.delete();
    }

    public CursorPageResponse<PersonalReadingRecordListResponse, ReadingRecordCursor> getRecords(
            Long personalBookId,
            OffsetDateTime cursorCreatedAt,
            Long cursorRecordId,
            Integer size
    ) {
        User userEntity = userValidator.findUserOrThrow(SecurityUtil.getCurrentUserId());
        PersonalBook personalBookEntity = bookValidator.validatePersonalBook(userEntity.getId(), personalBookId);
        int pageSize = resolvePageSize(size);
        LocalDateTime cursorCreatedAtValue = cursorCreatedAt != null ? cursorCreatedAt.toLocalDateTime() : null;

        boolean hasCursor = cursorCreatedAtValue != null && cursorRecordId != null;
        List<PersonalReadingRecord> entities;
        if (hasCursor) {
            entities = personalReadingRecordRepository.findRecordsByCursor(
                    personalBookEntity.getId(),
                    userEntity.getId(),
                    cursorCreatedAtValue,
                    cursorRecordId,
                    PageRequest.of(0, pageSize + 1)
            );
        } else {
            entities = personalReadingRecordRepository
                    .findAllByPersonalBook_IdAndUserId(
                            personalBookEntity.getId(),
                            userEntity.getId(),
                            PageRequest.of(
                                    0,
                                    pageSize + 1,
                                    Sort.by(Sort.Direction.DESC, "createdAt", "id")
                            )
                    )
                    .getContent();
        }
        long totalCount = personalReadingRecordRepository.countByPersonalBook_IdAndUserId(
                personalBookEntity.getId(),
                userEntity.getId()
        );

        boolean hasNext = entities.size() > pageSize;
        List<PersonalReadingRecord> pageEntities = hasNext ? entities.subList(0, pageSize) : entities;
        List<PersonalReadingRecordListResponse> items = pageEntities.stream()
                .map(PersonalReadingRecordListResponse::from)
                .toList();

        ReadingRecordCursor nextCursor = null;
        if (hasNext && !pageEntities.isEmpty()) {
            PersonalReadingRecord last = pageEntities.get(pageEntities.size() - 1);
            nextCursor = ReadingRecordCursor.from(last.getCreatedAt(), last.getId());
        }

        return CursorPageResponse.of(items, pageSize, hasNext, nextCursor, totalCount);
    }

    public PersonalReadingTopicAnswerResponse getTopicAnswers(Long personalBookId) {
        User userEntity = userValidator.findUserOrThrow(SecurityUtil.getCurrentUserId());
        PersonalBook personalBookEntity = bookValidator.validatePersonalBook(userEntity.getId(), personalBookId);

        if (personalBookEntity.getGathering() == null || personalBookEntity.getBook() == null) {
            throw new MeetingException(MeetingErrorCode.MEETING_NOT_FOUND);
        }

        Meeting meeting = meetingRepository
                .findTopByGatheringIdAndBookIdAndMeetingStatusOrderByMeetingStartDateDescIdDesc(
                        personalBookEntity.getGathering().getId(),
                        personalBookEntity.getBook().getId(),
                        MeetingStatus.CONFIRMED
                )
                .orElseThrow(() -> new MeetingException(MeetingErrorCode.MEETING_NOT_FOUND));

        List<Topic> topics = topicRepository.findTopicsInfoByMeetingId(meeting.getId());
        List<TopicAnswer> topicAnswers = topicAnswerRepository.findByMeetingIdUserId(meeting.getId(), userEntity.getId());

        Map<Long, TopicAnswer> topicAnswerMap = topicAnswers.stream()
                .collect(Collectors.toMap(ta -> ta.getTopic().getId(), Function.identity()));

        List<PersonalReadingTopicAnswerResponse.TopicAnswerInfo> items = topics.stream()
                .sorted(Comparator
                        .comparing(Topic::getConfirmOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(Topic::getId))
                .map(topic -> new PersonalReadingTopicAnswerResponse.TopicAnswerInfo(
                        topic.getTitle(),
                        topic.getDescription(),
                        topic.getConfirmOrder(),
                        topicAnswerMap.containsKey(topic.getId())
                                ? topicAnswerMap.get(topic.getId()).getContent()
                                : null
                ))
                .toList();

        return new PersonalReadingTopicAnswerResponse(
                "PRE_OPINION",
                meeting.getGathering().getGatheringName(),
                meeting.getMeetingStartDate(),
                items
        );
    }

    private Map<String, Object> normalizeMeta(RecordType recordType, Map<String, Object> meta) {
        if (recordType == null) {
            throw new RecordException(RecordErrorCode.INVALID_RECORD_TYPE);
        }

        if (recordType == RecordType.MEMO) {
            return null;
        }
        if (recordType == RecordType.QUOTE) {
            if (meta == null) {
                throw new RecordException(RecordErrorCode.INVALID_RECORD_REQUEST);
            }

            Object page = meta.get("page");
            Object excerpt = meta.get("excerpt");

            if (page == null || excerpt == null) {
                throw new RecordException(RecordErrorCode.INVALID_RECORD_REQUEST);
            }
            meta.put("page", String.valueOf(page));
            meta.put("excerpt", String.valueOf(excerpt));
        }
        return meta;
    }

    private int resolvePageSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return size;
    }
}
