package com.dokdok.topic.service;

import com.dokdok.book.dto.request.BookReviewRequest;
import com.dokdok.book.dto.response.BookReviewResponse;
import com.dokdok.book.repository.BookReviewRepository;
import com.dokdok.book.service.BookReviewService;
import com.dokdok.gathering.service.GatheringValidator;
import com.dokdok.meeting.entity.Meeting;
import com.dokdok.meeting.service.MeetingValidator;
import com.dokdok.topic.dto.request.TopicAnswerBulkSaveRequest;
import com.dokdok.topic.dto.request.TopicAnswerBulkSubmitRequest;
import com.dokdok.topic.dto.response.PreOpinionSaveResponse;
import com.dokdok.topic.dto.response.PreOpinionSubmitResponse;
import com.dokdok.topic.entity.Topic;
import com.dokdok.topic.entity.TopicAnswer;
import com.dokdok.topic.exception.TopicException;
import com.dokdok.topic.repository.TopicAnswerRepository;
import com.dokdok.topic.repository.TopicRepository;
import com.dokdok.user.entity.User;
import com.dokdok.global.exception.GlobalException;
import java.util.List;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TopicAnswerServiceTest {

    @Mock
    private TopicAnswerRepository topicAnswerRepository;

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private GatheringValidator gatheringValidator;

    @Mock
    private MeetingValidator meetingValidator;

    @Mock
    private BookReviewRepository bookReviewRepository;

    @Mock
    private BookReviewService bookReviewService;

    @InjectMocks
    private TopicAnswerService topicAnswerService;

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
    @DisplayName("토픽 답변 일괄 저장 시 저장 요청과 응답 DTO를 확인한다")
    void createAnswer_savesAnswerAndReturnsResponse() {
        Topic topic = Topic.builder().id(12L).build();
        TopicAnswer saved = TopicAnswer.builder()
                .id(100L)
                .topic(topic)
                .content("이 책을 읽고 ...")
                .isSubmitted(false)
                .build();

        given(topicRepository.findAllByIdInAndMeetingId(List.of(12L), 1L))
                .willReturn(List.of(topic));
        given(topicAnswerRepository.findByMeetingIdUserId(1L, 1L))
                .willReturn(List.of());
        given(topicAnswerRepository.save(any(TopicAnswer.class)))
                .willReturn(saved);
        given(meetingValidator.findMeetingOrThrow(1L))
                .willReturn(Meeting.builder().book(com.dokdok.book.entity.Book.builder().id(10L).build()).build());
        given(bookReviewRepository.findByBookIdAndUserId(10L, 1L))
                .willReturn(Optional.empty());
        BookReviewResponse reviewResponse = new BookReviewResponse(1L, 10L, 1L, BigDecimal.valueOf(4.5), List.of());
        given(bookReviewService.createReview(eq(10L), any(BookReviewRequest.class)))
                .willReturn(reviewResponse);

        TopicAnswerBulkSaveRequest request = new TopicAnswerBulkSaveRequest(
                new BookReviewRequest(BigDecimal.valueOf(4.5), List.of(1L)),
                List.of(new TopicAnswerBulkSaveRequest.AnswerItem(12L, "이 책을 읽고 ..."))
        );

        PreOpinionSaveResponse response = topicAnswerService.createAnswer(
                1L, 1L, request
        );

        ArgumentCaptor<TopicAnswer> captor = ArgumentCaptor.forClass(TopicAnswer.class);
        verify(topicAnswerRepository).save(captor.capture());

        TopicAnswer captured = captor.getValue();
        assertThat(captured.getTopic()).isEqualTo(topic);
        assertThat(captured.getUser().getId()).isEqualTo(1L);
        assertThat(captured.getContent()).isEqualTo("이 책을 읽고 ...");

        assertThat(response.review().reviewId()).isEqualTo(1L);
        assertThat(response.answers()).hasSize(1);
        assertThat(response.answers().get(0).topicId()).isEqualTo(12L);
        assertThat(response.answers().get(0).isSubmitted()).isFalse();
    }

    @Test
    @DisplayName("토픽이 없으면 예외가 발생한다")
    void createAnswer_throwsWhenTopicMissing() {
        given(topicRepository.findAllByIdInAndMeetingId(List.of(12L), 1L))
                .willReturn(List.of());
        TopicAnswerBulkSaveRequest request = new TopicAnswerBulkSaveRequest(
                new BookReviewRequest(BigDecimal.valueOf(4.5), List.of(1L)),
                List.of(new TopicAnswerBulkSaveRequest.AnswerItem(12L, "이 책을 읽고 ..."))
        );

        assertThatThrownBy(() -> topicAnswerService.createAnswer(
                1L, 1L, request
        )).isInstanceOf(TopicException.class);

        verifyNoInteractions(topicAnswerRepository);
    }

    @Test
    @DisplayName("인증 정보가 없으면 예외가 발생한다")
    void createAnswer_throwsWhenUnauthenticated() {
        SecurityContextHolder.clearContext();
        TopicAnswerBulkSaveRequest request = new TopicAnswerBulkSaveRequest(
                new BookReviewRequest(BigDecimal.valueOf(4.5), List.of(1L)),
                List.of(new TopicAnswerBulkSaveRequest.AnswerItem(12L, "이 책을 읽고 ..."))
        );

        assertThatThrownBy(() -> topicAnswerService.createAnswer(
                1L, 1L, request
        )).isInstanceOf(GlobalException.class);
    }

    @Test
    @DisplayName("토픽 답변 일괄 저장 시 기존 답변이 갱신된다")
    void updateMyAnswer_updatesContentAndReturnsResponse() {
        Topic topic = Topic.builder().id(12L).build();
        User user = User.builder().id(1L).build();
        TopicAnswer answer = TopicAnswer.builder()
                .id(100L)
                .topic(topic)
                .user(user)
                .content("기존 내용")
                .isSubmitted(false)
                .build();

        given(topicRepository.findAllByIdInAndMeetingId(List.of(12L), 1L))
                .willReturn(List.of(topic));
        given(topicAnswerRepository.findByMeetingIdUserId(1L, 1L))
                .willReturn(List.of(answer));
        given(meetingValidator.findMeetingOrThrow(1L))
                .willReturn(Meeting.builder().book(com.dokdok.book.entity.Book.builder().id(10L).build()).build());
        given(bookReviewRepository.findByBookIdAndUserId(10L, 1L))
                .willReturn(Optional.empty());
        given(bookReviewService.createReview(eq(10L), any(BookReviewRequest.class)))
                .willReturn(new BookReviewResponse(1L, 10L, 1L, BigDecimal.valueOf(4.5), List.of()));

        TopicAnswerBulkSaveRequest request = new TopicAnswerBulkSaveRequest(
                new BookReviewRequest(BigDecimal.valueOf(4.5), List.of(1L)),
                List.of(new TopicAnswerBulkSaveRequest.AnswerItem(12L, "수정된 내용"))
        );

        PreOpinionSaveResponse response = topicAnswerService.updateMyAnswer(
                1L, 1L, request
        );

        assertThat(answer.getContent()).isEqualTo("수정된 내용");
        assertThat(response.answers()).hasSize(1);
        assertThat(response.answers().get(0).topicId()).isEqualTo(12L);
        assertThat(response.answers().get(0).isSubmitted()).isFalse();
    }

    @Test
    @DisplayName("이미 제출된 답변은 수정할 수 없다")
    void updateMyAnswer_throwsWhenAlreadySubmitted() {
        Topic topic = Topic.builder().id(12L).build();
        User user = User.builder().id(1L).build();
        TopicAnswer answer = TopicAnswer.builder()
                .id(100L)
                .topic(topic)
                .user(user)
                .content("기존 내용")
                .isSubmitted(true)
                .build();

        given(topicRepository.findAllByIdInAndMeetingId(List.of(12L), 1L))
                .willReturn(List.of(topic));
        given(topicAnswerRepository.findByMeetingIdUserId(1L, 1L))
                .willReturn(List.of(answer));
        given(meetingValidator.findMeetingOrThrow(1L))
                .willReturn(Meeting.builder().book(com.dokdok.book.entity.Book.builder().id(10L).build()).build());
        given(bookReviewRepository.findByBookIdAndUserId(10L, 1L))
                .willReturn(Optional.empty());
        given(bookReviewService.createReview(eq(10L), any(BookReviewRequest.class)))
                .willReturn(new BookReviewResponse(1L, 10L, 1L, BigDecimal.valueOf(4.5), List.of()));

        TopicAnswerBulkSaveRequest request = new TopicAnswerBulkSaveRequest(
                new BookReviewRequest(BigDecimal.valueOf(4.5), List.of(1L)),
                List.of(new TopicAnswerBulkSaveRequest.AnswerItem(12L, "수정된 내용"))
        );

        assertThatThrownBy(() -> topicAnswerService.updateMyAnswer(
                1L, 1L, request
        )).isInstanceOf(TopicException.class);
    }

    @Test
    @DisplayName("토픽 답변 일괄 제출 시 제출 상태로 변경된다")
    void submitMyAnswer_updatesSubmittedState() {
        Topic topic = Topic.builder().id(12L).build();
        User user = User.builder().id(1L).build();
        TopicAnswer answer = TopicAnswer.builder()
                .id(100L)
                .topic(topic)
                .user(user)
                .content("기존 내용")
                .isSubmitted(false)
                .build();

        given(topicRepository.findAllByIdInAndMeetingId(List.of(12L), 1L))
                .willReturn(List.of(topic));
        given(topicAnswerRepository.findByMeetingIdUserId(1L, 1L))
                .willReturn(List.of(answer));
        given(meetingValidator.findMeetingOrThrow(1L))
                .willReturn(Meeting.builder().book(com.dokdok.book.entity.Book.builder().id(10L).build()).build());
        given(bookReviewRepository.findByBookIdAndUserId(10L, 1L))
                .willReturn(Optional.empty());
        given(bookReviewService.createReview(eq(10L), any(BookReviewRequest.class)))
                .willReturn(new BookReviewResponse(1L, 10L, 1L, BigDecimal.valueOf(4.5), List.of()));

        TopicAnswerBulkSubmitRequest request = new TopicAnswerBulkSubmitRequest(
                new BookReviewRequest(BigDecimal.valueOf(4.5), List.of(1L)),
                List.of(12L)
        );
        PreOpinionSubmitResponse response =
                topicAnswerService.submitMyAnswer(1L, 1L, request);

        assertThat(answer.getIsSubmitted()).isTrue();
        assertThat(response.answers()).hasSize(1);
        assertThat(response.answers().get(0).topicId()).isEqualTo(12L);
        assertThat(response.answers().get(0).isSubmitted()).isTrue();
    }

    @Test
    @DisplayName("이미 제출된 답변은 다시 제출할 수 없다")
    void submitMyAnswer_throwsWhenAlreadySubmitted() {
        Topic topic = Topic.builder().id(12L).build();
        User user = User.builder().id(1L).build();
        TopicAnswer answer = TopicAnswer.builder()
                .id(100L)
                .topic(topic)
                .user(user)
                .content("기존 내용")
                .isSubmitted(true)
                .build();

        given(topicRepository.findAllByIdInAndMeetingId(List.of(12L), 1L))
                .willReturn(List.of(topic));
        given(topicAnswerRepository.findByMeetingIdUserId(1L, 1L))
                .willReturn(List.of(answer));

        TopicAnswerBulkSubmitRequest request = new TopicAnswerBulkSubmitRequest(
                new BookReviewRequest(BigDecimal.valueOf(4.5), List.of(1L)),
                List.of(12L)
        );
        assertThatThrownBy(() -> topicAnswerService.submitMyAnswer(
                1L, 1L, request
        )).isInstanceOf(TopicException.class);
    }

}
