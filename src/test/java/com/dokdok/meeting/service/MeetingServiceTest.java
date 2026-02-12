package com.dokdok.meeting.service;

import com.dokdok.book.entity.Book;
import com.dokdok.book.repository.BookRepository;
import com.dokdok.book.service.BookValidator;
import com.dokdok.book.service.PersonalBookService;
import com.dokdok.gathering.entity.Gathering;
import com.dokdok.gathering.exception.GatheringErrorCode;
import com.dokdok.gathering.exception.GatheringException;
import com.dokdok.gathering.repository.GatheringMemberRepository;
import com.dokdok.gathering.repository.GatheringRepository;
import com.dokdok.gathering.service.GatheringValidator;
import com.dokdok.global.response.CursorResponse;
import com.dokdok.global.response.PageResponse;
import com.dokdok.global.util.SecurityUtil;
import com.dokdok.meeting.dto.*;
import com.dokdok.meeting.entity.Meeting;
import com.dokdok.meeting.entity.MeetingMember;
import com.dokdok.meeting.entity.MeetingMemberRole;
import com.dokdok.meeting.entity.MeetingStatus;
import com.dokdok.meeting.exception.MeetingErrorCode;
import com.dokdok.meeting.exception.MeetingException;
import com.dokdok.meeting.repository.MeetingMemberRepository;
import com.dokdok.meeting.repository.MeetingRepository;
import com.dokdok.topic.entity.TopicStatus;
import com.dokdok.topic.entity.TopicType;
import com.dokdok.topic.repository.TopicAnswerRepository;
import com.dokdok.topic.repository.TopicRepository;
import com.dokdok.user.entity.User;
import com.dokdok.user.service.UserValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingServiceTest {

    @InjectMocks
    private MeetingService meetingService;

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private MeetingMemberRepository meetingMemberRepository;

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private TopicAnswerRepository topicAnswerRepository;

    @Mock
    private GatheringRepository gatheringRepository;

    @Mock
    private GatheringMemberRepository gatheringMemberRepository;

    @Mock
    private GatheringValidator gatheringValidator;

    @Mock
    private MeetingValidator meetingValidator;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookValidator bookValidator;

    @Mock
    private UserValidator userValidator;

    @Mock
    private PersonalBookService personalBookService;

    private Meeting meeting;
    private Long meetingId;
    private Gathering gathering;
    private User leader;

    @BeforeEach
    void setUp() {
        meetingId = 1L;
        leader = User.builder()
                .id(10L)
                .nickname("leader")
                .build();
        gathering = Gathering.builder()
                .id(100L)
                .gatheringName("gathering")
                .gatheringLeader(leader)
                .invitationLink("link")
                .build();
        meeting = Meeting.builder()
                .id(meetingId)
                .meetingName("Meeting 1")
                .meetingStatus(MeetingStatus.PENDING)
                .meetingStartDate(LocalDateTime.now().plusDays(2))
                .meetingEndDate(LocalDateTime.now().plusDays(2).plusHours(1))
                .meetingLeader(leader)
                .gathering(gathering)
                .book(sampleBook())
                .build();
    }

    private Book sampleBook() {
        return Book.builder()
                .id(50L)
                .bookName("Sample Book")
                .author("Author")
                .publisher("Publisher")
                .isbn("ISBN-0000")
                .thumbnail("thumbnail")
                .build();
    }

    @DisplayName("약속을 조회하면 상세 응답을 반환한다.")
    @Test
    void givenMeetingId_whenFindMeeting_thenMeetingResponse() {
        // given
        Long userId = 1L;
        given(meetingValidator.findMeetingOrThrow(meetingId))
                .willReturn(meeting);
        given(meetingMemberRepository.findAllByMeetingId(meetingId))
                .willReturn(java.util.Collections.emptyList());
        given(topicRepository.findConfirmedTopicDateByMeetingId(meetingId, TopicStatus.CONFIRMED))
                .willReturn(null);
        try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

            // when
            MeetingDetailResponse findMeeting = meetingService.findMeeting(meetingId);

            // then
            assertThat(findMeeting.meetingName()).isEqualTo(meeting.getMeetingName());
            assertThat(findMeeting.meetingStatus()).isEqualTo(meeting.getMeetingStatus());
            assertThat(findMeeting.progressStatus()).isEqualTo(MeetingDetailProgressStatus.PRE);
            assertThat(findMeeting.confirmedTopic()).isFalse();
            assertThat(findMeeting.confirmedTopicDate()).isNull();
        }
    }

    @DisplayName("약속 상세 응답에 진행 상태가 전/중/후로 내려간다.")
    @Test
    void givenMeetingDates_whenFindMeeting_thenProgressStatus() {
        // given
        Long userId = 1L;
        LocalDateTime now = LocalDateTime.now();
        Meeting upcomingMeeting = Meeting.builder()
                .id(meetingId)
                .meetingName("Upcoming")
                .meetingStatus(MeetingStatus.CONFIRMED)
                .meetingStartDate(now.plusDays(1))
                .meetingEndDate(now.plusDays(1).plusHours(1))
                .meetingLeader(leader)
                .gathering(gathering)
                .build();
        Meeting ongoingMeeting = Meeting.builder()
                .id(meetingId)
                .meetingName("Ongoing")
                .meetingStatus(MeetingStatus.CONFIRMED)
                .meetingStartDate(now.minusHours(1))
                .meetingEndDate(now.plusHours(1))
                .meetingLeader(leader)
                .gathering(gathering)
                .build();
        Meeting finishedMeeting = Meeting.builder()
                .id(meetingId)
                .meetingName("Finished")
                .meetingStatus(MeetingStatus.DONE)
                .meetingStartDate(now.minusDays(1))
                .meetingEndDate(now.minusDays(1).plusHours(1))
                .meetingLeader(leader)
                .gathering(gathering)
                .build();

        given(meetingValidator.findMeetingOrThrow(meetingId))
                .willReturn(upcomingMeeting, ongoingMeeting, finishedMeeting);
        given(meetingMemberRepository.findAllByMeetingId(meetingId))
                .willReturn(java.util.Collections.emptyList());
        given(topicRepository.findConfirmedTopicDateByMeetingId(meetingId, TopicStatus.CONFIRMED))
                .willReturn(null);

        try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

            // when
            MeetingDetailResponse upcoming = meetingService.findMeeting(meetingId);
            MeetingDetailResponse ongoing = meetingService.findMeeting(meetingId);
            MeetingDetailResponse finished = meetingService.findMeeting(meetingId);

            // then
            assertThat(upcoming.progressStatus()).isEqualTo(MeetingDetailProgressStatus.PRE);
            assertThat(ongoing.progressStatus()).isEqualTo(MeetingDetailProgressStatus.ONGOING);
            assertThat(finished.progressStatus()).isEqualTo(MeetingDetailProgressStatus.POST);
        }
    }

    @DisplayName("약속 상세 응답에 주제 확정 여부와 날짜가 내려간다.")
    @Test
    void givenConfirmedTopicDate_whenFindMeeting_thenConfirmedTopicFields() {
        // given
        Long userId = 1L;
        LocalDateTime confirmedAt = LocalDateTime.now().minusHours(1);
        given(meetingValidator.findMeetingOrThrow(meetingId))
                .willReturn(meeting);
        given(meetingMemberRepository.findAllByMeetingId(meetingId))
                .willReturn(java.util.Collections.emptyList());
        given(topicRepository.findConfirmedTopicDateByMeetingId(meetingId, TopicStatus.CONFIRMED))
                .willReturn(confirmedAt);

        try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

            // when
            MeetingDetailResponse findMeeting = meetingService.findMeeting(meetingId);

            // then
            assertThat(findMeeting.confirmedTopic()).isTrue();
            assertThat(findMeeting.confirmedTopicDate()).isEqualTo(confirmedAt);
        }
    }

    @DisplayName("존재하지 않는 약속을 조회하면 예외를 던진다.")
    @Test
    void givenMissingMeetingId_whenFindMeeting_thenThrowMeetingException() {
        // given
        Long meetingId = 999L;
        Long userId = 1L;

        given(meetingValidator.findMeetingOrThrow(meetingId))
                .willThrow(new MeetingException(MeetingErrorCode.MEETING_NOT_FOUND));

        try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

            // when + then
            assertThatThrownBy(() -> meetingService.findMeeting(meetingId))
                    .isInstanceOf(MeetingException.class)
                    .extracting("errorCode")
                    .isEqualTo(MeetingErrorCode.MEETING_NOT_FOUND);
        }
    }

    @DisplayName("모임장이 약속을 삭제하면 연관 데이터가 soft delete 되고 약속이 삭제된다.")
    @Test
    void givenLeaderAndDeletableMeeting_whenDeleteMeeting_thenSoftDeleteAndRemove() {
        // given
        Long userId = leader.getId();
        meeting = Meeting.builder()
                .id(meetingId)
                .meetingStatus(MeetingStatus.CONFIRMED)
                .meetingStartDate(LocalDateTime.now().plusDays(2))
                .meetingLeader(leader)
                .gathering(gathering)
                .build();

        given(meetingValidator.findMeetingOrThrow(meetingId))
                .willReturn(meeting);

        try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

            // when
            meetingService.deleteMeeting(meetingId);

            // then
            verify(gatheringValidator).validateLeader(gathering.getId(), userId);
            verify(topicAnswerRepository).softDeleteByMeetingId(meetingId);
            verify(topicRepository).softDeleteByMeetingId(meetingId);
            verify(meetingRepository).delete(meeting);
        }
    }

    @DisplayName("약속 시작 24시간 이내면 약속 삭제가 불가하다.")
    @Test
    void givenMeetingWithin24Hours_whenDeleteMeeting_thenThrowException() {
        // given
        Long userId = leader.getId();
        meeting = Meeting.builder()
                .id(meetingId)
                .meetingStatus(MeetingStatus.CONFIRMED)
                .meetingStartDate(LocalDateTime.now().plusHours(23))
                .meetingLeader(leader)
                .gathering(gathering)
                .build();

        given(meetingValidator.findMeetingOrThrow(meetingId))
                .willReturn(meeting);

        try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

            // when + then
            assertThatThrownBy(() -> meetingService.deleteMeeting(meetingId))
                    .isInstanceOf(MeetingException.class)
                    .extracting("errorCode")
                    .isEqualTo(MeetingErrorCode.MEETING_DELETE_NOT_ALLOWED);
        }
    }

    @DisplayName("완료된 약속은 삭제할 수 없다.")
    @Test
    void givenDoneMeeting_whenDeleteMeeting_thenThrowException() {
        // given
        Long userId = leader.getId();
        meeting = Meeting.builder()
                .id(meetingId)
                .meetingStatus(MeetingStatus.DONE)
                .meetingLeader(leader)
                .gathering(gathering)
                .build();

        given(meetingValidator.findMeetingOrThrow(meetingId))
                .willReturn(meeting);

        try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

            // when + then
            assertThatThrownBy(() -> meetingService.deleteMeeting(meetingId))
                    .isInstanceOf(MeetingException.class)
                    .extracting("errorCode")
                    .isEqualTo(MeetingErrorCode.INVALID_MEETING_STATUS_CHANGE);
        }
    }

    @DisplayName("약속 생성 요청을 처리하면 약속 응답을 반환한다.")
    @Test
    void givenMeetingCreateRequest_whenCreateMeeting_thenMeetingResponse() {
        // given
        Long gatheringId = 3L;
        Long userId = 7L;
        String title = "book";
        String authors = "author";
        String publisher = "publisher";
        String isbn = "9781234567890";
        String thumbnail = "https://example.com/thumb.jpg";
        MeetingCreateRequest.BookInfo bookInfo = new MeetingCreateRequest.BookInfo(
                title, authors, publisher, isbn, thumbnail
        );
        LocalDateTime startDate = LocalDateTime.of(2024, 1, 20, 20, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 1, 20, 22, 0);
        int memberCount = 5;
        MeetingCreateRequest request = MeetingCreateRequest.builder()
                .gatheringId(gatheringId)
                .book(bookInfo)
                .meetingName(null)
                .meetingStartDate(startDate)
                .meetingEndDate(endDate)
                .maxParticipants(null)
                .build();

        User user = User.builder()
                .id(userId)
                .nickname("leader")
                .build();

        Gathering gathering = Gathering.builder()
                .id(gatheringId)
                .gatheringName("gathering")
                .gatheringLeader(user)
                .invitationLink("link")
                .build();

        Book book = Book.builder()
                .id(12L)
                .bookName(title)
                .author(authors)
                .publisher(publisher)
                .isbn(isbn)
                .thumbnail(thumbnail)
                .build();

        Meeting savedMeeting = Meeting.builder()
                .id(25L)
                .gathering(gathering)
                .book(book)
                .meetingLeader(user)
                .meetingName(book.getBookName())
                .meetingStatus(MeetingStatus.PENDING)
                .maxParticipants(memberCount)
                .meetingStartDate(startDate)
                .meetingEndDate(endDate)
                .build();

        given(gatheringRepository.findById(gatheringId))
                .willReturn(Optional.of(gathering));
        given(gatheringMemberRepository.countByGatheringIdAndRemovedAtIsNull(gatheringId))
                .willReturn(memberCount);
        given(bookRepository.findByIsbn(isbn))
                .willReturn(Optional.of(book));
        given(userValidator.findUserOrThrow(userId))
                .willReturn(user);
        given(meetingRepository.save(any(Meeting.class)))
                .willReturn(savedMeeting);

        try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

            // when
            MeetingResponse response = meetingService.createMeeting(request);

            // then
            assertThat(response.meetingId()).isEqualTo(savedMeeting.getId());
            assertThat(response.meetingStatus()).isEqualTo(MeetingStatus.PENDING);
            assertThat(response.meetingName()).isEqualTo(book.getBookName());
            assertThat(response.schedule().startDateTime()).isEqualTo(startDate);
            assertThat(response.schedule().endDateTime()).isEqualTo(endDate);
            assertThat(response.participants().maxCount()).isEqualTo(memberCount);
        }
    }

    @DisplayName("모임을 찾지 못하면 약속 생성 요청이 실패한다.")
    @Test
    void givenMissingGathering_whenCreateMeeting_thenThrowGatheringException() {
        // given
        Long gatheringId = 3L;
        Long userId = 7L;
        MeetingCreateRequest.BookInfo bookInfo = new MeetingCreateRequest.BookInfo(
                "book", "author", "publisher", "9781234567890", "https://example.com/thumb.jpg"
        );
        MeetingCreateRequest request = MeetingCreateRequest.builder()
                .gatheringId(gatheringId)
                .book(bookInfo)
                .build();

        given(gatheringRepository.findById(gatheringId))
                .willReturn(Optional.empty());

        try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

            // when + then
            assertThatThrownBy(() -> meetingService.createMeeting(request))
                    .isInstanceOf(GatheringException.class)
                    .extracting("errorCode")
                    .isEqualTo(GatheringErrorCode.GATHERING_NOT_FOUND);
        }
    }

    @DisplayName("책이 없으면 새로 생성해 약속을 생성한다.")
    @Test
    void givenMissingBook_whenCreateMeeting_thenCreateBook() {
        // given
        Long gatheringId = 3L;
        Long userId = 7L;
        String title = "book";
        String authors = "author";
        String publisher = "publisher";
        String isbn = "9781234567890";
        String thumbnail = "https://example.com/thumb.jpg";
        LocalDateTime startDate = LocalDateTime.of(2024, 1, 20, 20, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 1, 20, 22, 0);
        MeetingCreateRequest.BookInfo bookInfo = new MeetingCreateRequest.BookInfo(
                title, authors, publisher, isbn, thumbnail
        );
        MeetingCreateRequest request = MeetingCreateRequest.builder()
                .gatheringId(gatheringId)
                .book(bookInfo)
                .meetingStartDate(startDate)
                .meetingEndDate(endDate)
                .maxParticipants(1)
                .build();

        given(gatheringRepository.findById(gatheringId))
                .willReturn(Optional.of(Gathering.builder()
                        .id(gatheringId)
                        .gatheringName("gathering")
                        .invitationLink("link")
                        .build()));
        given(gatheringMemberRepository.countByGatheringIdAndRemovedAtIsNull(gatheringId))
                .willReturn(5);
        given(bookRepository.findByIsbn(isbn))
                .willReturn(Optional.empty());
        given(bookRepository.save(any(Book.class)))
                .willAnswer(invocation -> {
                    Book saved = invocation.getArgument(0);
                    return Book.builder()
                            .id(12L)
                            .bookName(saved.getBookName())
                            .author(saved.getAuthor())
                            .publisher(saved.getPublisher())
                            .isbn(saved.getIsbn())
                            .thumbnail(saved.getThumbnail())
                            .build();
                });
        given(userValidator.findUserOrThrow(userId))
                .willReturn(User.builder().id(userId).nickname("leader").build());
        given(meetingRepository.save(any(Meeting.class)))
                .willAnswer(invocation -> {
                    Meeting meeting = invocation.getArgument(0);
                    return Meeting.builder()
                            .id(25L)
                            .gathering(meeting.getGathering())
                            .book(meeting.getBook())
                            .meetingLeader(meeting.getMeetingLeader())
                            .meetingName(meeting.getMeetingName())
                            .meetingStatus(meeting.getMeetingStatus())
                            .maxParticipants(meeting.getMaxParticipants())
                            .meetingStartDate(meeting.getMeetingStartDate())
                            .meetingEndDate(meeting.getMeetingEndDate())
                            .build();
                });

        try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

            // when
            MeetingResponse response = meetingService.createMeeting(request);

            // then
            assertThat(response.meetingId()).isEqualTo(25L);
            assertThat(response.book().bookName()).isEqualTo(title);
        }
    }

    @DisplayName("모임장이 약속을 확정하면 약속장이 멤버로 포함된다.")
    @Test
    void givenMeetingStatus_whenConfirm_thenMeetingStatusChange() {
        // given
        Long meetingId = 1L;
        Long gatheringLeaderId = 10L;

        given(meetingValidator.findMeetingOrThrow(meetingId)).willReturn(meeting);
        given(meetingRepository.existsByGatheringIdAndMeetingStatus(gathering.getId(), MeetingStatus.CONFIRMED))
                .willReturn(false);
        given(meetingMemberRepository.findByMeetingIdAndUserId(meetingId, leader.getId()))
                .willReturn(Optional.empty());

        try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(gatheringLeaderId);

            // when
            MeetingStatusResponse response = meetingService.confirmMeeting(meetingId);

            // then
            verify(gatheringValidator).validateLeader(gathering.getId(), gatheringLeaderId);
            assertThat(meeting.getMeetingStatus()).isEqualTo(response.meetingStatus());
            ArgumentCaptor<MeetingMember> meetingMemberCaptor = ArgumentCaptor.forClass(MeetingMember.class);
            verify(meetingMemberRepository).save(meetingMemberCaptor.capture());
            MeetingMember savedMember = meetingMemberCaptor.getValue();
            assertThat(savedMember.getUser().getId()).isEqualTo(leader.getId());
            assertThat(savedMember.getMeetingRole()).isEqualTo(MeetingMemberRole.LEADER);
        }
    }

    @DisplayName("이미 확정된 약속이 있으면 다른 약속을 확정할 수 없다.")
    @Test
    void givenConfirmedMeetingExists_whenConfirm_thenThrowMeetingException() {
        // given
        Long meetingId = 1L;
        Long gatheringLeaderId = 10L;
        given(meetingValidator.findMeetingOrThrow(meetingId)).willReturn(meeting);
        given(meetingRepository.existsByGatheringIdAndMeetingStatus(gathering.getId(), MeetingStatus.CONFIRMED))
                .willReturn(true);

        try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(gatheringLeaderId);

            // when + then
            assertThatThrownBy(() -> meetingService.confirmMeeting(meetingId))
                    .isInstanceOf(MeetingException.class)
                    .extracting("errorCode")
                    .isEqualTo(MeetingErrorCode.INVALID_MEETING_STATUS_CHANGE);
        }
    }

    @DisplayName("모임장이 아닌 사용자는 약속 상태를 변경할 수 없다.")
    @Test
    void givenNotGatheringLeader_whenChangeStatus_thenThrowGatheringException() {
        // given
        Long meetingId = 1L;
        Long notLeaderId = 99L;
        given(meetingValidator.findMeetingOrThrow(meetingId)).willReturn(meeting);
        doThrow(new GatheringException(GatheringErrorCode.NOT_GATHERING_LEADER))
                .when(gatheringValidator).validateLeader(gathering.getId(), notLeaderId);

        try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(notLeaderId);

            // when + then
            assertThatThrownBy(() -> meetingService.confirmMeeting(meetingId))
                    .isInstanceOf(GatheringException.class)
                    .extracting("errorCode")
                    .isEqualTo(GatheringErrorCode.NOT_GATHERING_LEADER);
        }
    }

    @DisplayName("약속장이 없으면 약속 확정이 실패한다.")
    @Test
    void givenMissingLeader_whenConfirm_thenThrowMeetingException() {
        // given
        Long meetingId = 1L;
        Long gatheringLeaderId = 10L;
        Meeting missingLeaderMeeting = Meeting.builder()
                .id(meetingId)
                .meetingName("Meeting 1")
                .meetingStatus(MeetingStatus.PENDING)
                .gathering(gathering)
                .build();
        given(meetingValidator.findMeetingOrThrow(meetingId)).willReturn(missingLeaderMeeting);
        given(meetingRepository.existsByGatheringIdAndMeetingStatus(gathering.getId(), MeetingStatus.CONFIRMED))
                .willReturn(false);

        try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(gatheringLeaderId);

            // when + then
            assertThatThrownBy(() -> meetingService.confirmMeeting(meetingId))
                    .isInstanceOf(MeetingException.class)
                    .extracting("errorCode")
                    .isEqualTo(MeetingErrorCode.INVALID_MEETING_STATUS_CHANGE);
        }
    }

    @DisplayName("확정된 약속은 거절할 수 없다.")
    @Test
    void givenConfirmedMeeting_whenReject_thenThrowMeetingException() {
        // given
        Long meetingId = 1L;
        Long gatheringLeaderId = 10L;
        Meeting confirmedMeeting = Meeting.builder()
                .id(meetingId)
                .meetingName("Meeting 1")
                .meetingStatus(MeetingStatus.CONFIRMED)
                .gathering(gathering)
                .meetingLeader(leader)
                .build();
        given(meetingValidator.findMeetingOrThrow(meetingId)).willReturn(confirmedMeeting);

        try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(gatheringLeaderId);

            // when + then
            assertThatThrownBy(() -> meetingService.rejectMeeting(meetingId))
                    .isInstanceOf(MeetingException.class)
                    .extracting("errorCode")
                    .isEqualTo(MeetingErrorCode.INVALID_MEETING_STATUS_CHANGE);
        }
    }

    @DisplayName("약속 참가 신청을 한다.")
    @Test
    void givenMeetingId_whenMeetingJoin_thenMeetingId() {
        // given
        Long meetingId = 3L;
        Long userId = 7L;
        Meeting meeting = Meeting.builder()
                .id(meetingId)
                .gathering(Gathering.builder()
                        .id(1L)
                        .gatheringName("gathering")
                        .invitationLink("link")
                        .build())
                .book(sampleBook())
                .build();
        User user = User.builder()
                .id(userId)
                .nickname("member")
                .build();

        given(meetingValidator.findMeetingOrThrow(meetingId))
                .willReturn(meeting);
        given(meetingMemberRepository.findAnyByMeetingIdAndUserId(meetingId, userId))
                .willReturn(Optional.empty());
        given(userValidator.findUserOrThrow(userId))
                .willReturn(user);

        try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

            // when
            Long response = meetingService.joinMeeting(meetingId);

            // then
            assertThat(response).isEqualTo(meetingId);
            verify(meetingValidator).validateCapacity(meetingId, meeting.getMaxParticipants());
            verify(meetingMemberRepository).save(any());
        }

    }

    @DisplayName("이미 약속 멤버면 참가 신청이 실패한다.")
    @Test
    void givenExistingMember_whenJoinMeeting_thenThrowException() {
        // given
        Long meetingId = 3L;
        Long userId = 7L;
        Meeting meeting = Meeting.builder()
                .id(meetingId)
                .gathering(Gathering.builder()
                        .id(1L)
                        .gatheringName("gathering")
                        .invitationLink("link")
                        .build())
                .book(sampleBook())
                .build();

        given(meetingValidator.findMeetingOrThrow(meetingId))
                .willReturn(meeting);
        given(meetingMemberRepository.findAnyByMeetingIdAndUserId(meetingId, userId))
                .willReturn(Optional.of(MeetingMember.builder()
                        .meeting(meeting)
                        .user(User.builder().id(userId).build())
                        .build()));

        try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

            // when
            // then
            assertThatThrownBy(() -> meetingService.joinMeeting(meetingId))
                    .isInstanceOf(MeetingException.class)
                    .extracting("errorCode")
                    .isEqualTo(MeetingErrorCode.MEETING_ALREADY_JOINED);
            verify(meetingValidator, never()).validateCapacity(any(), any());
            verify(userValidator, never()).findUserOrThrow(any());
            verify(meetingMemberRepository, never()).save(any());
        }
    }

    @DisplayName("취소 이력이 있는 멤버는 재참여 처리된다.")
    @Test
    void givenCanceledMember_whenJoinMeeting_thenRestore() {
        // given
        Long meetingId = 3L;
        Long userId = 7L;
        Integer maxParticipants = 5;
        Meeting meeting = Meeting.builder()
                .id(meetingId)
                .maxParticipants(maxParticipants)
                .gathering(Gathering.builder()
                        .id(1L)
                        .gatheringName("gathering")
                        .invitationLink("link")
                        .build())
                .book(sampleBook())
                .build();
        MeetingMember canceledMember = MeetingMember.builder()
                .meeting(meeting)
                .user(User.builder().id(userId).build())
                .canceledAt(LocalDateTime.now().minusDays(1))
                .build();

        given(meetingValidator.findMeetingOrThrow(meetingId))
                .willReturn(meeting);
        given(meetingMemberRepository.findAnyByMeetingIdAndUserId(meetingId, userId))
                .willReturn(Optional.of(canceledMember));

        try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

            // when
            Long response = meetingService.joinMeeting(meetingId);

            // then
            assertThat(response).isEqualTo(meetingId);
            assertThat(canceledMember.getCanceledAt()).isNull();
            verify(meetingValidator).validateCapacity(meetingId, maxParticipants);
            verify(userValidator, never()).findUserOrThrow(any());
            verify(meetingMemberRepository, never()).save(any());
        }
    }

    @DisplayName("모임 멤버가 아니면 약속 참가 신청에 실패한다.")
    @Test
    void givenNotGatheringMember_whenJoinMeeting_thenThrowException() {
        // given
        Long meetingId = 3L;
        Long userId = 7L;
        Meeting meeting = Meeting.builder()
                .id(meetingId)
                .gathering(Gathering.builder()
                        .id(1L)
                        .gatheringName("gathering")
                        .invitationLink("link")
                        .build())
                .book(sampleBook())
                .build();

        given(meetingValidator.findMeetingOrThrow(meetingId))
                .willReturn(meeting);
        doThrow(new GatheringException(GatheringErrorCode.NOT_GATHERING_MEMBER))
                .when(gatheringValidator).validateMembership(meeting.getGathering().getId(), userId);

        try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

            // when + then
            assertThatThrownBy(() -> meetingService.joinMeeting(meetingId))
                    .isInstanceOf(GatheringException.class)
                    .extracting("errorCode")
                    .isEqualTo(GatheringErrorCode.NOT_GATHERING_MEMBER);
        }
    }

    @DisplayName("약속 정원이 마감되면 참가 신청에 실패한다.")
    @Test
    void givenFullMeeting_whenJoinMeeting_thenThrowException() {
        // given
        Long meetingId = 3L;
        Long userId = 7L;
        Meeting meeting = Meeting.builder()
                .id(meetingId)
                .maxParticipants(2)
                .gathering(Gathering.builder()
                        .id(1L)
                        .gatheringName("gathering")
                        .invitationLink("link")
                        .build())
                .book(sampleBook())
                .build();

        given(meetingValidator.findMeetingOrThrow(meetingId))
                .willReturn(meeting);
        given(meetingMemberRepository.findAnyByMeetingIdAndUserId(meetingId, userId))
                .willReturn(Optional.empty());
        doThrow(new MeetingException(MeetingErrorCode.MEETING_FULL))
                .when(meetingValidator).validateCapacity(meetingId, meeting.getMaxParticipants());

        try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

            // when + then
            assertThatThrownBy(() -> meetingService.joinMeeting(meetingId))
                    .isInstanceOf(MeetingException.class)
                    .extracting("errorCode")
                    .isEqualTo(MeetingErrorCode.MEETING_FULL);
        }
    }

    @DisplayName("약속 시작 24시간 이내면 참가 신청에 실패한다.")
    @Test
    void givenMeetingWithin24Hours_whenJoinMeeting_thenThrowException() {
        // given
        Long meetingId = 3L;
        Long userId = 7L;
        Meeting meeting = Meeting.builder()
                .id(meetingId)
                .meetingStartDate(LocalDateTime.now().plusHours(1))
                .gathering(Gathering.builder()
                        .id(1L)
                        .gatheringName("gathering")
                        .invitationLink("link")
                        .build())
                .book(sampleBook())
                .build();

        given(meetingValidator.findMeetingOrThrow(meetingId))
                .willReturn(meeting);

        try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

            // when + then
            assertThatThrownBy(() -> meetingService.joinMeeting(meetingId))
                    .isInstanceOf(MeetingException.class)
                    .extracting("errorCode")
                    .isEqualTo(MeetingErrorCode.MEETING_JOIN_NOT_ALLOWED);
            verify(gatheringValidator, never()).validateMembership(any(), any());
            verify(meetingValidator, never()).validateCapacity(any(), any());
            verify(userValidator, never()).findUserOrThrow(any());
            verify(meetingMemberRepository, never()).save(any());
        }
    }

    @DisplayName("약속 참가 신청을 취소할 수 있다.")
    @Test
    void givenMeetingId_whenMeetingCancel_thenSuccess() {
        // given
        Long userId = 7L;
        Meeting meeting = Meeting.builder()
                .id(meetingId)
                .meetingStartDate(LocalDateTime.now().plusDays(2))
                .book(sampleBook())
                .gathering(gathering)
                .build();
        MeetingMember meetingMember = MeetingMember.builder()
                .meeting(meeting)
                .user(User.builder().id(userId).build())
                .build();

        given(meetingValidator.findMeetingOrThrow(meetingId)).willReturn(meeting);
        given(meetingValidator.getAnyMeetingMember(meetingId, userId))
                .willReturn(meetingMember);

        try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

            // when
            Long response = meetingService.cancelMeeting(meetingId);

            // then
            assertThat(response).isEqualTo(meetingId);
            assertThat(meetingMember.getCanceledAt()).isNotNull();
            verify(topicRepository).softDeleteByMeetingIdAndProposedById(meetingId, userId);
        }

    }

    @DisplayName("약속에 참가하지 않은 사람은 취소할 수 없다.")
    @Test
    void givenMeetingId_whenMeetingCancel_thenException() {
        // given
        Long userId = 7L;
        Meeting meeting = Meeting.builder()
                .id(meetingId)
                .meetingStartDate(LocalDateTime.now().plusDays(2))
                .book(sampleBook())
                .gathering(gathering)
                .build();

        given(meetingValidator.findMeetingOrThrow(meetingId)).willReturn(meeting);
        given(meetingValidator.getAnyMeetingMember(meetingId, userId))
                .willThrow(new MeetingException(MeetingErrorCode.NOT_MEETING_MEMBER));

        try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

            // when + then
            assertThatThrownBy(() -> meetingService.cancelMeeting(meetingId))
                    .isInstanceOf(MeetingException.class)
                    .extracting("errorCode")
                    .isEqualTo(MeetingErrorCode.NOT_MEETING_MEMBER);
        }

    }

    @DisplayName("신청 마감 기한 전까지만 취소 가능하다.")
    @Test
    void givenMeetingId_whenMeetingCancel_thenThrowException() {
        // given
        Long userId = 7L;
        Meeting meeting = Meeting.builder()
                .id(meetingId)
                .meetingStartDate(LocalDateTime.now().plusHours(1))
                .book(sampleBook())
                .gathering(gathering)
                .build();

        given(meetingValidator.findMeetingOrThrow(meetingId)).willReturn(meeting);

        try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

            // when + then
            assertThatThrownBy(() -> meetingService.cancelMeeting(meetingId))
                    .isInstanceOf(MeetingException.class)
                    .extracting("errorCode")
                    .isEqualTo(MeetingErrorCode.MEETING_CANCEL_NOT_ALLOWED);
        }

    }

    @DisplayName("주제를 등록했던 사람이 참가 취소하면 주제까지 삭제된다.")
    @Test
    void givenMeetingId_whenMeetingCancel_thenDeleteWithTopics() {
        // given
        Long userId = 7L;
        Meeting meeting = Meeting.builder()
                .id(meetingId)
                .meetingStartDate(LocalDateTime.now().plusDays(2))
                .book(sampleBook())
                .gathering(gathering)
                .build();
        MeetingMember meetingMember = MeetingMember.builder()
                .meeting(meeting)
                .user(User.builder().id(userId).build())
                .build();

        given(meetingValidator.findMeetingOrThrow(meetingId)).willReturn(meeting);
        given(meetingValidator.getAnyMeetingMember(meetingId, userId))
                .willReturn(meetingMember);

        try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

            // when
            meetingService.cancelMeeting(meetingId);

            // then
            verify(topicRepository).softDeleteByMeetingIdAndProposedById(meetingId, userId);
        }
    }

    @DisplayName("진행 전의 약속만 수정 가능하다.")
    @Test
    void givenMeetingUpdateRequest_whenMeetingUpdate_thenSuccess() {
        // given
        LocalDateTime endDate = meeting.getMeetingStartDate().plusHours(2);
        MeetingUpdateRequest request = MeetingUpdateRequest.builder()
                .meetingName("약속명 변경")
                .location(new MeetingLocationDto(
                        "장소 변경",
                        "서울 어딘가",
                        37.0,
                        127.0
                ))
                .endDate(endDate)
                .build();

        given(meetingValidator.findMeetingOrThrow(meetingId)).willReturn(meeting);

        // when
        try (MockedStatic<SecurityUtil> mock = mockStatic(SecurityUtil.class)) {
            mock.when(SecurityUtil::getCurrentUserId).thenReturn(1L);
            MeetingUpdateResponse response = meetingService.updateMeeting(meetingId, request);

            // then
            assertThat(response.meetingName()).isEqualTo(request.meetingName());
            assertThat(response.endDate()).isEqualTo(request.endDate());
        }
    }

    @DisplayName("종료된 약속은 수정할 수 없다.")
    @Test
    void givenDoneMeeting_whenUpdateMeeting_thenThrowException() {
        // given
        Meeting doneMeeting = Meeting.builder()
                .id(meetingId)
                .meetingName("Meeting 1")
                .meetingStatus(MeetingStatus.DONE)
                .meetingLeader(leader)
                .gathering(gathering)
                .build();
        MeetingUpdateRequest request = MeetingUpdateRequest.builder()
                .meetingName("약속명 변경")
                .build();

        given(meetingValidator.findMeetingOrThrow(meetingId)).willReturn(doneMeeting);

        // when + then
        try (MockedStatic<SecurityUtil> mock = mockStatic(SecurityUtil.class)) {
            mock.when(SecurityUtil::getCurrentUserId).thenReturn(leader.getId());

            assertThatThrownBy(() -> meetingService.updateMeeting(meetingId, request))
                    .isInstanceOf(MeetingException.class)
                    .extracting("errorCode")
                    .isEqualTo(MeetingErrorCode.INVALID_MEETING_STATUS_CHANGE);
        }
    }

    @DisplayName("종료 일시는 시작 일시보다 이전일 수 없다.")
    @Test
    void givenInvalidDates_whenUpdateMeeting_thenThrowException() {
        // given
        LocalDateTime startDate = LocalDateTime.now().plusDays(2);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);
        MeetingUpdateRequest request = MeetingUpdateRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .build();

        given(meetingValidator.findMeetingOrThrow(meetingId)).willReturn(meeting);

        // when + then
        try (MockedStatic<SecurityUtil> mock = mockStatic(SecurityUtil.class)) {
            mock.when(SecurityUtil::getCurrentUserId).thenReturn(leader.getId());

            assertThatThrownBy(() -> meetingService.updateMeeting(meetingId, request))
                    .isInstanceOf(MeetingException.class)
                    .extracting("errorCode")
                    .isEqualTo(MeetingErrorCode.INVALID_MEETING_STATUS_CHANGE);
        }
    }

    @DisplayName("현재 참여 인원보다 적게 최대 참여 인원을 수정할 수 없다.")
    @Test
    void givenMaxParticipantsLessThanCurrent_whenUpdateMeeting_thenThrowException() {
        // given
        MeetingUpdateRequest request = MeetingUpdateRequest.builder()
                .maxParticipants(1)
                .build();

        given(meetingValidator.findMeetingOrThrow(meetingId)).willReturn(meeting);
        given(gatheringMemberRepository.countByGatheringIdAndRemovedAtIsNull(gathering.getId()))
                .willReturn(5);
        given(meetingValidator.countActiveMembers(meetingId)).willReturn(2);

        // when + then
        try (MockedStatic<SecurityUtil> mock = mockStatic(SecurityUtil.class)) {
            mock.when(SecurityUtil::getCurrentUserId).thenReturn(leader.getId());

            assertThatThrownBy(() -> meetingService.updateMeeting(meetingId, request))
                    .isInstanceOf(MeetingException.class)
                    .extracting("errorCode")
                    .isEqualTo(MeetingErrorCode.MAX_PARTICIPANTS_LESS_THAN_CURRENT);
        }
    }

    @DisplayName("약속 시작 24시간 이내면 약속을 수정할 수 없다.")
    @Test
    void givenMeetingWithin24Hours_whenUpdateMeeting_thenThrowException() {
        // given
        Meeting meeting = Meeting.builder()
                .id(meetingId)
                .meetingName("Meeting 1")
                .meetingStatus(MeetingStatus.PENDING)
                .meetingStartDate(LocalDateTime.now().plusHours(1))
                .meetingLeader(leader)
                .gathering(gathering)
                .build();
        MeetingUpdateRequest request = MeetingUpdateRequest.builder()
                .meetingName("약속명 변경")
                .build();

        given(meetingValidator.findMeetingOrThrow(meetingId)).willReturn(meeting);

        // when + then
        try (MockedStatic<SecurityUtil> mock = mockStatic(SecurityUtil.class)) {
            mock.when(SecurityUtil::getCurrentUserId).thenReturn(leader.getId());

            assertThatThrownBy(() -> meetingService.updateMeeting(meetingId, request))
                    .isInstanceOf(MeetingException.class)
                    .extracting("errorCode")
                    .isEqualTo(MeetingErrorCode.MEETING_UPDATE_NOT_ALLOWED);
            verify(meetingValidator, never()).countActiveMembers(any());
        }
    }

    @DisplayName("모임 약속 리스트(전체)를 조회하면 아이템과 참여 여부를 반환한다.")
    @Test
    void givenGatheringIdAndAllFilter_whenGetMeetingList_thenReturnItems() {
        // given
        Long gatheringId = 100L;
        Long userId = 55L;
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        Book book1 = Book.builder().id(1L).bookName("book1").build();
        Book book2 = Book.builder().id(2L).bookName("book2").build();
        Meeting meeting1 = Meeting.builder()
                .id(1L)
                .meetingName("meeting1")
                .meetingStatus(MeetingStatus.CONFIRMED)
                .meetingStartDate(LocalDateTime.now())
                .meetingEndDate(LocalDateTime.now().plusHours(2))
                .gathering(gathering)
                .book(book1)
                .build();
        Meeting meeting2 = Meeting.builder()
                .id(2L)
                .meetingName("meeting2")
                .meetingStatus(MeetingStatus.CONFIRMED)
                .meetingStartDate(LocalDateTime.now().plusDays(1))
                .meetingEndDate(LocalDateTime.now().plusDays(1).plusHours(2))
                .gathering(gathering)
                .book(book2)
                .build();

        List<Meeting> meetings = List.of(meeting1, meeting2);
        Page<Meeting> meetingPage = new PageImpl<>(meetings, pageable, meetings.size());
        given(meetingRepository.findByGatheringIdAndMeetingStatus(
                eq(gatheringId),
                eq(MeetingStatus.CONFIRMED),
                any()
        )).willReturn(meetingPage);
        given(topicRepository.findTopicTypesByMeetingIds(List.of(1L, 2L)))
                .willReturn(List.of(
                        new Object[]{1L, TopicType.FREE},
                        new Object[]{1L, TopicType.DISCUSSION},
                        new Object[]{2L, TopicType.EMOTION}
                ));
        given(meetingMemberRepository.findActiveMeetingIdsByUserIdAndGatheringId(userId, gatheringId))
                .willReturn(List.of(1L));

        try (MockedStatic<SecurityUtil> mock = mockStatic(SecurityUtil.class)) {
            mock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

            // when
            PageResponse<MeetingListItemResponse> response =
                    meetingService.meetingList(gatheringId, MeetingListFilter.ALL, pageable);

            // then
            assertThat(response.items()).hasSize(2);
            assertThat(response.pageSize()).isEqualTo(10);
            assertThat(response.currentPage()).isEqualTo(0);
            assertThat(response.totalPages()).isEqualTo(1);
            MeetingListItemResponse item1 = response.items().stream()
                    .filter(item -> item.meetingId().equals(1L))
                    .findFirst()
                    .orElseThrow();
            assertThat(item1.joined()).isTrue();
            assertThat(item1.topicTypes()).containsExactlyInAnyOrder(TopicType.FREE, TopicType.DISCUSSION);
            assertThat(item1.myRole()).isEqualTo(MeetingMyRole.MEMBER);

            MeetingListItemResponse item2 = response.items().stream()
                    .filter(item -> item.meetingId().equals(2L))
                    .findFirst()
                    .orElseThrow();
            assertThat(item2.joined()).isFalse();
            assertThat(item2.topicTypes()).containsExactly(TopicType.EMOTION);
            assertThat(item2.myRole()).isEqualTo(MeetingMyRole.NONE);
        }
    }

    @DisplayName("모임장 약속 승인 리스트(PENDING)를 조회하면 아이템을 반환한다.")
    @Test
    void givenLeaderAndPending_whenGetApprovalMeetingList_thenReturnItems() {
        // given
        Long gatheringId = 100L;
        Long userId = leader.getId();
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        Book book = Book.builder().id(1L).bookName("book1").build();
        Meeting pendingMeeting = Meeting.builder()
                .id(1L)
                .meetingName("meeting1")
                .meetingStatus(MeetingStatus.PENDING)
                .meetingStartDate(LocalDateTime.now())
                .meetingEndDate(LocalDateTime.now().plusHours(2))
                .gathering(gathering)
                .book(book)
                .build();

        Page<Meeting> meetingPage = new PageImpl<>(List.of(pendingMeeting), pageable, 1);
        given(meetingRepository.findByGatheringIdAndMeetingStatus(eq(gatheringId), eq(MeetingStatus.PENDING), any()))
                .willReturn(meetingPage);
        given(topicRepository.findTopicTypesByMeetingIds(List.of(1L)))
                .willReturn(List.of());
        given(meetingMemberRepository.findActiveMeetingIdsByUserIdAndGatheringId(userId, gatheringId))
                .willReturn(List.of());

        try (MockedStatic<SecurityUtil> mock = mockStatic(SecurityUtil.class)) {
            mock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

            // when
            PageResponse<MeetingListItemResponse> response = meetingService.getApprovalMeetingList(
                    gatheringId,
                    MeetingStatus.PENDING,
                    pageable
            );

            // then
            assertThat(response.items()).hasSize(1);
            assertThat(response.items().get(0).meetingId()).isEqualTo(1L);
            verify(gatheringValidator).validateLeader(gatheringId, userId);
        }
    }

    @DisplayName("모임장이 아니면 약속 승인 리스트 조회가 실패한다.")
    @Test
    void givenNotLeader_whenGetApprovalMeetingList_thenThrowGatheringException() {
        // given
        Long gatheringId = 100L;
        Long userId = 77L;
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        doThrow(new GatheringException(GatheringErrorCode.NOT_GATHERING_LEADER))
                .when(gatheringValidator).validateLeader(gatheringId, userId);

        try (MockedStatic<SecurityUtil> mock = mockStatic(SecurityUtil.class)) {
            mock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

            // when + then
            assertThatThrownBy(() -> meetingService.getApprovalMeetingList(
                    gatheringId,
                    MeetingStatus.PENDING,
                    pageable
            ))
                    .isInstanceOf(GatheringException.class)
                    .extracting("errorCode")
                    .isEqualTo(GatheringErrorCode.NOT_GATHERING_LEADER);
        }
    }

    @DisplayName("승인 리스트는 PENDING 또는 CONFIRMED만 조회할 수 있다.")
    @Test
    void givenInvalidStatus_whenGetApprovalMeetingList_thenThrowMeetingException() {
        // given
        Long gatheringId = 100L;
        Long userId = leader.getId();
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);

        try (MockedStatic<SecurityUtil> mock = mockStatic(SecurityUtil.class)) {
            mock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

            // when + then
            assertThatThrownBy(() -> meetingService.getApprovalMeetingList(
                    gatheringId,
                    MeetingStatus.DONE,
                    pageable
            ))
                    .isInstanceOf(MeetingException.class)
                    .extracting("errorCode")
                    .isEqualTo(MeetingErrorCode.INVALID_MEETING_STATUS_CHANGE);

            assertThatThrownBy(() -> meetingService.getApprovalMeetingList(
                    gatheringId,
                    MeetingStatus.REJECTED,
                    pageable
            ))
                    .isInstanceOf(MeetingException.class)
                    .extracting("errorCode")
                    .isEqualTo(MeetingErrorCode.INVALID_MEETING_STATUS_CHANGE);
        }
    }

    @DisplayName("약속 탭 카운트를 조회하면 각 탭의 개수를 반환한다.")
    @Test
    void givenGatheringId_whenGetMeetingTabCounts_thenReturnCounts() {
        // given
        Long gatheringId = 100L;
        Long userId = 55L;
        given(meetingRepository.countByGatheringIdAndMeetingStatus(gatheringId, MeetingStatus.CONFIRMED))
                .willReturn(3);
        given(meetingRepository.countByGatheringIdAndMeetingStatus(gatheringId, MeetingStatus.DONE))
                .willReturn(2);
        given(meetingRepository.countUpcomingMeetings(eq(gatheringId), eq(MeetingStatus.CONFIRMED), any(), any()))
                .willReturn(1);
        given(meetingMemberRepository.countMeetingsByUserIdAndStatus(userId, gatheringId, MeetingStatus.DONE))
                .willReturn(1);

        try (MockedStatic<SecurityUtil> mock = mockStatic(SecurityUtil.class)) {
            mock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

            // when
            MeetingTabCountsResponse response = meetingService.getMeetingTabCounts(gatheringId);

            // then
            assertThat(response.all()).isEqualTo(3);
            assertThat(response.upcoming()).isEqualTo(1);
            assertThat(response.done()).isEqualTo(2);
            assertThat(response.joined()).isEqualTo(1);
            verify(meetingRepository).countUpcomingMeetings(
                    eq(gatheringId),
                    eq(MeetingStatus.CONFIRMED),
                    any(LocalDateTime.class),
                    any(LocalDateTime.class)
            );
        }
    }

    @DisplayName("내 약속 리스트 조회 시 필터가 null이면 전체 기준으로 조회한다.")
    @Test
    void givenNullFilter_whenGetMyMeetingList_thenReturnItems() {
        // given
        Long userId = leader.getId();
        int size = 4;
        User meetingLeader = User.builder().id(99L).nickname("meetingLeader").build();
        Meeting myMeeting = Meeting.builder()
                .id(1L)
                .meetingName("myMeeting")
                .meetingStatus(MeetingStatus.CONFIRMED)
                .meetingStartDate(null)
                .meetingEndDate(null)
                .meetingLeader(meetingLeader)
                .gathering(gathering)
                .book(Book.builder().id(1L).bookName("book").build())
                .build();

        given(meetingMemberRepository.findMyMeetingsByStatusesAfterCursor(
                eq(userId),
                eq(List.of(MeetingStatus.CONFIRMED, MeetingStatus.DONE)),
                any(),
                any(),
                any()
        )).willReturn(List.of(myMeeting));

        try (MockedStatic<SecurityUtil> mock = mockStatic(SecurityUtil.class)) {
            mock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

            // when
            CursorResponse<MyMeetingListItemResponse, MeetingListCursor> response =
                    meetingService.getMyMeetingList(null, size, null);

            // then
            assertThat(response.items()).hasSize(1);
            MyMeetingListItemResponse item = response.items().get(0);
            assertThat(item.myRole()).isEqualTo(MeetingMyRole.GATHERING_LEADER);
            assertThat(item.progressStatus()).isEqualTo(MeetingProgressStatus.UNKNOWN);
        }
    }

    @DisplayName("다가오는 내 약속 리스트를 조회하면 UPCOMING 상태를 반환한다.")
    @Test
    void givenUpcomingFilter_whenGetMyMeetingList_thenReturnUpcomingItems() {
        // given
        Long userId = 55L;
        int size = 4;
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = start.plusHours(2);
        Meeting upcomingMeeting = Meeting.builder()
                .id(2L)
                .meetingName("upcoming")
                .meetingStatus(MeetingStatus.CONFIRMED)
                .meetingStartDate(start)
                .meetingEndDate(end)
                .meetingLeader(leader)
                .gathering(gathering)
                .book(Book.builder().id(2L).bookName("book2").build())
                .build();

        given(meetingMemberRepository.findMyUpcomingMeetingsAfterCursor(
                eq(userId),
                eq(MeetingStatus.CONFIRMED),
                any(),
                any(),
                any(),
                any(),
                any()
        )).willReturn(List.of(upcomingMeeting));

        try (MockedStatic<SecurityUtil> mock = mockStatic(SecurityUtil.class)) {
            mock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

            // when
            CursorResponse<MyMeetingListItemResponse, MeetingListCursor> response =
                    meetingService.getMyMeetingList(MyMeetingListFilter.UPCOMING, size, null);

            // then
            assertThat(response.items()).hasSize(1);
            MyMeetingListItemResponse item = response.items().get(0);
            assertThat(item.progressStatus()).isEqualTo(MeetingProgressStatus.UPCOMING);
        }
    }

    @DisplayName("내 약속 탭 카운트를 조회하면 전체/다가오는/완료 카운트를 반환한다.")
    @Test
    void givenUser_whenGetMyMeetingTabCounts_thenReturnCounts() {
        // given
        Long userId = 55L;
        given(meetingMemberRepository.countMyMeetingsByStatuses(
                userId,
                List.of(MeetingStatus.CONFIRMED, MeetingStatus.DONE)
        )).willReturn(5);
        given(meetingMemberRepository.countMyUpcomingMeetings(
                eq(userId),
                eq(MeetingStatus.CONFIRMED),
                any(),
                any()
        )).willReturn(2);
        given(meetingMemberRepository.countMyMeetingsByStatus(userId, MeetingStatus.DONE))
                .willReturn(3);

        try (MockedStatic<SecurityUtil> mock = mockStatic(SecurityUtil.class)) {
            mock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

            // when
            MyMeetingTabCountsResponse response = meetingService.getMyMeetingTabCounts();

            // then
            assertThat(response.all()).isEqualTo(5);
            assertThat(response.upcoming()).isEqualTo(2);
            assertThat(response.done()).isEqualTo(3);
        }
    }

}
