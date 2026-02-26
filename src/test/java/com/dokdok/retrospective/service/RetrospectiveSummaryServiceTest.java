package com.dokdok.retrospective.service;

import com.dokdok.gathering.entity.Gathering;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetrospectiveSummaryServiceTest {

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private TopicRetrospectiveSummaryRepository topicRetrospectiveSummaryRepository;

    @Mock
    private MeetingValidator meetingValidator;

    @Mock
    private RetrospectiveValidator retrospectiveValidator;

    @InjectMocks
    private RetrospectiveSummaryService retrospectiveSummaryService;

    private Long meetingId;
    private Long userId;
    private Long gatheringId;
    private Meeting meeting;
    private Topic topic;
    private TopicRetrospectiveSummary summary;

    @BeforeEach
    void setUp() {
        meetingId = 1L;
        userId = 1L;
        gatheringId = 1L;

        Gathering gathering = Gathering.builder().id(gatheringId).build();
        meeting = Meeting.builder().id(meetingId).gathering(gathering).build();
        topic = Topic.builder()
                .id(10L)
                .meeting(meeting)
                .title("토픽 제목")
                .description("토픽 설명")
                .topicStatus(TopicStatus.CONFIRMED)
                .build();
        summary = TopicRetrospectiveSummary.builder()
                .id(100L)
                .topic(topic)
                .summary("기존 요약")
                .keyPoints(List.of(new TopicRetrospectiveSummary.KeyPoint(
                        "기존 포인트",
                        List.of("포인트 상세")
                )))
                .build();
    }

    @Test
    @DisplayName("AI 요약 조회 시 토픽과 요약이 매핑되어 반환된다")
    void getRetrospectiveSummary_returnsMappedSummary() {
        try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

            when(meetingValidator.findMeetingOrThrow(meetingId)).thenReturn(meeting);
            doNothing().when(retrospectiveValidator)
                    .validateSummaryUpdatePermission(gatheringId, meetingId, userId);
            when(topicRepository.findByMeetingIdAndTopicStatusOrderByConfirmOrderAsc(meetingId, TopicStatus.CONFIRMED))
                    .thenReturn(List.of(topic));
            when(topicRetrospectiveSummaryRepository.findAllByTopicIdIn(List.of(topic.getId())))
                    .thenReturn(List.of(summary));

            RetrospectiveSummaryResponse response = retrospectiveSummaryService.getRetrospectiveSummary(meetingId);

            assertThat(response.meetingId()).isEqualTo(meetingId);
            assertThat(response.isPublished()).isFalse();
            assertThat(response.topics()).hasSize(1);
            assertThat(response.topics().get(0).topicId()).isEqualTo(topic.getId());
            assertThat(response.topics().get(0).summary()).isEqualTo("기존 요약");
            assertThat(response.topics().get(0).keyPoints()).hasSize(1);
            assertThat(response.topics().get(0).keyPoints().get(0).title()).isEqualTo("기존 포인트");
        }
    }

    @Test
    @DisplayName("AI 요약 수정 시 요약이 갱신되고 결과를 반환한다")
    void updateRetrospectiveSummary_updatesSummaryAndReturns() {
        RetrospectiveSummaryUpdateRequest request = RetrospectiveSummaryUpdateRequest.builder()
                .topics(List.of(RetrospectiveSummaryUpdateRequest.TopicSummaryUpdateRequest.builder()
                        .topicId(topic.getId())
                        .summary("수정 요약")
                        .keyPoints(List.of(RetrospectiveSummaryUpdateRequest.KeyPointUpdateRequest.builder()
                                .title("수정 포인트")
                                .details(List.of("수정 상세"))
                                .build()))
                        .build()))
                .build();

        try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

            when(meetingValidator.findMeetingOrThrow(meetingId)).thenReturn(meeting);
            doNothing().when(retrospectiveValidator)
                    .validateSummaryUpdatePermission(gatheringId, meetingId, userId);
            when(topicRetrospectiveSummaryRepository.findByTopicId(topic.getId()))
                    .thenReturn(Optional.of(summary));
            when(topicRepository.findByMeetingIdAndTopicStatusOrderByConfirmOrderAsc(meetingId, TopicStatus.CONFIRMED))
                    .thenReturn(List.of(topic));
            when(topicRetrospectiveSummaryRepository.findAllByTopicIdIn(List.of(topic.getId())))
                    .thenReturn(List.of(summary));

            RetrospectiveSummaryResponse response =
                    retrospectiveSummaryService.updateRetrospectiveSummary(meetingId, request);

            assertThat(summary.getSummary()).isEqualTo("수정 요약");
            assertThat(summary.getKeyPoints()).hasSize(1);
            assertThat(summary.getKeyPoints().get(0).getTitle()).isEqualTo("수정 포인트");
            assertThat(response.topics().get(0).summary()).isEqualTo("수정 요약");
            assertThat(response.topics().get(0).keyPoints()).hasSize(1);
            assertThat(response.topics().get(0).keyPoints().get(0).title()).isEqualTo("수정 포인트");
            // update에서 1번, 내부 getRetrospectiveSummary에서 1번 = 총 2번 호출
            verify(retrospectiveValidator, times(2)).validateSummaryUpdatePermission(gatheringId, meetingId, userId);
        }
    }

    @Nested
    @DisplayName("약속 회고 생성(퍼블리시)")
    class PublishRetrospectiveTest {

        @Test
        @DisplayName("약속 회고 생성 시 isPublished가 true로 변경된다")
        void publishRetrospective_success() {
            try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
                securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

                when(meetingValidator.findMeetingOrThrow(meetingId)).thenReturn(meeting);
                doNothing().when(retrospectiveValidator)
                        .validateSummaryUpdatePermission(gatheringId, meetingId, userId);
                when(topicRepository.findByMeetingIdAndTopicStatusOrderByConfirmOrderAsc(meetingId, TopicStatus.CONFIRMED))
                        .thenReturn(List.of(topic));
                when(topicRetrospectiveSummaryRepository.findAllByTopicIdIn(List.of(topic.getId())))
                        .thenReturn(List.of(summary));

                RetrospectiveSummaryResponse response = retrospectiveSummaryService.publishRetrospective(meetingId);

                assertThat(meeting.isRetrospectivePublished()).isTrue();
                assertThat(meeting.getRetrospectivePublishedAt()).isNotNull();
                assertThat(response.isPublished()).isTrue();
                assertThat(response.publishedAt()).isNotNull();
            }
        }

        @Test
        @DisplayName("이미 생성된 약속 회고를 다시 생성하려고 하면 예외가 발생한다")
        void publishRetrospective_alreadyPublished_throwsException() {
            // 이미 퍼블리시된 상태로 설정
            meeting.publishRetrospective();

            try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
                securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

                when(meetingValidator.findMeetingOrThrow(meetingId)).thenReturn(meeting);
                doNothing().when(retrospectiveValidator)
                        .validateSummaryUpdatePermission(gatheringId, meetingId, userId);

                assertThatThrownBy(() -> retrospectiveSummaryService.publishRetrospective(meetingId))
                        .isInstanceOf(RetrospectiveException.class)
                        .hasFieldOrPropertyWithValue("errorCode", RetrospectiveErrorCode.RETROSPECTIVE_ALREADY_PUBLISHED);
            }
        }
    }
}
