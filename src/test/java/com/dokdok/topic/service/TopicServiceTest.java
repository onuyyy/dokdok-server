package com.dokdok.topic.service;

import com.dokdok.gathering.exception.GatheringErrorCode;
import com.dokdok.gathering.exception.GatheringException;
import com.dokdok.gathering.service.GatheringValidator;
import com.dokdok.global.exception.GlobalErrorCode;
import com.dokdok.global.exception.GlobalException;
import com.dokdok.global.util.SecurityUtil;
import com.dokdok.meeting.entity.Meeting;
import com.dokdok.meeting.entity.MeetingMember;
import com.dokdok.meeting.entity.MeetingStatus;
import com.dokdok.meeting.exception.MeetingErrorCode;
import com.dokdok.meeting.exception.MeetingException;
import com.dokdok.meeting.service.MeetingValidator;
import com.dokdok.topic.dto.request.ConfirmTopicsRequest;
import com.dokdok.topic.dto.request.SuggestTopicRequest;
import com.dokdok.topic.dto.response.ConfirmTopicsResponse;
import com.dokdok.topic.dto.response.ConfirmedTopicsResponse;
import com.dokdok.topic.dto.response.SuggestTopicResponse;
import com.dokdok.topic.dto.response.TopicsWithActionsResponse;
import com.dokdok.topic.dto.response.TopicLikeResponse;
import com.dokdok.topic.entity.Topic;
import com.dokdok.topic.entity.TopicLike;
import com.dokdok.topic.entity.TopicStatus;
import com.dokdok.topic.entity.TopicType;
import com.dokdok.topic.exception.TopicErrorCode;
import com.dokdok.topic.exception.TopicException;
import com.dokdok.topic.repository.TopicAnswerRepository;
import com.dokdok.topic.repository.TopicLikeRepository;
import com.dokdok.topic.repository.TopicRepository;
import com.dokdok.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TopicService 테스트")
class TopicServiceTest {

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private TopicLikeRepository topicLikeRepository;

    @Mock
    private TopicAnswerRepository topicAnswerRepository;

    @Mock
    private MeetingValidator meetingValidator;

    @Mock
    private GatheringValidator gatheringValidator;

    @Mock
    private TopicValidator topicValidator;

    @InjectMocks
    private TopicService topicService;

    private User testUser;
    private Meeting testMeeting;
    private MeetingMember testMeetingMember;
    private Topic testTopic;
    private SuggestTopicRequest testRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .userName("김독서")
                .nickname("책벌레김")
                .userEmail("kim@example.com")
                .kakaoId(1001L)
                .build();

        testMeeting = Meeting.builder()
                .id(1L)
                .meetingName("클린 코드 1회차")
                .meetingStatus(MeetingStatus.CONFIRMED)
                .build();

        testMeetingMember = MeetingMember.builder()
                .id(1L)
                .meeting(testMeeting)
                .user(testUser)
                .attendanceStatus("ATTENDING")
                .build();

        testTopic = Topic.builder()
                .id(1L)
                .meeting(testMeeting)
                .proposedBy(testUser)
                .title("의미 있는 이름 짓기")
                .description("변수명, 함수명, 클래스명을 짓는 원칙에 대해 토론합니다.")
                .topicType(TopicType.DISCUSSION)
                .build();

        testRequest = new SuggestTopicRequest(
                "의미 있는 이름 짓기",
                "변수명, 함수명, 클래스명을 짓는 원칙에 대해 토론합니다.",
                TopicType.DISCUSSION
        );
    }

    @Test
    @DisplayName("기본 자유형 토픽이 없으면 생성한다")
    void createDefaultTopic_Success() {
        Meeting meeting = Meeting.builder()
                .id(10L)
                .meetingLeader(testUser)
                .build();

        given(topicRepository.countByMeetingIdAndDeletedAtIsNull(meeting.getId()))
                .willReturn(0L);

        topicService.createDefaultTopic(meeting);

        var captor = org.mockito.ArgumentCaptor.forClass(Topic.class);
        verify(topicRepository).save(captor.capture());
        Topic saved = captor.getValue();
        assertThat(saved.getMeeting()).isEqualTo(meeting);
        assertThat(saved.getProposedBy()).isEqualTo(testUser);
        assertThat(saved.getTopicType()).isEqualTo(TopicType.FREE);
        assertThat(saved.getTitle()).isEqualTo(TopicType.FREE.getDisplayName());
        assertThat(saved.getDescription()).isEqualTo(TopicType.FREE.getDescription());
    }

    @Test
    @DisplayName("이미 토픽이 있으면 기본 토픽을 생성하지 않는다")
    void createDefaultTopic_SkipWhenExists() {
        Meeting meeting = Meeting.builder()
                .id(10L)
                .meetingLeader(testUser)
                .build();

        given(topicRepository.countByMeetingIdAndDeletedAtIsNull(meeting.getId()))
                .willReturn(1L);

        topicService.createDefaultTopic(meeting);

        verify(topicRepository, never()).save(any());
    }

    @Test
    @DisplayName("약속장이 없으면 기본 토픽을 생성하지 않는다")
    void createDefaultTopic_SkipWhenLeaderMissing() {
        Meeting meeting = Meeting.builder()
                .id(10L)
                .meetingLeader(null)
                .build();

        given(topicRepository.countByMeetingIdAndDeletedAtIsNull(meeting.getId()))
                .willReturn(0L);

        topicService.createDefaultTopic(meeting);

        verify(topicRepository, never()).save(any());
    }

    @Test
    @DisplayName("선택한 주제를 확정 상태로 변경한다")
    void confirmTopics_Success() {
        Long gatheringId = 1L;
        Long meetingId = 1L;
        Long userId = 1L;
        List<Long> topicIds = List.of(12L, 13L);
        ConfirmTopicsRequest request = new ConfirmTopicsRequest(topicIds);

        Topic topic1 = Topic.builder()
                .id(12L)
                .meeting(testMeeting)
                .topicStatus(TopicStatus.PROPOSED)
                .build();
        Topic topic2 = Topic.builder()
                .id(13L)
                .meeting(testMeeting)
                .topicStatus(TopicStatus.CONFIRMED)
                .build();

        try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId)
                    .thenReturn(userId);

            doNothing().when(gatheringValidator)
                    .validateMembership(gatheringId, userId);
            doNothing().when(meetingValidator)
                    .validateMeetingInGathering(meetingId, gatheringId);
            given(topicRepository.findAllByIdInAndMeetingId(topicIds, meetingId))
                    .willReturn(List.of(topic1, topic2));

            ConfirmTopicsResponse response =
                    topicService.confirmTopics(gatheringId, meetingId, request);

            assertThat(response.meetingId()).isEqualTo(meetingId);
            assertThat(response.topicStatus()).isEqualTo(TopicStatus.CONFIRMED);
            assertThat(response.topics()).hasSize(2);
            assertThat(response.topics().get(0).topicId()).isEqualTo(12L);
            assertThat(response.topics().get(0).confirmOrder()).isEqualTo(1);
            assertThat(response.topics().get(1).topicId()).isEqualTo(13L);
            assertThat(response.topics().get(1).confirmOrder()).isEqualTo(2);
            assertThat(topic1.getTopicStatus()).isEqualTo(TopicStatus.CONFIRMED);
            assertThat(topic2.getTopicStatus()).isEqualTo(TopicStatus.CONFIRMED);
            assertThat(topic1.getConfirmOrder()).isEqualTo(1);
            assertThat(topic2.getConfirmOrder()).isEqualTo(2);

            verify(gatheringValidator).validateMembership(gatheringId, userId);
            verify(meetingValidator).validateMeetingInGathering(meetingId, gatheringId);
            verify(topicRepository).findAllByIdInAndMeetingId(topicIds, meetingId);
        }
    }

    @Test
    @DisplayName("선택한 주제가 존재하지 않으면 예외가 발생한다")
    void confirmTopics_ThrowsWhenTopicMissing() {
        Long gatheringId = 1L;
        Long meetingId = 1L;
        Long userId = 1L;
        List<Long> topicIds = List.of(12L, 13L);
        ConfirmTopicsRequest request = new ConfirmTopicsRequest(topicIds);

        Topic topic1 = Topic.builder()
                .id(12L)
                .meeting(testMeeting)
                .topicStatus(TopicStatus.PROPOSED)
                .build();

        try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId)
                    .thenReturn(userId);

            doNothing().when(gatheringValidator)
                    .validateMembership(gatheringId, userId);
            doNothing().when(meetingValidator)
                    .validateMeetingInGathering(meetingId, gatheringId);
            given(topicRepository.findAllByIdInAndMeetingId(topicIds, meetingId))
                    .willReturn(List.of(topic1));

            assertThatThrownBy(() ->
                    topicService.confirmTopics(gatheringId, meetingId, request))
                    .isInstanceOf(TopicException.class)
                    .hasFieldOrPropertyWithValue("errorCode", TopicErrorCode.TOPIC_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getConfirmedTopics - 확정 주제 조회")
    class GetConfirmedTopicsTest {

        @Test
        @DisplayName("확정된 주제를 순서대로 조회한다")
        void getConfirmedTopics_Success() {
            Long gatheringId = 1L;
            Long meetingId = 1L;
            Long userId = 1L;
            int pageSize = 10;

            Topic topic1 = Topic.builder()
                    .id(12L)
                    .meeting(testMeeting)
                    .proposedBy(testUser)
                    .title("첫 번째 주제")
                    .description("첫 번째 설명")
                    .topicType(TopicType.DISCUSSION)
                    .topicStatus(TopicStatus.CONFIRMED)
                    .confirmOrder(1)
                    .build();
            Topic topic2 = Topic.builder()
                    .id(13L)
                    .meeting(testMeeting)
                    .proposedBy(testUser)
                    .title("두 번째 주제")
                    .description("두 번째 설명")
                    .topicType(TopicType.DISCUSSION)
                    .topicStatus(TopicStatus.CONFIRMED)
                    .confirmOrder(2)
                    .build();

            try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
                securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

                doNothing().when(gatheringValidator).validateMembership(gatheringId, userId);
                doNothing().when(meetingValidator).validateMeetingInGathering(meetingId, gatheringId);
                given(topicRepository.findConfirmedTopicsFirstPage(eq(meetingId), any(Pageable.class)))
                        .willReturn(List.of(topic1, topic2));
                given(topicRepository.countByMeetingIdAndTopicStatusAndDeletedAtIsNull(meetingId, TopicStatus.CONFIRMED))
                        .willReturn(2L);
                given(topicAnswerRepository.existsByMeetingIdAndUserId(meetingId, userId))
                        .willReturn(false);
                given(meetingValidator.findMeetingOrThrow(meetingId)).willReturn(testMeeting);

                ConfirmedTopicsResponse response =
                        topicService.getConfirmedTopics(gatheringId, meetingId, pageSize, null, null);

                assertThat(response.page().items()).hasSize(2);
                assertThat(response.page().items().get(0).topicId()).isEqualTo(12L);
                assertThat(response.page().items().get(0).topicType()).isEqualTo(TopicType.DISCUSSION);
                assertThat(response.page().items().get(1).confirmOrder()).isEqualTo(2);
                assertThat(response.page().pageSize()).isEqualTo(pageSize);
                assertThat(response.page().hasNext()).isFalse();
                assertThat(response.page().nextCursor()).isNull();
                assertThat(response.page().totalCount()).isEqualTo(2);
                assertThat(response.actions().canViewPreOpinions()).isFalse();
                assertThat(response.actions().canWritePreOpinions()).isTrue();

                verify(gatheringValidator).validateMembership(gatheringId, userId);
                verify(meetingValidator).validateMeetingInGathering(meetingId, gatheringId);
                verify(topicRepository).findConfirmedTopicsFirstPage(eq(meetingId), any(Pageable.class));
            }
        }

        @Test
        @DisplayName("모임이 없으면 예외가 발생한다")
        void getConfirmedTopics_ThrowsWhenGatheringMissing() {
            Long gatheringId = 1L;
            Long meetingId = 1L;
            Long userId = 1L;
            int pageSize = 10;

            try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
                securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

                doThrow(new GatheringException(GatheringErrorCode.NOT_GATHERING_MEMBER))
                        .when(gatheringValidator).validateMembership(gatheringId, userId);

                assertThatThrownBy(() -> topicService.getConfirmedTopics(gatheringId, meetingId, pageSize, null, null))
                        .isInstanceOf(GatheringException.class)
                        .hasFieldOrPropertyWithValue("errorCode", GatheringErrorCode.NOT_GATHERING_MEMBER);

                verify(topicRepository, never())
                        .findConfirmedTopicsFirstPage(any(), any());
            }
        }
    }

    @Test
    @DisplayName("정상적으로 주제를 생성한다")
    void createTopic_Success() {
        Long gatheringId = 1L;
        Long meetingId = 1L;
        Long userId = 1L;

        try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
            // SecurityUtil에서 userId 가져오기
            securityUtilMock.when(SecurityUtil::getCurrentUserId)
                    .thenReturn(userId);

            // 모임 검증 통과
            doNothing().when(gatheringValidator)
                    .validateGathering(gatheringId);

            // 회차 소속 검증 통과
            doNothing().when(meetingValidator)
                    .validateMeetingInGathering(meetingId, gatheringId);

            // 회차 상태 검증 통과
            doNothing().when(meetingValidator)
                    .validateMeetingStatus(meetingId);

            // 회차 참석자 조회 성공
            given(meetingValidator.getMeetingMember(meetingId, userId))
                    .willReturn(testMeetingMember);

            // Topic 저장 성공
            given(topicRepository.save(any(Topic.class)))
                    .willReturn(testTopic);

            SuggestTopicResponse response =
                    topicService.createTopic(gatheringId, meetingId, testRequest);

            assertThat(response).isNotNull();
            assertThat(response.title()).isEqualTo("의미 있는 이름 짓기");
            assertThat(response.topicType()).isEqualTo(TopicType.DISCUSSION);
            assertThat(response.topicTypeLabel()).isEqualTo("토론형");
            assertThat(response.topicTypeDescription()).isEqualTo("찬반 토론이나 다양한 관점을 나누는 주제입니다.");
            assertThat(response.createdBy().userId()).isEqualTo(1L);
            assertThat(response.createdBy().nickname()).isEqualTo("책벌레김");

            verify(gatheringValidator).validateGathering(gatheringId);
            verify(meetingValidator).validateMeetingInGathering(meetingId, gatheringId);
            verify(meetingValidator).validateMeetingStatus(meetingId);
            verify(meetingValidator).getMeetingMember(meetingId, userId);
            verify(topicRepository).save(any(Topic.class));
        }
    }

    @Test
    @DisplayName("인증되지 않은 사용자인 경우 예외가 발생한다")
    void createTopic_Unauthorized_ThrowsException() {
        Long gatheringId = 1L;
        Long meetingId = 1L;

        try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId)
                    .thenThrow(new GlobalException(GlobalErrorCode.UNAUTHORIZED));

            assertThatThrownBy(() ->
                    topicService.createTopic(gatheringId, meetingId, testRequest))
                    .isInstanceOf(GlobalException.class)
                    .hasFieldOrPropertyWithValue("errorCode",
                            GlobalErrorCode.UNAUTHORIZED);

            verify(gatheringValidator, never()).validateGathering(any());
            verify(meetingValidator, never()).validateMeetingInGathering(any(), any());
            verify(meetingValidator, never()).getMeetingMember(any(), any());
            verify(topicRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("모임 멤버가 아닌 경우 예외가 발생한다")
    void createTopic_NotGatheringMember_ThrowsException() {
        Long gatheringId = 1L;
        Long meetingId = 1L;
        Long userId = 1L;

        try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId)
                    .thenReturn(userId);

            doThrow(new GatheringException(GatheringErrorCode.GATHERING_NOT_FOUND))
                    .when(gatheringValidator)
                    .validateGathering(gatheringId);

            assertThatThrownBy(() ->
                    topicService.createTopic(gatheringId, meetingId, testRequest))
                    .isInstanceOf(GatheringException.class)
                    .hasFieldOrPropertyWithValue("errorCode",
                            GatheringErrorCode.GATHERING_NOT_FOUND);

            verify(meetingValidator, never()).validateMeetingInGathering(any(), any());
            verify(meetingValidator, never()).getMeetingMember(any(), any());
            verify(topicRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("회차가 해당 모임에 속하지 않는 경우 예외가 발생한다")
    void createTopic_MeetingNotInGathering_ThrowsException() {
        Long gatheringId = 1L;
        Long meetingId = 999L;
        Long userId = 1L;

        try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId)
                    .thenReturn(userId);

            doNothing().when(gatheringValidator)
                    .validateGathering(gatheringId);

            doThrow(new MeetingException(MeetingErrorCode.NOT_GATHERING_MEETING))
                    .when(meetingValidator)
                    .validateMeetingInGathering(meetingId, gatheringId);

            assertThatThrownBy(() ->
                    topicService.createTopic(gatheringId, meetingId, testRequest))
                    .isInstanceOf(MeetingException.class)
                    .hasFieldOrPropertyWithValue("errorCode",
                            MeetingErrorCode.NOT_GATHERING_MEETING);

            verify(meetingValidator, never()).validateMeetingStatus(any());
            verify(meetingValidator, never()).getMeetingMember(any(), any());
            verify(topicRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("약속 상태가 CONFIRMED가 아니면 예외가 발생한다")
    void createTopic_MeetingStatusPending_ThrowsException() {
        Long gatheringId = 1L;
        Long meetingId = 1L;
        Long userId = 1L;

        try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId)
                    .thenReturn(userId);

            doNothing().when(gatheringValidator)
                    .validateGathering(gatheringId);

            doNothing().when(meetingValidator)
                    .validateMeetingInGathering(meetingId, gatheringId);

            doThrow(new MeetingException(MeetingErrorCode.MEETING_NOT_CONFIRMED))
                    .when(meetingValidator)
                    .validateMeetingStatus(meetingId);

            assertThatThrownBy(() ->
                    topicService.createTopic(gatheringId, meetingId, testRequest))
                    .isInstanceOf(MeetingException.class)
                    .hasFieldOrPropertyWithValue("errorCode",
                            MeetingErrorCode.MEETING_NOT_CONFIRMED);

            verify(meetingValidator, never()).getMeetingMember(any(), any());
            verify(topicRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("회차 참석자가 아닌 경우 예외가 발생한다")
    void createTopic_NotMeetingMember_ThrowsException() {
        Long gatheringId = 1L;
        Long meetingId = 1L;
        Long userId = 1L;

        try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId)
                    .thenReturn(userId);

            doNothing().when(gatheringValidator)
                    .validateGathering(gatheringId);

            doNothing().when(meetingValidator)
                    .validateMeetingInGathering(meetingId, gatheringId);

            doNothing().when(meetingValidator)
                    .validateMeetingStatus(meetingId);

            given(meetingValidator.getMeetingMember(meetingId, userId))
                    .willThrow(new MeetingException(MeetingErrorCode.NOT_MEETING_MEMBER));

            assertThatThrownBy(() ->
                    topicService.createTopic(gatheringId, meetingId, testRequest))
                    .isInstanceOf(MeetingException.class)
                    .hasFieldOrPropertyWithValue("errorCode",
                            MeetingErrorCode.NOT_MEETING_MEMBER);

            verify(topicRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getTopics - 주제 목록 조회 (커서 기반 페이지네이션)")
    class GetTopicsTest {

        @Test
        @DisplayName("첫 페이지 조회 - 로그인 사용자")
        void getTopics_FirstPage_WithAuthenticatedUser() {
            Long gatheringId = 1L;
            Long meetingId = 1L;
            Long userId = 1L;
            int pageSize = 10;

            User user1 = User.builder()
                    .id(1L)
                    .userName("김독서")
                    .nickname("책벌레김")
                    .build();

            User user2 = User.builder()
                    .id(2L)
                    .userName("이개발")
                    .nickname("코드러버")
                    .build();

            Topic topic1 = Topic.builder()
                    .id(1L)
                    .meeting(testMeeting)
                    .proposedBy(user1)
                    .title("의미 있는 이름 짓기")
                    .description("변수명, 함수명, 클래스명을 짓는 원칙에 대해 토론합니다.")
                    .topicType(TopicType.DISCUSSION)
                    .topicStatus(TopicStatus.PROPOSED)
                    .likeCount(5)
                    .build();

            Topic topic2 = Topic.builder()
                    .id(2L)
                    .meeting(testMeeting)
                    .proposedBy(user2)
                    .title("함수 작성 원칙")
                    .description("작고 명확한 함수를 작성하는 방법을 논의합니다.")
                    .topicType(TopicType.DISCUSSION)
                    .topicStatus(TopicStatus.PROPOSED)
                    .likeCount(3)
                    .build();

            List<Topic> topics = List.of(topic1, topic2);

            try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
                securityUtilMock.when(SecurityUtil::getCurrentUserId)
                        .thenReturn(userId);

                doNothing().when(gatheringValidator)
                        .validateGathering(gatheringId);

                doNothing().when(meetingValidator)
                        .validateMeetingInGathering(meetingId, gatheringId);

                given(topicRepository.canConfirmTopic(meetingId, userId))
                        .willReturn(false);

                given(topicRepository.canSuggestTopic(meetingId, userId))
                        .willReturn(true);

                given(topicRepository.findTopicsFirstPage(eq(meetingId), any(Pageable.class)))
                        .willReturn(topics);

                given(topicRepository.findDeletableTopicIds(any(), eq(userId)))
                        .willReturn(Set.of(1L));

                given(topicLikeRepository.findLikedTopicIds(any(), eq(userId)))
                        .willReturn(Set.of(1L));

                given(topicRepository.countByMeetingIdAndDeletedAtIsNull(meetingId))
                        .willReturn(2L);

                TopicsWithActionsResponse response = topicService.getTopics(
                        gatheringId, meetingId, pageSize, null, null
                );

                assertThat(response).isNotNull();
                assertThat(response.items()).hasSize(2);
                assertThat(response.pageSize()).isEqualTo(10);
                assertThat(response.hasNext()).isFalse();
                assertThat(response.nextCursor()).isNull();
                assertThat(response.totalCount()).isEqualTo(2);

                assertThat(response.actions().canConfirm()).isFalse();
                assertThat(response.actions().canSuggest()).isTrue();

                assertThat(response.items().get(0).title()).isEqualTo("의미 있는 이름 짓기");
                assertThat(response.items().get(0).likeCount()).isEqualTo(5);
                assertThat(response.items().get(0).topicType()).isEqualTo(TopicType.DISCUSSION);
                assertThat(response.items().get(0).canDelete()).isTrue();
                assertThat(response.items().get(0).isLiked()).isTrue();

                assertThat(response.items().get(1).title()).isEqualTo("함수 작성 원칙");
                assertThat(response.items().get(1).likeCount()).isEqualTo(3);
                assertThat(response.items().get(1).canDelete()).isFalse();
                assertThat(response.items().get(1).isLiked()).isFalse();

                verify(gatheringValidator).validateGathering(gatheringId);
                verify(meetingValidator).validateMeetingInGathering(meetingId, gatheringId);
                verify(topicRepository).findTopicsFirstPage(eq(meetingId), any(Pageable.class));
                verify(topicRepository).findDeletableTopicIds(any(), eq(userId));
                verify(topicLikeRepository).findLikedTopicIds(any(), eq(userId));
            }
        }

        @Test
        @DisplayName("첫 페이지 조회 - 비로그인 사용자")
        void getTopics_FirstPage_WithAnonymousUser() {
            Long gatheringId = 1L;
            Long meetingId = 1L;
            int pageSize = 10;

            User user1 = User.builder()
                    .id(1L)
                    .userName("김독서")
                    .nickname("책벌레김")
                    .build();

            Topic topic1 = Topic.builder()
                    .id(1L)
                    .meeting(testMeeting)
                    .proposedBy(user1)
                    .title("의미 있는 이름 짓기")
                    .description("변수명, 함수명, 클래스명을 짓는 원칙에 대해 토론합니다.")
                    .topicType(TopicType.DISCUSSION)
                    .topicStatus(TopicStatus.PROPOSED)
                    .likeCount(5)
                    .build();

            List<Topic> topics = List.of(topic1);

            try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
                securityUtilMock.when(SecurityUtil::getCurrentUserId)
                        .thenReturn(null);

                doNothing().when(gatheringValidator)
                        .validateGathering(gatheringId);

                doNothing().when(meetingValidator)
                        .validateMeetingInGathering(meetingId, gatheringId);

                given(topicRepository.canConfirmTopic(meetingId, null))
                        .willReturn(false);

                given(topicRepository.canSuggestTopic(meetingId, null))
                        .willReturn(false);

                given(topicRepository.findTopicsFirstPage(eq(meetingId), any(Pageable.class)))
                        .willReturn(topics);

                given(topicRepository.countByMeetingIdAndDeletedAtIsNull(meetingId))
                        .willReturn(1L);

                TopicsWithActionsResponse response = topicService.getTopics(
                        gatheringId, meetingId, pageSize, null, null
                );

                assertThat(response).isNotNull();
                assertThat(response.items()).hasSize(1);
                assertThat(response.items().get(0).canDelete()).isFalse();
                assertThat(response.items().get(0).isLiked()).isFalse();
                assertThat(response.totalCount()).isEqualTo(1);
                assertThat(response.actions().canConfirm()).isFalse();
                assertThat(response.actions().canSuggest()).isFalse();

                verify(topicRepository, never()).findDeletableTopicIds(any(), any());
                verify(topicLikeRepository, never()).findLikedTopicIds(any(), any());
            }
        }

        @Test
        @DisplayName("빈 주제 목록을 조회한다")
        void getTopics_EmptyList() {
            Long gatheringId = 1L;
            Long meetingId = 1L;
            Long userId = 1L;
            int pageSize = 10;

            try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
                securityUtilMock.when(SecurityUtil::getCurrentUserId)
                        .thenReturn(userId);

                doNothing().when(gatheringValidator)
                        .validateGathering(gatheringId);

                doNothing().when(meetingValidator)
                        .validateMeetingInGathering(meetingId, gatheringId);

                given(topicRepository.canConfirmTopic(meetingId, userId))
                        .willReturn(false);

                given(topicRepository.canSuggestTopic(meetingId, userId))
                        .willReturn(false);

                given(topicRepository.findTopicsFirstPage(eq(meetingId), any(Pageable.class)))
                        .willReturn(List.of());

                given(topicRepository.countByMeetingIdAndDeletedAtIsNull(meetingId))
                        .willReturn(0L);

                TopicsWithActionsResponse response = topicService.getTopics(
                        gatheringId, meetingId, pageSize, null, null
                );

                assertThat(response).isNotNull();
                assertThat(response.items()).isEmpty();
                assertThat(response.pageSize()).isEqualTo(10);
                assertThat(response.hasNext()).isFalse();
                assertThat(response.nextCursor()).isNull();
                assertThat(response.totalCount()).isEqualTo(0);

                verify(gatheringValidator).validateGathering(gatheringId);
                verify(meetingValidator).validateMeetingInGathering(meetingId, gatheringId);
                verify(topicRepository).findTopicsFirstPage(eq(meetingId), any(Pageable.class));
                verify(topicRepository, never()).findDeletableTopicIds(any(), any());
                verify(topicLikeRepository, never()).findLikedTopicIds(any(), any());
            }
        }

        @Test
        @DisplayName("다음 페이지가 있는 경우 hasNext와 nextCursor가 설정된다")
        void getTopics_WithNextPage() {
            Long gatheringId = 1L;
            Long meetingId = 1L;
            Long userId = 1L;
            int pageSize = 2;

            // pageSize + 1 = 3개 반환하여 다음 페이지 존재 표시
            List<Topic> topics = List.of(
                    Topic.builder()
                            .id(1L)
                            .meeting(testMeeting)
                            .proposedBy(testUser)
                            .title("주제 1")
                            .topicType(TopicType.DISCUSSION)
                            .topicStatus(TopicStatus.PROPOSED)
                            .likeCount(10)
                            .build(),
                    Topic.builder()
                            .id(2L)
                            .meeting(testMeeting)
                            .proposedBy(testUser)
                            .title("주제 2")
                            .topicType(TopicType.DISCUSSION)
                            .topicStatus(TopicStatus.PROPOSED)
                            .likeCount(5)
                            .build(),
                    Topic.builder()
                            .id(3L)
                            .meeting(testMeeting)
                            .proposedBy(testUser)
                            .title("주제 3")
                            .topicType(TopicType.DISCUSSION)
                            .topicStatus(TopicStatus.PROPOSED)
                            .likeCount(3)
                            .build()
            );

            try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
                securityUtilMock.when(SecurityUtil::getCurrentUserId)
                        .thenReturn(userId);

                doNothing().when(gatheringValidator)
                        .validateGathering(gatheringId);

                doNothing().when(meetingValidator)
                        .validateMeetingInGathering(meetingId, gatheringId);

                given(topicRepository.canConfirmTopic(meetingId, userId))
                        .willReturn(false);

                given(topicRepository.canSuggestTopic(meetingId, userId))
                        .willReturn(true);

                given(topicRepository.findTopicsFirstPage(eq(meetingId), any(Pageable.class)))
                        .willReturn(topics);

                given(topicRepository.findDeletableTopicIds(any(), eq(userId)))
                        .willReturn(Set.of());

                given(topicLikeRepository.findLikedTopicIds(any(), eq(userId)))
                        .willReturn(Set.of());

                given(topicRepository.countByMeetingIdAndDeletedAtIsNull(meetingId))
                        .willReturn(10L);

                TopicsWithActionsResponse response = topicService.getTopics(
                        gatheringId, meetingId, pageSize, null, null
                );

                assertThat(response).isNotNull();
                assertThat(response.items()).hasSize(2);
                assertThat(response.pageSize()).isEqualTo(2);
                assertThat(response.hasNext()).isTrue();
                assertThat(response.nextCursor()).isNotNull();
                assertThat(response.nextCursor().likeCount()).isEqualTo(5);
                assertThat(response.nextCursor().topicId()).isEqualTo(2L);
                assertThat(response.totalCount()).isEqualTo(10);

                verify(topicRepository).findTopicsFirstPage(eq(meetingId), any(Pageable.class));
            }
        }

        @Test
        @DisplayName("커서를 사용하여 다음 페이지를 조회한다")
        void getTopics_WithCursor() {
            Long gatheringId = 1L;
            Long meetingId = 1L;
            Long userId = 1L;
            int pageSize = 10;
            Integer cursorLikeCount = 5;
            Long cursorTopicId = 2L;

            Topic topic3 = Topic.builder()
                    .id(3L)
                    .meeting(testMeeting)
                    .proposedBy(testUser)
                    .title("주제 3")
                    .topicType(TopicType.DISCUSSION)
                    .topicStatus(TopicStatus.PROPOSED)
                    .likeCount(3)
                    .build();

            List<Topic> topics = List.of(topic3);

            try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
                securityUtilMock.when(SecurityUtil::getCurrentUserId)
                        .thenReturn(userId);

                doNothing().when(gatheringValidator)
                        .validateGathering(gatheringId);

                doNothing().when(meetingValidator)
                        .validateMeetingInGathering(meetingId, gatheringId);

                given(topicRepository.canConfirmTopic(meetingId, userId))
                        .willReturn(false);

                given(topicRepository.canSuggestTopic(meetingId, userId))
                        .willReturn(true);

                given(topicRepository.findTopicsAfterCursor(
                        eq(meetingId), eq(cursorLikeCount), eq(cursorTopicId), any(Pageable.class)
                )).willReturn(topics);

                given(topicRepository.findDeletableTopicIds(any(), eq(userId)))
                        .willReturn(Set.of());

                given(topicLikeRepository.findLikedTopicIds(any(), eq(userId)))
                        .willReturn(Set.of(3L));

                TopicsWithActionsResponse response = topicService.getTopics(
                        gatheringId, meetingId, pageSize, cursorLikeCount, cursorTopicId
                );

                assertThat(response).isNotNull();
                assertThat(response.items()).hasSize(1);
                assertThat(response.hasNext()).isFalse();
                assertThat(response.nextCursor()).isNull();
                assertThat(response.totalCount()).isNull();
                assertThat(response.items().get(0).isLiked()).isTrue();

                verify(topicRepository).findTopicsAfterCursor(
                        eq(meetingId), eq(cursorLikeCount), eq(cursorTopicId), any(Pageable.class)
                );
                verify(topicRepository, never()).findTopicsFirstPage(any(), any());
                verify(topicRepository, never()).countByMeetingIdAndDeletedAtIsNull(any());
            }
        }

        @Test
        @DisplayName("모임을 찾을 수 없는 경우 예외가 발생한다")
        void getTopics_GatheringNotFound_ThrowsException() {
            Long gatheringId = 999L;
            Long meetingId = 1L;
            Long userId = 1L;
            int pageSize = 10;

            try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
                securityUtilMock.when(SecurityUtil::getCurrentUserId)
                        .thenReturn(userId);

                doThrow(new GatheringException(GatheringErrorCode.GATHERING_NOT_FOUND))
                        .when(gatheringValidator)
                        .validateGathering(gatheringId);

                assertThatThrownBy(() ->
                        topicService.getTopics(gatheringId, meetingId, pageSize, null, null))
                        .isInstanceOf(GatheringException.class)
                        .hasFieldOrPropertyWithValue("errorCode",
                                GatheringErrorCode.GATHERING_NOT_FOUND);

                verify(meetingValidator, never()).validateMeetingInGathering(any(), any());
                verify(topicRepository, never()).findTopicsFirstPage(any(), any());
            }
        }

        @Test
        @DisplayName("회차가 해당 모임에 속하지 않는 경우 예외가 발생한다")
        void getTopics_MeetingNotInGathering_ThrowsException() {
            Long gatheringId = 1L;
            Long meetingId = 999L;
            Long userId = 1L;
            int pageSize = 10;

            try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
                securityUtilMock.when(SecurityUtil::getCurrentUserId)
                        .thenReturn(userId);

                doNothing().when(gatheringValidator)
                        .validateGathering(gatheringId);

                doThrow(new MeetingException(MeetingErrorCode.NOT_GATHERING_MEETING))
                        .when(meetingValidator)
                        .validateMeetingInGathering(meetingId, gatheringId);

                assertThatThrownBy(() ->
                        topicService.getTopics(gatheringId, meetingId, pageSize, null, null))
                        .isInstanceOf(MeetingException.class)
                        .hasFieldOrPropertyWithValue("errorCode",
                                MeetingErrorCode.NOT_GATHERING_MEETING);

                verify(topicRepository, never()).findTopicsFirstPage(any(), any());
            }
        }
    }

    @Nested
    @DisplayName("deleteTopic - 주제 삭제")
    class DeleteTopicTest {

        @Test
        @DisplayName("정상적으로 주제를 삭제한다")
        void deleteTopic_Success() {
            Long gatheringId = 1L;
            Long meetingId = 1L;
            Long topicId = 1L;
            Long userId = 1L;

            Topic mockTopic = mock(Topic.class);

            try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
                securityUtilMock.when(SecurityUtil::getCurrentUserId)
                        .thenReturn(userId);

                doNothing().when(gatheringValidator)
                        .validateGathering(gatheringId);

                doNothing().when(meetingValidator)
                        .validateMeetingInGathering(meetingId, gatheringId);

                doNothing().when(meetingValidator)
                        .validateMeetingMember(meetingId, userId);

                doNothing().when(topicValidator)
                        .validateTopicInMeeting(topicId, meetingId);

                given(topicValidator.getDeletableTopic(topicId, userId))
                        .willReturn(mockTopic);

                topicService.deleteTopic(gatheringId, meetingId, topicId);

                verify(gatheringValidator).validateGathering(gatheringId);
                verify(meetingValidator).validateMeetingInGathering(meetingId, gatheringId);
                verify(meetingValidator).validateMeetingMember(meetingId, userId);
                verify(topicValidator).validateTopicInMeeting(topicId, meetingId);
                verify(topicValidator).getDeletableTopic(topicId, userId);
                verify(mockTopic).softDelete();
            }
        }

        @Test
        @DisplayName("인증되지 않은 사용자인 경우 예외가 발생한다")
        void deleteTopic_Unauthorized_ThrowsException() {
            Long gatheringId = 1L;
            Long meetingId = 1L;
            Long topicId = 1L;

            try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
                securityUtilMock.when(SecurityUtil::getCurrentUserId)
                        .thenThrow(new GlobalException(GlobalErrorCode.UNAUTHORIZED));

                assertThatThrownBy(() ->
                        topicService.deleteTopic(gatheringId, meetingId, topicId))
                        .isInstanceOf(GlobalException.class)
                        .hasFieldOrPropertyWithValue("errorCode",
                                GlobalErrorCode.UNAUTHORIZED);

                verify(gatheringValidator, never()).validateGathering(any());
                verify(meetingValidator, never()).validateMeetingInGathering(any(), any());
                verify(topicValidator, never()).validateTopicInMeeting(any(), any());
                verify(topicValidator, never()).getDeletableTopic(any(), any());
            }
        }

        @Test
        @DisplayName("모임을 찾을 수 없는 경우 예외가 발생한다")
        void deleteTopic_GatheringNotFound_ThrowsException() {
            Long gatheringId = 999L;
            Long meetingId = 1L;
            Long topicId = 1L;
            Long userId = 1L;

            try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
                securityUtilMock.when(SecurityUtil::getCurrentUserId)
                        .thenReturn(userId);

                doThrow(new GatheringException(GatheringErrorCode.GATHERING_NOT_FOUND))
                        .when(gatheringValidator)
                        .validateGathering(gatheringId);

                assertThatThrownBy(() ->
                        topicService.deleteTopic(gatheringId, meetingId, topicId))
                        .isInstanceOf(GatheringException.class)
                        .hasFieldOrPropertyWithValue("errorCode",
                                GatheringErrorCode.GATHERING_NOT_FOUND);

                verify(meetingValidator, never()).validateMeetingInGathering(any(), any());
                verify(meetingValidator, never()).validateMeetingMember(any(), any());
                verify(topicValidator, never()).validateTopicInMeeting(any(), any());
                verify(topicValidator, never()).getDeletableTopic(any(), any());
            }
        }

        @Test
        @DisplayName("회차가 해당 모임에 속하지 않는 경우 예외가 발생한다")
        void deleteTopic_MeetingNotInGathering_ThrowsException() {
            Long gatheringId = 1L;
            Long meetingId = 999L;
            Long topicId = 1L;
            Long userId = 1L;

            try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
                securityUtilMock.when(SecurityUtil::getCurrentUserId)
                        .thenReturn(userId);

                doNothing().when(gatheringValidator)
                        .validateGathering(gatheringId);

                doThrow(new MeetingException(MeetingErrorCode.NOT_GATHERING_MEETING))
                        .when(meetingValidator)
                        .validateMeetingInGathering(meetingId, gatheringId);

                assertThatThrownBy(() ->
                        topicService.deleteTopic(gatheringId, meetingId, topicId))
                        .isInstanceOf(MeetingException.class)
                        .hasFieldOrPropertyWithValue("errorCode",
                                MeetingErrorCode.NOT_GATHERING_MEETING);

                verify(meetingValidator, never()).validateMeetingMember(any(), any());
                verify(topicValidator, never()).validateTopicInMeeting(any(), any());
                verify(topicValidator, never()).getDeletableTopic(any(), any());
            }
        }

        @Test
        @DisplayName("주제가 해당 회차에 속하지 않는 경우 예외가 발생한다")
        void deleteTopic_TopicNotInMeeting_ThrowsException() {
            Long gatheringId = 1L;
            Long meetingId = 1L;
            Long topicId = 999L;
            Long userId = 1L;

            try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
                securityUtilMock.when(SecurityUtil::getCurrentUserId)
                        .thenReturn(userId);

                doNothing().when(gatheringValidator)
                        .validateGathering(gatheringId);

                doNothing().when(meetingValidator)
                        .validateMeetingInGathering(meetingId, gatheringId);

                doNothing().when(meetingValidator)
                        .validateMeetingMember(meetingId, userId);

                doThrow(new TopicException(TopicErrorCode.TOPIC_NOT_IN_MEETING))
                        .when(topicValidator)
                        .validateTopicInMeeting(topicId, meetingId);

                assertThatThrownBy(() ->
                        topicService.deleteTopic(gatheringId, meetingId, topicId))
                        .isInstanceOf(TopicException.class)
                        .hasFieldOrPropertyWithValue("errorCode",
                                TopicErrorCode.TOPIC_NOT_IN_MEETING);

                verify(topicValidator, never()).getDeletableTopic(any(), any());
            }
        }

        @Test
        @DisplayName("주제를 찾을 수 없는 경우 예외가 발생한다")
        void deleteTopic_TopicNotFound_ThrowsException() {
            Long gatheringId = 1L;
            Long meetingId = 1L;
            Long topicId = 999L;
            Long userId = 1L;

            try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
                securityUtilMock.when(SecurityUtil::getCurrentUserId)
                        .thenReturn(userId);

                doNothing().when(gatheringValidator)
                        .validateGathering(gatheringId);

                doNothing().when(meetingValidator)
                        .validateMeetingInGathering(meetingId, gatheringId);

                doNothing().when(meetingValidator)
                        .validateMeetingMember(meetingId, userId);

                doThrow(new TopicException(TopicErrorCode.TOPIC_NOT_FOUND))
                        .when(topicValidator)
                        .validateTopicInMeeting(topicId, meetingId);

                assertThatThrownBy(() ->
                        topicService.deleteTopic(gatheringId, meetingId, topicId))
                        .isInstanceOf(TopicException.class)
                        .hasFieldOrPropertyWithValue("errorCode",
                                TopicErrorCode.TOPIC_NOT_FOUND);

                verify(topicValidator, never()).getDeletableTopic(any(), any());
            }
        }

        @Test
        @DisplayName("주제가 이미 삭제된 경우 예외가 발생한다")
        void deleteTopic_TopicAlreadyDeleted_ThrowsException() {
            Long gatheringId = 1L;
            Long meetingId = 1L;
            Long topicId = 1L;
            Long userId = 1L;

            try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
                securityUtilMock.when(SecurityUtil::getCurrentUserId)
                        .thenReturn(userId);

                doNothing().when(gatheringValidator)
                        .validateGathering(gatheringId);

                doNothing().when(meetingValidator)
                        .validateMeetingInGathering(meetingId, gatheringId);

                doNothing().when(meetingValidator)
                        .validateMeetingMember(meetingId, userId);

                doThrow(new TopicException(TopicErrorCode.TOPIC_ALREADY_DELETED))
                        .when(topicValidator)
                        .validateTopicInMeeting(topicId, meetingId);

                assertThatThrownBy(() ->
                        topicService.deleteTopic(gatheringId, meetingId, topicId))
                        .isInstanceOf(TopicException.class)
                        .hasFieldOrPropertyWithValue("errorCode",
                                TopicErrorCode.TOPIC_ALREADY_DELETED);

                verify(topicValidator, never()).getDeletableTopic(any(), any());
            }
        }

        @Test
        @DisplayName("삭제 권한이 없는 경우 예외가 발생한다")
        void deleteTopic_UserCannotDelete_ThrowsException() {
            Long gatheringId = 1L;
            Long meetingId = 1L;
            Long topicId = 1L;
            Long userId = 2L;

            try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
                securityUtilMock.when(SecurityUtil::getCurrentUserId)
                        .thenReturn(userId);

                doNothing().when(gatheringValidator)
                        .validateGathering(gatheringId);

                doNothing().when(meetingValidator)
                        .validateMeetingInGathering(meetingId, gatheringId);

                doNothing().when(meetingValidator)
                        .validateMeetingMember(meetingId, userId);

                doNothing().when(topicValidator)
                        .validateTopicInMeeting(topicId, meetingId);

                given(topicValidator.getDeletableTopic(topicId, userId))
                        .willThrow(new TopicException(TopicErrorCode.TOPIC_USER_CANNOT_DELETE));

                assertThatThrownBy(() ->
                        topicService.deleteTopic(gatheringId, meetingId, topicId))
                        .isInstanceOf(TopicException.class)
                        .hasFieldOrPropertyWithValue("errorCode",
                                TopicErrorCode.TOPIC_USER_CANNOT_DELETE);
            }
        }
    }

    @Nested
    @DisplayName("toggleTopicLike - 주제 좋아요/취소")
    class ToggleTopicLikeTest {

        @Test
        @DisplayName("정상적으로 주제에 좋아요를 추가한다")
        void toggleTopicLike_AddLike_Success() {
            Long gatheringId = 1L;
            Long meetingId = 1L;
            Long topicId = 1L;

            Topic topicWithLikeCount = Topic.builder()
                    .id(topicId)
                    .meeting(testMeeting)
                    .proposedBy(testUser)
                    .title("테스트 주제")
                    .topicType(TopicType.DISCUSSION)
                    .likeCount(5)
                    .build();

            try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
                securityUtilMock.when(SecurityUtil::getCurrentUserEntity)
                        .thenReturn(testUser);

                doNothing().when(gatheringValidator)
                        .validateGathering(gatheringId);

                doNothing().when(meetingValidator)
                        .validateMeetingInGathering(meetingId, gatheringId);

                doNothing().when(meetingValidator)
                        .validateMeetingMember(meetingId, testUser.getId());

                given(topicValidator.getTopicInMeeting(topicId, meetingId))
                        .willReturn(topicWithLikeCount);

                given(topicLikeRepository.existsByTopicId(topicId))
                        .willReturn(false);

                given(topicLikeRepository.save(any(TopicLike.class)))
                        .willReturn(TopicLike.create(topicWithLikeCount, testUser));

                TopicLikeResponse response =
                        topicService.toggleTopicLike(gatheringId, meetingId, topicId);

                assertThat(response).isNotNull();
                assertThat(response.topicId()).isEqualTo(topicId);
                assertThat(response.liked()).isTrue();
                assertThat(response.newCount()).isEqualTo(6);

                verify(gatheringValidator).validateGathering(gatheringId);
                verify(meetingValidator).validateMeetingInGathering(meetingId, gatheringId);
                verify(meetingValidator).validateMeetingMember(meetingId, testUser.getId());
                verify(topicValidator).getTopicInMeeting(topicId, meetingId);
                verify(topicLikeRepository).existsByTopicId(topicId);
                verify(topicLikeRepository).save(any(TopicLike.class));
                verify(topicRepository).increaseLikeCount(topicId);
                verify(topicLikeRepository, never()).deleteByTopicIdAndUserId(any(), any());
            }
        }

        @Test
        @DisplayName("정상적으로 주제 좋아요를 취소한다")
        void toggleTopicLike_CancelLike_Success() {
            Long gatheringId = 1L;
            Long meetingId = 1L;
            Long topicId = 1L;

            Topic topicWithLikeCount = Topic.builder()
                    .id(topicId)
                    .meeting(testMeeting)
                    .proposedBy(testUser)
                    .title("테스트 주제")
                    .topicType(TopicType.DISCUSSION)
                    .likeCount(5)
                    .build();

            try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
                securityUtilMock.when(SecurityUtil::getCurrentUserEntity)
                        .thenReturn(testUser);

                doNothing().when(gatheringValidator)
                        .validateGathering(gatheringId);

                doNothing().when(meetingValidator)
                        .validateMeetingInGathering(meetingId, gatheringId);

                doNothing().when(meetingValidator)
                        .validateMeetingMember(meetingId, testUser.getId());

                given(topicValidator.getTopicInMeeting(topicId, meetingId))
                        .willReturn(topicWithLikeCount);

                given(topicLikeRepository.existsByTopicId(topicId))
                        .willReturn(true);

                TopicLikeResponse response =
                        topicService.toggleTopicLike(gatheringId, meetingId, topicId);

                assertThat(response).isNotNull();
                assertThat(response.topicId()).isEqualTo(topicId);
                assertThat(response.liked()).isFalse();
                assertThat(response.newCount()).isEqualTo(4);

                verify(gatheringValidator).validateGathering(gatheringId);
                verify(meetingValidator).validateMeetingInGathering(meetingId, gatheringId);
                verify(meetingValidator).validateMeetingMember(meetingId, testUser.getId());
                verify(topicValidator).getTopicInMeeting(topicId, meetingId);
                verify(topicLikeRepository).existsByTopicId(topicId);
                verify(topicLikeRepository).deleteByTopicIdAndUserId(topicId, testUser.getId());
                verify(topicRepository).decreaseLikeCount(topicId);
                verify(topicLikeRepository, never()).save(any(TopicLike.class));
            }
        }

        @Test
        @DisplayName("인증되지 않은 사용자인 경우 예외가 발생한다")
        void toggleTopicLike_Unauthorized_ThrowsException() {
            Long gatheringId = 1L;
            Long meetingId = 1L;
            Long topicId = 1L;

            try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
                securityUtilMock.when(SecurityUtil::getCurrentUserEntity)
                        .thenThrow(new GlobalException(GlobalErrorCode.UNAUTHORIZED));

                assertThatThrownBy(() ->
                        topicService.toggleTopicLike(gatheringId, meetingId, topicId))
                        .isInstanceOf(GlobalException.class)
                        .hasFieldOrPropertyWithValue("errorCode",
                                GlobalErrorCode.UNAUTHORIZED);

                verify(gatheringValidator, never()).validateGathering(any());
                verify(meetingValidator, never()).validateMeetingInGathering(any(), any());
                verify(topicValidator, never()).getTopicInMeeting(any(), any());
                verify(topicLikeRepository, never()).existsByTopicId(any());
            }
        }

        @Test
        @DisplayName("모임을 찾을 수 없는 경우 예외가 발생한다")
        void toggleTopicLike_GatheringNotFound_ThrowsException() {
            Long gatheringId = 999L;
            Long meetingId = 1L;
            Long topicId = 1L;

            try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
                securityUtilMock.when(SecurityUtil::getCurrentUserEntity)
                        .thenReturn(testUser);

                doThrow(new GatheringException(GatheringErrorCode.GATHERING_NOT_FOUND))
                        .when(gatheringValidator)
                        .validateGathering(gatheringId);

                assertThatThrownBy(() ->
                        topicService.toggleTopicLike(gatheringId, meetingId, topicId))
                        .isInstanceOf(GatheringException.class)
                        .hasFieldOrPropertyWithValue("errorCode",
                                GatheringErrorCode.GATHERING_NOT_FOUND);

                verify(meetingValidator, never()).validateMeetingInGathering(any(), any());
                verify(topicValidator, never()).getTopicInMeeting(any(), any());
                verify(topicLikeRepository, never()).existsByTopicId(any());
            }
        }

        @Test
        @DisplayName("회차가 해당 모임에 속하지 않는 경우 예외가 발생한다")
        void toggleTopicLike_MeetingNotInGathering_ThrowsException() {
            Long gatheringId = 1L;
            Long meetingId = 999L;
            Long topicId = 1L;

            try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
                securityUtilMock.when(SecurityUtil::getCurrentUserEntity)
                        .thenReturn(testUser);

                doNothing().when(gatheringValidator)
                        .validateGathering(gatheringId);

                doThrow(new MeetingException(MeetingErrorCode.NOT_GATHERING_MEETING))
                        .when(meetingValidator)
                        .validateMeetingInGathering(meetingId, gatheringId);

                assertThatThrownBy(() ->
                        topicService.toggleTopicLike(gatheringId, meetingId, topicId))
                        .isInstanceOf(MeetingException.class)
                        .hasFieldOrPropertyWithValue("errorCode",
                                MeetingErrorCode.NOT_GATHERING_MEETING);

                verify(meetingValidator, never()).validateMeetingMember(any(), any());
                verify(topicValidator, never()).getTopicInMeeting(any(), any());
                verify(topicLikeRepository, never()).existsByTopicId(any());
            }
        }

        @Test
        @DisplayName("회차 멤버가 아닌 경우 예외가 발생한다")
        void toggleTopicLike_NotMeetingMember_ThrowsException() {
            Long gatheringId = 1L;
            Long meetingId = 1L;
            Long topicId = 1L;

            try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
                securityUtilMock.when(SecurityUtil::getCurrentUserEntity)
                        .thenReturn(testUser);

                doNothing().when(gatheringValidator)
                        .validateGathering(gatheringId);

                doNothing().when(meetingValidator)
                        .validateMeetingInGathering(meetingId, gatheringId);

                doThrow(new MeetingException(MeetingErrorCode.NOT_MEETING_MEMBER))
                        .when(meetingValidator)
                        .validateMeetingMember(meetingId, testUser.getId());

                assertThatThrownBy(() ->
                        topicService.toggleTopicLike(gatheringId, meetingId, topicId))
                        .isInstanceOf(MeetingException.class)
                        .hasFieldOrPropertyWithValue("errorCode",
                                MeetingErrorCode.NOT_MEETING_MEMBER);

                verify(topicValidator, never()).getTopicInMeeting(any(), any());
                verify(topicLikeRepository, never()).existsByTopicId(any());
            }
        }

        @Test
        @DisplayName("주제를 찾을 수 없는 경우 예외가 발생한다")
        void toggleTopicLike_TopicNotFound_ThrowsException() {
            Long gatheringId = 1L;
            Long meetingId = 1L;
            Long topicId = 999L;

            try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
                securityUtilMock.when(SecurityUtil::getCurrentUserEntity)
                        .thenReturn(testUser);

                doNothing().when(gatheringValidator)
                        .validateGathering(gatheringId);

                doNothing().when(meetingValidator)
                        .validateMeetingInGathering(meetingId, gatheringId);

                doNothing().when(meetingValidator)
                        .validateMeetingMember(meetingId, testUser.getId());

                given(topicValidator.getTopicInMeeting(topicId, meetingId))
                        .willThrow(new TopicException(TopicErrorCode.TOPIC_NOT_FOUND));

                assertThatThrownBy(() ->
                        topicService.toggleTopicLike(gatheringId, meetingId, topicId))
                        .isInstanceOf(TopicException.class)
                        .hasFieldOrPropertyWithValue("errorCode",
                                TopicErrorCode.TOPIC_NOT_FOUND);

                verify(topicLikeRepository, never()).existsByTopicId(any());
            }
        }

        @Test
        @DisplayName("주제가 해당 회차에 속하지 않는 경우 예외가 발생한다")
        void toggleTopicLike_TopicNotInMeeting_ThrowsException() {
            Long gatheringId = 1L;
            Long meetingId = 1L;
            Long topicId = 1L;

            try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
                securityUtilMock.when(SecurityUtil::getCurrentUserEntity)
                        .thenReturn(testUser);

                doNothing().when(gatheringValidator)
                        .validateGathering(gatheringId);

                doNothing().when(meetingValidator)
                        .validateMeetingInGathering(meetingId, gatheringId);

                doNothing().when(meetingValidator)
                        .validateMeetingMember(meetingId, testUser.getId());

                given(topicValidator.getTopicInMeeting(topicId, meetingId))
                        .willThrow(new TopicException(TopicErrorCode.TOPIC_NOT_IN_MEETING));

                assertThatThrownBy(() ->
                        topicService.toggleTopicLike(gatheringId, meetingId, topicId))
                        .isInstanceOf(TopicException.class)
                        .hasFieldOrPropertyWithValue("errorCode",
                                TopicErrorCode.TOPIC_NOT_IN_MEETING);

                verify(topicLikeRepository, never()).existsByTopicId(any());
            }
        }

        @Test
        @DisplayName("이미 삭제된 주제에 좋아요를 시도하면 예외가 발생한다")
        void toggleTopicLike_TopicAlreadyDeleted_ThrowsException() {
            Long gatheringId = 1L;
            Long meetingId = 1L;
            Long topicId = 1L;

            try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
                securityUtilMock.when(SecurityUtil::getCurrentUserEntity)
                        .thenReturn(testUser);

                doNothing().when(gatheringValidator)
                        .validateGathering(gatheringId);

                doNothing().when(meetingValidator)
                        .validateMeetingInGathering(meetingId, gatheringId);

                doNothing().when(meetingValidator)
                        .validateMeetingMember(meetingId, testUser.getId());

                given(topicValidator.getTopicInMeeting(topicId, meetingId))
                        .willThrow(new TopicException(TopicErrorCode.TOPIC_ALREADY_DELETED));

                assertThatThrownBy(() ->
                        topicService.toggleTopicLike(gatheringId, meetingId, topicId))
                        .isInstanceOf(TopicException.class)
                        .hasFieldOrPropertyWithValue("errorCode",
                                TopicErrorCode.TOPIC_ALREADY_DELETED);

                verify(topicLikeRepository, never()).existsByTopicId(any());
            }
        }
    }
}
