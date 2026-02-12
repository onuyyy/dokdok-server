package com.dokdok.topic.service;

import com.dokdok.ai.client.AiSummaryClient;
import com.dokdok.ai.dto.TopicSummaryRequest;
import com.dokdok.gathering.service.GatheringValidator;
import com.dokdok.global.exception.GlobalException;
import com.dokdok.meeting.service.MeetingValidator;
import com.dokdok.topic.entity.Topic;
import com.dokdok.topic.entity.TopicAnswer;
import com.dokdok.topic.repository.TopicAnswerRepository;
import com.dokdok.user.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TopicSummaryServiceTest {

    @Mock
    private GatheringValidator gatheringValidator;

    @Mock
    private MeetingValidator meetingValidator;

    @Mock
    private TopicValidator topicValidator;

    @Mock
    private TopicAnswerRepository topicAnswerRepository;

    @Mock
    private AiSummaryClient aiSummaryClient;

    @InjectMocks
    private TopicSummaryService topicSummaryService;

    @BeforeEach
    void setUpSecurityContext() {
        User user = User.builder()
                .id(1L)
                .nickname("tester")
                .kakaoId(1L)
                .build();
        com.dokdok.oauth2.CustomOAuth2User principal = com.dokdok.oauth2.CustomOAuth2User.builder()
                .user(user)
                .attributes(java.util.Collections.emptyMap())
                .build();
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("토픽 요약 요청 시 제출된 답변만 모아 AI에 전달한다")
    void requestTopicSummary_buildsPayloadAndDelegatesToClient() {
        Long gatheringId = 1L;
        Long meetingId = 2L;
        Long topicId = 5L;
        Topic topic = Topic.builder().id(topicId).title("데미안 토론").build();

        User user3 = User.builder().id(3L).kakaoId(3L).build();
        User user4 = User.builder().id(4L).kakaoId(4L).build();
        User user5 = User.builder().id(5L).kakaoId(5L).build();

        TopicAnswer answer1 = TopicAnswer.builder()
                .topic(topic)
                .user(user3)
                .content("답변 1")
                .isSubmitted(true)
                .build();
        TopicAnswer answer2 = TopicAnswer.builder()
                .topic(topic)
                .user(user4)
                .content("  ")
                .isSubmitted(true)
                .build();
        TopicAnswer answer3 = TopicAnswer.builder()
                .topic(topic)
                .user(user5)
                .content("답변 3")
                .isSubmitted(true)
                .build();

        doNothing().when(gatheringValidator).validateGathering(gatheringId);
        doNothing().when(meetingValidator).validateMeetingInGathering(meetingId, gatheringId);
        doNothing().when(meetingValidator).validateMeetingMember(meetingId, 1L);
        when(topicValidator.getTopicInMeeting(topicId, meetingId)).thenReturn(topic);
        when(topicAnswerRepository.findSubmittedByTopicId(topicId))
                .thenReturn(List.of(answer1, answer2, answer3));
        when(aiSummaryClient.requestTopicSummary(org.mockito.ArgumentMatchers.any()))
                .thenReturn("ok");

        String result = topicSummaryService.requestTopicSummary(gatheringId, meetingId, topicId);

        ArgumentCaptor<TopicSummaryRequest> captor = ArgumentCaptor.forClass(TopicSummaryRequest.class);
        verify(aiSummaryClient).requestTopicSummary(captor.capture());

        TopicSummaryRequest request = captor.getValue();
        assertThat(request.topicId()).isEqualTo(topicId);
        assertThat(request.topicTitle()).isEqualTo("데미안 토론");
        assertThat(request.answers()).containsExactly(
                new TopicSummaryRequest.Answer(3L, "답변 1"),
                new TopicSummaryRequest.Answer(5L, "답변 3")
        );
        assertThat(result).isEqualTo("ok");
    }

    @Test
    @DisplayName("제출된 답변이 비어있으면 빈 answers로 AI 요청한다")
    void requestTopicSummary_sendsEmptyAnswersWhenNoContent() {
        Long gatheringId = 1L;
        Long meetingId = 2L;
        Long topicId = 5L;
        Topic topic = Topic.builder().id(topicId).title("빈 답변 토픽").build();

        User user3 = User.builder().id(3L).kakaoId(3L).build();
        TopicAnswer answer1 = TopicAnswer.builder()
                .topic(topic)
                .user(user3)
                .content("   ")
                .isSubmitted(true)
                .build();

        doNothing().when(gatheringValidator).validateGathering(gatheringId);
        doNothing().when(meetingValidator).validateMeetingInGathering(meetingId, gatheringId);
        doNothing().when(meetingValidator).validateMeetingMember(meetingId, 1L);
        when(topicValidator.getTopicInMeeting(topicId, meetingId)).thenReturn(topic);
        when(topicAnswerRepository.findSubmittedByTopicId(topicId))
                .thenReturn(List.of(answer1));
        when(aiSummaryClient.requestTopicSummary(org.mockito.ArgumentMatchers.any()))
                .thenReturn("ok");

        topicSummaryService.requestTopicSummary(gatheringId, meetingId, topicId);

        ArgumentCaptor<TopicSummaryRequest> captor = ArgumentCaptor.forClass(TopicSummaryRequest.class);
        verify(aiSummaryClient).requestTopicSummary(captor.capture());
        assertThat(captor.getValue().answers()).isEmpty();
    }

    @Test
    @DisplayName("제출된 답변이 없으면 빈 answers로 AI 요청한다")
    void requestTopicSummary_sendsEmptyAnswersWhenNoSubmittedAnswer() {
        Long gatheringId = 1L;
        Long meetingId = 2L;
        Long topicId = 5L;
        Topic topic = Topic.builder().id(topicId).title("빈 답변 토픽").build();

        doNothing().when(gatheringValidator).validateGathering(gatheringId);
        doNothing().when(meetingValidator).validateMeetingInGathering(meetingId, gatheringId);
        doNothing().when(meetingValidator).validateMeetingMember(meetingId, 1L);
        when(topicValidator.getTopicInMeeting(topicId, meetingId)).thenReturn(topic);
        when(topicAnswerRepository.findSubmittedByTopicId(topicId))
                .thenReturn(List.of());
        when(aiSummaryClient.requestTopicSummary(org.mockito.ArgumentMatchers.any()))
                .thenReturn("ok");

        topicSummaryService.requestTopicSummary(gatheringId, meetingId, topicId);

        ArgumentCaptor<TopicSummaryRequest> captor = ArgumentCaptor.forClass(TopicSummaryRequest.class);
        verify(aiSummaryClient).requestTopicSummary(captor.capture());
        assertThat(captor.getValue().answers()).isEmpty();
    }

    @Test
    @DisplayName("인증 정보가 없으면 예외가 발생한다")
    void requestTopicSummary_throwsWhenUnauthenticated() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> topicSummaryService.requestTopicSummary(1L, 2L, 3L))
                .isInstanceOf(GlobalException.class);
    }

    @Test
    @DisplayName("AI 호출 실패 시 예외가 그대로 전파된다")
    void requestTopicSummary_throwsWhenAiClientFails() {
        Long gatheringId = 1L;
        Long meetingId = 2L;
        Long topicId = 5L;
        Topic topic = Topic.builder().id(topicId).title("데미안 토론").build();

        doNothing().when(gatheringValidator).validateGathering(gatheringId);
        doNothing().when(meetingValidator).validateMeetingInGathering(meetingId, gatheringId);
        doNothing().when(meetingValidator).validateMeetingMember(meetingId, 1L);
        when(topicValidator.getTopicInMeeting(topicId, meetingId)).thenReturn(topic);
        when(topicAnswerRepository.findSubmittedByTopicId(topicId))
                .thenReturn(List.of());
        when(aiSummaryClient.requestTopicSummary(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new IllegalStateException("ai fail"));

        assertThatThrownBy(() -> topicSummaryService.requestTopicSummary(gatheringId, meetingId, topicId))
                .isInstanceOf(IllegalStateException.class);
    }
}
