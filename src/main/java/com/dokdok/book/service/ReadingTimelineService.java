package com.dokdok.book.service;

import com.dokdok.book.dto.request.PreOpinionTimeType;
import com.dokdok.book.dto.response.*;
import com.dokdok.book.entity.ReflectionRecordType;
import com.dokdok.book.entity.PersonalBook;
import com.dokdok.book.entity.PersonalReadingRecord;
import com.dokdok.book.repository.PersonalReadingRecordRepository;
import com.dokdok.book.repository.ReadingTimelineRepository;
import com.dokdok.book.repository.dto.ReadingTimelineIndexRow;
import com.dokdok.global.response.CursorResponse;
import com.dokdok.global.util.SecurityUtil;
import com.dokdok.meeting.entity.Meeting;
import com.dokdok.meeting.repository.MeetingRepository;
import com.dokdok.retrospective.dto.projection.ChangedThoughtProjection;
import com.dokdok.retrospective.dto.projection.FreeTextProjection;
import com.dokdok.retrospective.dto.projection.OtherPerspectiveProjection;
import com.dokdok.retrospective.dto.response.RetrospectiveRecordResponse;
import com.dokdok.retrospective.entity.PersonalMeetingRetrospective;
import com.dokdok.retrospective.repository.ChangedThoughtRepository;
import com.dokdok.retrospective.repository.FreeTextRepository;
import com.dokdok.retrospective.repository.OthersPerspectiveRepository;
import com.dokdok.retrospective.repository.PersonalRetrospectiveRepository;
import com.dokdok.retrospective.service.PersonalRetrospectiveAssembler;
import com.dokdok.topic.entity.Topic;
import com.dokdok.topic.entity.TopicAnswer;
import com.dokdok.topic.repository.TopicAnswerRepository;
import com.dokdok.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadingTimelineService {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final ReadingTimelineRepository readingTimelineRepository;
    private final PersonalReadingRecordRepository personalReadingRecordRepository;
    private final PersonalRetrospectiveRepository personalRetrospectiveRepository;
    private final ChangedThoughtRepository changedThoughtRepository;
    private final OthersPerspectiveRepository othersPerspectiveRepository;
    private final FreeTextRepository freeTextRepository;
    private final TopicRepository topicRepository;
    private final TopicAnswerRepository topicAnswerRepository;
    private final MeetingRepository meetingRepository;
    private final BookValidator bookValidator;
    private final PersonalRetrospectiveAssembler personalRetrospectiveAssembler;

    public CursorResponse<ReadingTimelineItem, ReadingTimelineCursor> getTimeline(
            Long personalBookId,
            LocalDateTime cursorEventAt,
            ReadingTimelineType cursorType,
            Long cursorSourceId,
            Integer size,
            PreOpinionTimeType preOpinionTime
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        PersonalBook personalBook = bookValidator.validatePersonalBook(userId, personalBookId);

        Long bookId = personalBook.getBook().getId();
        Long gatheringId = personalBook.getGathering() != null
                ? personalBook.getGathering().getId()
                : null;

        int pageSize = resolvePageSize(size);

        boolean hasCursor = cursorEventAt != null && cursorType != null && cursorSourceId != null;
        LocalDateTime cursorEventAtValue = hasCursor ? cursorEventAt : null;
        Integer cursorTypeOrder = hasCursor ? cursorType.getOrder() : null;
        Long cursorSourceIdValue = hasCursor ? cursorSourceId : null;

        List<ReadingTimelineIndexRow> indexRows = readingTimelineRepository.findTimeline(
                personalBookId,
                userId,
                bookId,
                gatheringId,
                (preOpinionTime != null ? preOpinionTime.name() : PreOpinionTimeType.ANSWER_CREATED.name()),
                cursorEventAtValue,
                cursorTypeOrder,
                cursorSourceIdValue,
                pageSize + 1
        );

        boolean hasNext = indexRows.size() > pageSize;
        List<ReadingTimelineIndexRow> pageRows = hasNext
                ? indexRows.subList(0, pageSize)
                : indexRows;

        if (pageRows.isEmpty()) {
            return CursorResponse.of(List.of(), pageSize, false, null);
        }

        List<Long> readingRecordIds = pageRows.stream()
                .filter(row -> ReadingTimelineType.READING_RECORD.name().equals(row.type()))
                .map(ReadingTimelineIndexRow::sourceId)
                .toList();
        List<Long> retrospectiveIds = pageRows.stream()
                .filter(row -> ReadingTimelineType.PERSONAL_RETROSPECTIVE.name().equals(row.type()))
                .map(ReadingTimelineIndexRow::sourceId)
                .toList();
        List<Long> groupRetrospectiveMeetingIds = pageRows.stream()
                .filter(row -> ReadingTimelineType.GROUP_RETROSPECTIVE.name().equals(row.type()))
                .map(ReadingTimelineIndexRow::sourceId)
                .toList();
        List<Long> meetingIds = pageRows.stream()
                .filter(row -> ReadingTimelineType.PRE_OPINION.name().equals(row.type()))
                .map(ReadingTimelineIndexRow::sourceId)
                .toList();

        Map<Long, PersonalReadingRecordListResponse> readingRecordMap =
                fetchReadingRecords(readingRecordIds, personalBookId, userId);
        Map<Long, RetrospectiveRecordResponse> retrospectiveMap =
                fetchRetrospectives(retrospectiveIds, userId);
        Map<Long, RetrospectiveRecordResponse> groupRetrospectiveMap =
                fetchGroupRetrospectives(groupRetrospectiveMeetingIds);
        Map<Long, ReadingTimelinePreOpinionResponse> preOpinionMap =
                fetchPreOpinions(meetingIds, userId);

        List<ReadingTimelineItem> items = pageRows.stream()
                .map(row -> {
                    ReadingTimelineType type = ReadingTimelineType.from(row.type());
                    return switch (type) {
                        case READING_RECORD -> ReadingTimelineItem.readingRecord(
                                row.eventAt(),
                                row.sourceId(),
                                readingRecordMap.get(row.sourceId())
                        );
                        case PERSONAL_RETROSPECTIVE -> ReadingTimelineItem.retrospective(
                                row.eventAt(),
                                row.sourceId(),
                                retrospectiveMap.get(row.sourceId())
                        );
                        case GROUP_RETROSPECTIVE -> ReadingTimelineItem.groupRetrospective(
                                row.eventAt(),
                                row.sourceId(),
                                groupRetrospectiveMap.get(row.sourceId())
                        );
                        case PRE_OPINION -> ReadingTimelineItem.preOpinion(
                                row.eventAt(),
                                row.sourceId(),
                                preOpinionMap.get(row.sourceId())
                        );
                    };
                })
                .toList();

        ReadingTimelineCursor nextCursor = null;
        if (hasNext) {
            ReadingTimelineIndexRow last = pageRows.get(pageRows.size() - 1);
            nextCursor = ReadingTimelineCursor.from(
                    last.eventAt(),
                    ReadingTimelineType.from(last.type()),
                    last.sourceId()
            );
        }

        return CursorResponse.of(items, pageSize, hasNext, nextCursor);
    }

    private Map<Long, PersonalReadingRecordListResponse> fetchReadingRecords(
            List<Long> recordIds,
            Long personalBookId,
            Long userId
    ) {
        if (recordIds.isEmpty()) {
            return Map.of();
        }
        List<PersonalReadingRecord> records =
                personalReadingRecordRepository.findByIdInAndPersonalBook_IdAndUserId(
                        recordIds, personalBookId, userId
                );
        Map<Long, PersonalReadingRecordListResponse> map = new HashMap<>();
        for (PersonalReadingRecord record : records) {
            map.put(record.getId(), PersonalReadingRecordListResponse.from(record));
        }
        return map;
    }

    private Map<Long, RetrospectiveRecordResponse> fetchRetrospectives(
            List<Long> retrospectiveIds,
            Long userId
    ) {
        if (retrospectiveIds.isEmpty()) {
            return Map.of();
        }

        List<PersonalMeetingRetrospective> retrospectives =
                personalRetrospectiveRepository.findByIdsWithMeeting(retrospectiveIds, userId);

        if (retrospectives.isEmpty()) {
            return Map.of();
        }

        List<Long> ids = retrospectives.stream()
                .map(PersonalMeetingRetrospective::getId)
                .toList();

        Map<Long, List<ChangedThoughtProjection>> changedThoughtsMap =
                changedThoughtRepository.findByRetrospectiveIds(ids)
                        .stream()
                        .collect(groupingBy(ChangedThoughtProjection::retrospectiveId));

        Map<Long, List<OtherPerspectiveProjection>> othersPerspectivesMap =
                othersPerspectiveRepository.findByRetrospectiveIds(ids)
                        .stream()
                        .collect(groupingBy(OtherPerspectiveProjection::retrospectiveId));

        Map<Long, List<FreeTextProjection>> freeTextsMap =
                freeTextRepository.findByRetrospectiveIds(ids)
                        .stream()
                        .collect(groupingBy(FreeTextProjection::retrospectiveId));

        List<RetrospectiveRecordResponse> responses = personalRetrospectiveAssembler.assembleRecords(
                retrospectives,
                changedThoughtsMap,
                othersPerspectivesMap,
                freeTextsMap
        );

        Map<Long, RetrospectiveRecordResponse> map = new HashMap<>();
        for (RetrospectiveRecordResponse response : responses) {
            map.put(response.retrospectiveId(), response);
        }
        return map;
    }

    private Map<Long, RetrospectiveRecordResponse> fetchGroupRetrospectives(List<Long> meetingIds) {
        if (meetingIds.isEmpty()) {
            return Map.of();
        }

        List<Meeting> meetings = meetingRepository.findByIdInWithGathering(meetingIds);
        Map<Long, RetrospectiveRecordResponse> map = new HashMap<>();

        for (Meeting meeting : meetings) {
            if (!meeting.isRetrospectivePublished() || meeting.getRetrospectivePublishedAt() == null) {
                continue;
            }

            map.put(
                    meeting.getId(),
                    RetrospectiveRecordResponse.of(
                            meeting.getId(),
                            meeting.getGathering().getGatheringName(),
                            ReflectionRecordType.MEETING_RETROSPECTIVE,
                            meeting.getRetrospectivePublishedAt(),
                            List.of(),
                            List.of()
                    )
            );
        }

        return map;
    }

    private Map<Long, ReadingTimelinePreOpinionResponse> fetchPreOpinions(
            List<Long> meetingIds,
            Long userId
    ) {
        if (meetingIds.isEmpty()) {
            return Map.of();
        }

        List<Meeting> meetings = meetingRepository.findByIdInWithGathering(meetingIds);
        Map<Long, Meeting> meetingMap = new HashMap<>();
        for (Meeting meeting : meetings) {
            meetingMap.put(meeting.getId(), meeting);
        }

        List<Topic> topics = topicRepository.findTopicsInfoByMeetingIds(meetingIds);
        Map<Long, List<Topic>> topicsByMeeting = topics.stream()
                .collect(groupingBy(topic -> topic.getMeeting().getId()));

        List<TopicAnswer> answers = topicAnswerRepository.findByMeetingIdsUserId(meetingIds, userId);
        Map<Long, Map<Long, TopicAnswer>> answersByMeeting = new HashMap<>();
        for (TopicAnswer answer : answers) {
            Long meetingId = answer.getTopic().getMeeting().getId();
            answersByMeeting
                    .computeIfAbsent(meetingId, key -> new HashMap<>())
                    .put(answer.getTopic().getId(), answer);
        }

        Map<Long, ReadingTimelinePreOpinionResponse> map = new HashMap<>();
        for (Long meetingId : meetingIds) {
            Meeting meeting = meetingMap.get(meetingId);
            if (meeting == null) {
                continue;
            }
            List<Topic> meetingTopics = topicsByMeeting.getOrDefault(meetingId, List.of());
            meetingTopics = meetingTopics.stream()
                    .sorted(Comparator
                            .comparing(Topic::getConfirmOrder, Comparator.nullsLast(Integer::compareTo))
                            .thenComparing(Topic::getId))
                    .toList();

            Map<Long, TopicAnswer> answerMap = answersByMeeting.getOrDefault(meetingId, Map.of());
            List<ReadingTimelinePreOpinionResponse.TopicAnswerInfo> items = meetingTopics.stream()
                    .map(topic -> new ReadingTimelinePreOpinionResponse.TopicAnswerInfo(
                            topic.getTitle(),
                            topic.getDescription(),
                            topic.getConfirmOrder(),
                            answerMap.containsKey(topic.getId())
                                    ? answerMap.get(topic.getId()).getContent()
                                    : null
                    ))
                    .toList();

            ReadingTimelinePreOpinionResponse response = new ReadingTimelinePreOpinionResponse(
                    "PRE_OPINION",
                    meeting.getGathering().getId(),
                    meeting.getId(),
                    meeting.getGathering().getGatheringName(),
                    meeting.getMeetingStartDate(),
                    items
            );
            map.put(meetingId, response);
        }

        return map;
    }

    private int resolvePageSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return size;
    }
}
