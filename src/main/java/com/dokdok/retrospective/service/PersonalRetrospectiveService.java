package com.dokdok.retrospective.service;

import com.dokdok.book.service.BookValidator;
import com.dokdok.global.response.CursorResponse;
import com.dokdok.retrospective.dto.projection.ChangedThoughtProjection;
import com.dokdok.retrospective.dto.projection.FreeTextProjection;
import com.dokdok.retrospective.dto.projection.OtherPerspectiveProjection;
import com.dokdok.retrospective.dto.response.*;
import com.dokdok.meeting.entity.MeetingMember;
import com.dokdok.meeting.repository.MeetingMemberRepository;
import com.dokdok.meeting.service.MeetingValidator;
import com.dokdok.retrospective.repository.ChangedThoughtRepository;
import com.dokdok.retrospective.repository.FreeTextRepository;
import com.dokdok.retrospective.repository.OthersPerspectiveRepository;
import com.dokdok.retrospective.repository.PersonalRetrospectiveRepository;
import com.dokdok.topic.repository.TopicAnswerRepository;
import com.dokdok.topic.repository.TopicRepository;
import com.dokdok.topic.service.TopicValidator;
import com.dokdok.user.service.UserValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dokdok.global.util.SecurityUtil;
import com.dokdok.meeting.entity.Meeting;
import com.dokdok.retrospective.dto.request.PersonalRetrospectiveRequest;
import com.dokdok.retrospective.entity.PersonalMeetingRetrospective;
import com.dokdok.retrospective.entity.RetrospectiveChangedThought;
import com.dokdok.retrospective.entity.RetrospectiveFreeText;
import com.dokdok.retrospective.entity.RetrospectiveOthersPerspective;
import com.dokdok.topic.entity.Topic;
import com.dokdok.topic.entity.TopicAnswer;
import com.dokdok.user.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;

import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PersonalRetrospectiveService {

    private final PersonalRetrospectiveRepository personalRetrospectiveRepository;
    private final ChangedThoughtRepository changedThoughtRepository;
    private final OthersPerspectiveRepository othersPerspectiveRepository;
    private final FreeTextRepository freeTextRepository;
    private final TopicRepository topicRepository;
    private final MeetingMemberRepository meetingMemberRepository;
    private final TopicAnswerRepository topicAnswerRepository;
    private final MeetingValidator meetingValidator;
    private final UserValidator userValidator;
    private final RetrospectiveValidator retrospectiveValidator;
    private final TopicValidator topicValidator;
    private final BookValidator bookValidator;
    private final PersonalRetrospectiveAssembler assembler;

    @Transactional
    public PersonalRetrospectiveResponse createPersonalRetrospective(Long meetingId, PersonalRetrospectiveRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();

        Meeting meeting = meetingValidator.findMeetingOrThrow(meetingId);
        User user = userValidator.findUserOrThrow(userId);

        retrospectiveValidator.validateRetrospective(meetingId, userId);

        PersonalMeetingRetrospective retrospective = PersonalMeetingRetrospective.create(meeting, user);

        setRetrospectiveData(retrospective, request, meetingId, userId);

        PersonalMeetingRetrospective saved = personalRetrospectiveRepository.save(retrospective);

        return PersonalRetrospectiveResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public PersonalRetrospectiveFormResponse getPersonalRetrospectiveForm(Long meetingId) {

        Long userId = SecurityUtil.getCurrentUserId();

        Meeting meeting = meetingValidator.findMeetingOrThrow(meetingId);
        meetingValidator.validateMeetingMember(meetingId, userId);
        retrospectiveValidator.validateRetrospective(meetingId, userId);

        List<Topic> topics = topicValidator.getConfirmedTopics(meetingId);
        List<TopicAnswer> topicAnswers = topicAnswerRepository.findByMeetingIdUserId(meetingId, userId);
        List<MeetingMember> meetingMembers = meetingMemberRepository.findOtherMembersByMeetingId(meetingId, userId);

        return assembler.assembleCreate(
                meeting,
                topics,
                topicAnswers,
                meetingMembers
        );
    }

    @Transactional(readOnly = true)
    public PersonalRetrospectiveEditResponse getPersonalRetrospectiveEditForm(Long meetingId) {

        Long userId = SecurityUtil.getCurrentUserId();

        meetingValidator.validateMeeting(meetingId);
        meetingValidator.validateMeetingMember(meetingId, userId);

        PersonalMeetingRetrospective retrospective
                = retrospectiveValidator.getRetrospectiveByMeetingAndUser(meetingId, userId);
        Long retrospectiveId = retrospective.getId();
        Meeting meeting = retrospective.getMeeting();

        List<RetrospectiveChangedThought> changedThoughts
                = changedThoughtRepository.findByPersonalMeetingRetrospective(retrospectiveId);

        List<RetrospectiveOthersPerspective> othersPerspectives
                = othersPerspectiveRepository.findByPersonalMeetingRetrospective(retrospectiveId);

        List<RetrospectiveFreeText> freeTexts =
                freeTextRepository.findByPersonalMeetingRetrospective_Id(retrospectiveId);

        List<Topic> topics = topicValidator.getConfirmedTopics(meetingId);

        List<TopicAnswer> topicAnswers = topicAnswerRepository.findByMeetingIdUserId(meetingId, userId);
        Map<Long, TopicAnswer> taMap = topicAnswers.stream()
                .collect(Collectors.toMap(ta -> ta.getTopic().getId(), ta -> ta));

        Map<Long, RetrospectiveChangedThought> ctMap = changedThoughts.stream()
                .collect(Collectors.toMap(ct -> ct.getTopic().getId(), ct -> ct));

        List<PersonalRetrospectiveEditResponse.ChangedThought> mergedChangedThoughts = topics.stream()
                .map(topic -> {
                    RetrospectiveChangedThought ct = ctMap.get(topic.getId());
                    TopicAnswer ta = taMap.get(topic.getId());
                    return ct != null
                            ? PersonalRetrospectiveEditResponse.ChangedThought.of(ct, ta)
                            : PersonalRetrospectiveEditResponse.ChangedThought.empty(topic.getId(), ta);
                })
                .toList();

        List<MeetingMember> meetingMembers
                = meetingMemberRepository.findOtherMembersByMeetingId(meetingId, userId);

        return assembler.assembleEdit(
                meeting,
                retrospectiveId,
                mergedChangedThoughts,
                othersPerspectives,
                freeTexts,
                topics,
                meetingMembers
        );
    }

    @Transactional
    public PersonalRetrospectiveResponse editPersonalRetrospective(
            Long meetingId,
            PersonalRetrospectiveRequest request
    ) {
        Long userId = SecurityUtil.getCurrentUserId();

        meetingValidator.validateMeeting(meetingId);
        meetingValidator.validateMeetingMember(meetingId, userId);
        PersonalMeetingRetrospective retrospective
                = retrospectiveValidator.getRetrospectiveByMeetingAndUser(meetingId, userId);

        retrospective.clearChangedThoughts();
        retrospective.clearOthersPerspectives();
        retrospective.clearFreeTexts();

        setRetrospectiveData(retrospective, request, meetingId, userId);

        PersonalMeetingRetrospective saved = personalRetrospectiveRepository.save(retrospective);

        return PersonalRetrospectiveResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public CursorResponse<RetrospectiveRecordResponse, RetrospectiveRecordsCursor> getRetrospectiveRecords(
            Long personalBookId,
            int pageSize,
            LocalDateTime cursorCreatedAt,
            Long cursorRetrospectiveId
    ) {
        Long userId = SecurityUtil.getCurrentUserId();

        bookValidator.validateBook(personalBookId);

        int fetchSize = pageSize + 1;
        PageRequest pageable = PageRequest.of(0, fetchSize);

        List<PersonalMeetingRetrospective> retrospectives;
        Integer totalCount = null;
        if (cursorCreatedAt == null || cursorRetrospectiveId == null) {
            retrospectives = personalRetrospectiveRepository.findRetrospectivesFirstPage(
                    personalBookId, userId, pageable
            );
            totalCount = personalRetrospectiveRepository.countRetrospectivesByBookAndUser(
                    personalBookId, userId
            );
        } else {
            retrospectives = personalRetrospectiveRepository.findRetrospectivesAfterCursor(
                    personalBookId, userId, cursorCreatedAt, cursorRetrospectiveId, pageable
            );
        }

        boolean hasNext = retrospectives.size() > pageSize;
        if (hasNext) {
            retrospectives = retrospectives.subList(0, pageSize);
        }

        if (retrospectives.isEmpty()) {
            return CursorResponse.of(List.of(), pageSize, false, null, totalCount);
        }

        List<Long> retrospectiveIds = retrospectives.stream()
                .map(PersonalMeetingRetrospective::getId)
                .toList();

        Map<Long, List<ChangedThoughtProjection>> changedThoughtsMap =
                changedThoughtRepository.findByRetrospectiveIds(retrospectiveIds)
                        .stream()
                        .collect(groupingBy(ChangedThoughtProjection::retrospectiveId));

        Map<Long, List<OtherPerspectiveProjection>> othersPerspectivesMap =
                othersPerspectiveRepository.findByRetrospectiveIds(retrospectiveIds)
                        .stream()
                        .collect(groupingBy(OtherPerspectiveProjection::retrospectiveId));

        Map<Long, List<FreeTextProjection>> freeTextsMap =
                freeTextRepository.findByRetrospectiveIds(retrospectiveIds)
                        .stream()
                        .collect(groupingBy(FreeTextProjection::retrospectiveId));

        List<RetrospectiveRecordResponse> items = assembler.assembleRecords(
                retrospectives,
                changedThoughtsMap,
                othersPerspectivesMap,
                freeTextsMap
        );

        PersonalMeetingRetrospective lastRetrospective = retrospectives.get(retrospectives.size() - 1);

        return RetrospectiveRecordsPageResponse.from(items, pageSize, hasNext, lastRetrospective, totalCount);
    }

    @Transactional
    public void deletePersonalRetrospective(Long meetingId) {
        Long userId = SecurityUtil.getCurrentUserId();

        meetingValidator.validateMeeting(meetingId);
        meetingValidator.validateMeetingMember(meetingId, userId);

        PersonalMeetingRetrospective retrospective
                = retrospectiveValidator.getRetrospectiveByMeetingAndUser(meetingId, userId);

        retrospective.softDelete();
    }

    @Transactional(readOnly = true)
    public PersonalRetrospectiveDetailResponse getPersonalRetrospective(Long meetingId) {
        Long userId = SecurityUtil.getCurrentUserId();

        meetingValidator.validateMeeting(meetingId);
        meetingValidator.validateMeetingMember(meetingId, userId);

        PersonalMeetingRetrospective retrospective
                = retrospectiveValidator.getRetrospectiveByMeetingAndUser(meetingId, userId);
        Long retrospectiveId = retrospective.getId();
        Meeting meeting = retrospective.getMeeting();

        List<RetrospectiveChangedThought> changedThoughts
                = changedThoughtRepository.findByPersonalMeetingRetrospective(retrospectiveId);

        List<RetrospectiveOthersPerspective> othersPerspectives
                = othersPerspectiveRepository.findByPersonalMeetingRetrospective(retrospectiveId);

        List<RetrospectiveFreeText> freeTexts =
                freeTextRepository.findByPersonalMeetingRetrospective_Id(retrospectiveId);

        List<Topic> topics = topicValidator.getConfirmedTopics(meetingId);
        List<TopicAnswer> topicAnswers = topicAnswerRepository.findByMeetingIdUserId(meetingId, userId);

        Map<Long, RetrospectiveChangedThought> ctMap = changedThoughts.stream()
                .collect(Collectors.toMap(ct -> ct.getTopic().getId(), ct -> ct));
        Map<Long, TopicAnswer> taMap = topicAnswers.stream()
                .collect(Collectors.toMap(ta -> ta.getTopic().getId(), ta -> ta));

        List<PersonalRetrospectiveDetailResponse.ChangedThought> mergedChangedThoughts = topics.stream()
                .map(topic -> PersonalRetrospectiveDetailResponse.ChangedThought.of(
                        topic,
                        ctMap.get(topic.getId()),
                        taMap.get(topic.getId())
                ))
                .toList();

        return assembler.assembleView(
                meeting,
                retrospectiveId,
                mergedChangedThoughts,
                othersPerspectives,
                freeTexts
        );
    }

    private void setRetrospectiveData(
            PersonalMeetingRetrospective retrospective,
            PersonalRetrospectiveRequest request,
            Long meetingId,
            Long userId
    ) {
        // ChangedThoughts 추가
        if (request.changedThoughts() != null) {
            for (var thought : request.changedThoughts()) {
                Topic topic = topicValidator.getTopicInMeeting(thought.topicId(), meetingId);

                RetrospectiveChangedThought changedThought = RetrospectiveChangedThought.create(
                        topic,
                        retrospective,
                        thought.keyIssue(),
                        thought.postOpinion()
                );

                retrospective.addChangedThought(changedThought);
            }
        }

        // OthersPerspectives 추가
        if (request.othersPerspectives() != null) {
            for (var perspective : request.othersPerspectives()) {
                Optional<Topic> topic = Optional.ofNullable(perspective.topicId())
                        .flatMap(topicRepository::findById);

                MeetingMember meetingMember = meetingValidator.getMeetingMember(meetingId, perspective.meetingMemberId());

                RetrospectiveOthersPerspective othersPerspective = RetrospectiveOthersPerspective.create(
                        retrospective,
                        topic.orElse(null),
                        meetingMember,
                        perspective.opinionContent(),
                        perspective.impressiveReason()
                );

                retrospective.addOthersPerspective(othersPerspective);
            }
        }

        // FreeTexts 추가
        if (request.freeTexts() != null) {
            for (var freeText : request.freeTexts()) {
                RetrospectiveFreeText text = RetrospectiveFreeText.of(
                        retrospective,
                        freeText.title(),
                        freeText.content()
                );

                retrospective.addFreeText(text);
            }
        }
    }

}
