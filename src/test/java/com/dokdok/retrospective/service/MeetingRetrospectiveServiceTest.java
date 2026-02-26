package com.dokdok.retrospective.service;

import com.dokdok.gathering.entity.Gathering;
import com.dokdok.gathering.exception.GatheringErrorCode;
import com.dokdok.gathering.exception.GatheringException;
import com.dokdok.global.response.CursorResponse;
import com.dokdok.global.util.SecurityUtil;
import com.dokdok.meeting.entity.Meeting;
import com.dokdok.meeting.exception.MeetingErrorCode;
import com.dokdok.meeting.exception.MeetingException;
import com.dokdok.meeting.service.MeetingValidator;
import com.dokdok.oauth2.CustomOAuth2User;
import com.dokdok.retrospective.dto.request.MeetingRetrospectiveRequest;
import com.dokdok.retrospective.dto.response.CollectedAnswersCursor;
import com.dokdok.retrospective.dto.response.MeetingRetrospectiveResponse;
import com.dokdok.retrospective.dto.response.MemberAnswerResponse;
import com.dokdok.retrospective.entity.MeetingRetrospective;
import com.dokdok.retrospective.entity.TopicRetrospectiveSummary;
import com.dokdok.retrospective.exception.RetrospectiveErrorCode;
import com.dokdok.retrospective.exception.RetrospectiveException;
import com.dokdok.retrospective.repository.RetrospectiveRepository;
import com.dokdok.retrospective.repository.TopicRetrospectiveSummaryRepository;
import com.dokdok.storage.service.StorageService;
import com.dokdok.topic.entity.Topic;
import com.dokdok.topic.entity.TopicAnswer;
import com.dokdok.topic.entity.TopicStatus;
import com.dokdok.topic.repository.TopicAnswerRepository;
import com.dokdok.topic.repository.TopicRepository;
import com.dokdok.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingRetrospectiveServiceTest {

	@Mock
	private TopicRepository topicRepository;

	@Mock
	private RetrospectiveValidator retrospectiveValidator;

	@Mock
	private TopicRetrospectiveSummaryRepository topicRetrospectiveSummaryRepository;

	@Mock
	private RetrospectiveRepository retrospectiveRepository;

	@Mock
	private StorageService storageService;

	@Mock
	private TopicAnswerRepository topicAnswerRepository;

	@InjectMocks
	private MeetingRetrospectiveService meetingRetrospectiveService;

	@Mock
	private MeetingValidator meetingValidator;

	@Test
	@DisplayName("약속이 없으면 예외가 발생한다")
	void getMeetingRetrospective_throwsWhenMeetingNotFound() {
		Long meetingId = 999L;
		Long userId = 1L;

		try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
			securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

			when(meetingValidator.findMeetingOrThrow(meetingId))
					.thenThrow(new MeetingException(MeetingErrorCode.MEETING_NOT_FOUND));

			assertThatThrownBy(() -> meetingRetrospectiveService.getMeetingRetrospective(meetingId))
					.isInstanceOf(MeetingException.class)
					.hasFieldOrPropertyWithValue("errorCode", MeetingErrorCode.MEETING_NOT_FOUND);
		}
	}

	@Test
	@DisplayName("모임 멤버가 아니면 예외가 발생한다")
	void getMeetingRetrospective_throwsWhenNotGatheringMember() {
		Long meetingId = 1L;
		Long userId = 999L;
		Long gatheringId = 1L;

		Gathering gathering = Gathering.builder().id(gatheringId).build();
		Meeting meeting = Meeting.builder()
				.id(meetingId)
				.gathering(gathering)
				.retrospectivePublished(true)
				.build();

		try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
			securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

			when(meetingValidator.findMeetingOrThrow(meetingId)).thenReturn(meeting);
			doThrow(new GatheringException(GatheringErrorCode.NOT_GATHERING_MEMBER))
					.when(retrospectiveValidator).validateMeetingRetrospectiveAccess(gatheringId, meetingId, userId);

			assertThatThrownBy(() -> meetingRetrospectiveService.getMeetingRetrospective(meetingId))
					.isInstanceOf(GatheringException.class)
					.hasFieldOrPropertyWithValue("errorCode", GatheringErrorCode.NOT_GATHERING_MEMBER);
		}
	}

	@Test
	@DisplayName("약속 멤버가 아니면 예외가 발생한다")
	void getMeetingRetrospective_throwsWhenNotMeetingMember() {
		Long meetingId = 1L;
		Long userId = 999L;
		Long gatheringId = 1L;

		Gathering gathering = Gathering.builder().id(gatheringId).build();
		Meeting meeting = Meeting.builder()
				.id(meetingId)
				.gathering(gathering)
				.retrospectivePublished(true)
				.build();

		try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
			securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

			when(meetingValidator.findMeetingOrThrow(meetingId)).thenReturn(meeting);
			doThrow(new MeetingException(MeetingErrorCode.NOT_GATHERING_MEETING))
					.when(retrospectiveValidator).validateMeetingRetrospectiveAccess(gatheringId, meetingId, userId);

			assertThatThrownBy(() -> meetingRetrospectiveService.getMeetingRetrospective(meetingId))
					.isInstanceOf(MeetingException.class)
					.hasFieldOrPropertyWithValue("errorCode", MeetingErrorCode.NOT_GATHERING_MEETING);
		}
	}

	@Test
	@DisplayName("퍼블리시되지 않은 약속 회고 조회 시 예외가 발생한다")
	void getMeetingRetrospective_throwsWhenNotPublished() {
		Long meetingId = 1L;
		Long userId = 1L;
		Long gatheringId = 1L;

		Gathering gathering = Gathering.builder().id(gatheringId).build();
		Meeting meeting = Meeting.builder()
				.id(meetingId)
				.gathering(gathering)
				.retrospectivePublished(false)
				.build();

		try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
			securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

			when(meetingValidator.findMeetingOrThrow(meetingId)).thenReturn(meeting);

			assertThatThrownBy(() -> meetingRetrospectiveService.getMeetingRetrospective(meetingId))
					.isInstanceOf(RetrospectiveException.class)
					.hasFieldOrPropertyWithValue("errorCode", RetrospectiveErrorCode.RETROSPECTIVE_NOT_PUBLISHED);
		}
	}

	@Test
	@DisplayName("코멘트가 없어도 정상적으로 조회한다")
	void getMeetingRetrospective_withNoComments_success() {
		Long meetingId = 1L;
		Long userId = 1L;
		Long gatheringId = 1L;

		User leader = User.builder().id(userId).nickname("리더").build();
		Gathering gathering = Gathering.builder().id(gatheringId).build();
		Meeting meeting = Meeting.builder()
				.id(meetingId)
				.gathering(gathering)
				.meetingLeader(leader)
				.meetingName("모임")
				.meetingStartDate(LocalDateTime.of(2026, 1, 15, 19, 0))
				.meetingEndDate(LocalDateTime.of(2026, 1, 15, 21, 0))
				.retrospectivePublished(true)
				.build();

		Topic topic = Topic.builder().id(1L).meeting(meeting).title("토픽1").build();
		TopicRetrospectiveSummary summary = TopicRetrospectiveSummary.builder()
				.id(1L)
				.topic(topic)
				.summary("요약1")
				.keyPoints(List.of(new TopicRetrospectiveSummary.KeyPoint(
						"핵심 포인트",
						List.of("포인트 상세")
				)))
				.build();

		try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
			securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

			when(meetingValidator.findMeetingOrThrow(meetingId)).thenReturn(meeting);
			doNothing().when(retrospectiveValidator).validateMeetingRetrospectiveAccess(gatheringId, meetingId, userId);
			when(topicRepository.findByMeetingIdAndTopicStatusOrderByConfirmOrderAsc(meetingId, TopicStatus.CONFIRMED))
					.thenReturn(List.of(topic));
			when(topicRetrospectiveSummaryRepository.findAllByTopicIdIn(List.of(1L))).thenReturn(List.of(summary));

			MeetingRetrospectiveResponse response = meetingRetrospectiveService.getMeetingRetrospective(meetingId);

			assertThat(response.topics()).hasSize(1);
			assertThat(response.topics().get(0).summary()).isEqualTo("요약1");
			assertThat(response.topics().get(0).keyPoints()).hasSize(1);
			assertThat(response.topics().get(0).keyPoints().get(0).title()).isEqualTo("핵심 포인트");
		}
	}

	@Test
	@DisplayName("퍼블리시되지 않은 약속에 코멘트 작성 시 예외가 발생한다")
	void createMeetingRetrospective_throwsWhenNotPublished() {
		Long meetingId = 1L;
		Long userId = 1L;
		Long gatheringId = 1L;

		Gathering gathering = Gathering.builder().id(gatheringId).build();
		Meeting meeting = Meeting.builder()
				.id(meetingId)
				.gathering(gathering)
				.retrospectivePublished(false)
				.build();
		User user = User.builder().id(userId).build();
		MeetingRetrospectiveRequest request = new MeetingRetrospectiveRequest("코멘트");

		CustomOAuth2User customOAuth2User = mock(CustomOAuth2User.class);
		when(customOAuth2User.getUser()).thenReturn(user);

		try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
			securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
			securityUtilMock.when(SecurityUtil::getCurrentUser).thenReturn(customOAuth2User);

			when(meetingValidator.findMeetingOrThrow(meetingId)).thenReturn(meeting);

			assertThatThrownBy(() -> meetingRetrospectiveService.createMeetingRetrospective(meetingId, request))
					.isInstanceOf(RetrospectiveException.class)
					.hasFieldOrPropertyWithValue("errorCode", RetrospectiveErrorCode.RETROSPECTIVE_NOT_PUBLISHED);

			verify(retrospectiveRepository, never()).save(any());
		}
	}

	@Test
	@DisplayName("공동 회고를 정상적으로 작성한다")
	void createMeetingRetrospective_success() {
		// given
		Long meetingId = 1L;
		Long userId = 1L;
		Long gatheringId = 1L;

		Gathering gathering = Gathering.builder().id(gatheringId).build();
		Meeting meeting = Meeting.builder()
				.id(meetingId)
				.gathering(gathering)
				.retrospectivePublished(true)
				.build();
		User user = User.builder().id(userId).nickname("사용자1").profileImageUrl("https://image.jpg").build();

		MeetingRetrospectiveRequest request = new MeetingRetrospectiveRequest("회고 코멘트입니다.");

		MeetingRetrospective saved = MeetingRetrospective.builder()
				.id(1L)
				.meeting(meeting)
				.createdBy(user)
				.comment("회고 코멘트입니다.")
				.build();

		CustomOAuth2User customOAuth2User = mock(CustomOAuth2User.class);
		when(customOAuth2User.getUser()).thenReturn(user);

		try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
			securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
			securityUtilMock.when(SecurityUtil::getCurrentUser).thenReturn(customOAuth2User);

			when(meetingValidator.findMeetingOrThrow(meetingId)).thenReturn(meeting);
			doNothing().when(retrospectiveValidator).validateMeetingRetrospectiveAccess(gatheringId, meetingId, userId);
			when(retrospectiveRepository.save(any(MeetingRetrospective.class))).thenReturn(saved);
			when(storageService.getPresignedProfileImage("https://image.jpg")).thenReturn("https://image.jpg");

			// when
			MeetingRetrospectiveResponse.CommentResponse response =
					meetingRetrospectiveService.createMeetingRetrospective(meetingId, request);

			// then
			assertThat(response.commentId()).isEqualTo(1L);
			assertThat(response.userId()).isEqualTo(userId);
			assertThat(response.comment()).isEqualTo("회고 코멘트입니다.");

			verify(meetingValidator).findMeetingOrThrow(meetingId);
			verify(retrospectiveValidator).validateMeetingRetrospectiveAccess(gatheringId, meetingId, userId);
			verify(retrospectiveRepository).save(any(MeetingRetrospective.class));
		}
	}

	@Test
	@DisplayName("공동 회고 작성 시 약속이 없으면 예외가 발생한다")
	void createMeetingRetrospective_throwsWhenMeetingNotFound() {
		Long meetingId = 999L;
		Long userId = 1L;
		User user = User.builder().id(userId).build();
		MeetingRetrospectiveRequest request = new MeetingRetrospectiveRequest("코멘트");

		CustomOAuth2User customOAuth2User = mock(CustomOAuth2User.class);
		when(customOAuth2User.getUser()).thenReturn(user);

		try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
			securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
			securityUtilMock.when(SecurityUtil::getCurrentUser).thenReturn(customOAuth2User);

			when(meetingValidator.findMeetingOrThrow(meetingId))
					.thenThrow(new MeetingException(MeetingErrorCode.MEETING_NOT_FOUND));

			assertThatThrownBy(() -> meetingRetrospectiveService.createMeetingRetrospective(meetingId, request))
					.isInstanceOf(MeetingException.class)
					.hasFieldOrPropertyWithValue("errorCode", MeetingErrorCode.MEETING_NOT_FOUND);

			verify(retrospectiveRepository, never()).save(any());
		}
	}

	@Test
	@DisplayName("공동 회고 작성 시 권한이 없으면 예외가 발생한다")
	void createMeetingRetrospective_throwsWhenNoAccess() {
		Long meetingId = 1L;
		Long userId = 999L;
		Long gatheringId = 1L;

		Gathering gathering = Gathering.builder().id(gatheringId).build();
		Meeting meeting = Meeting.builder()
				.id(meetingId)
				.gathering(gathering)
				.retrospectivePublished(true)
				.build();
		User user = User.builder().id(userId).build();
		MeetingRetrospectiveRequest request = new MeetingRetrospectiveRequest( "코멘트");

		CustomOAuth2User customOAuth2User = mock(CustomOAuth2User.class);
		when(customOAuth2User.getUser()).thenReturn(user);

		try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
			securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
			securityUtilMock.when(SecurityUtil::getCurrentUser).thenReturn(customOAuth2User);

			when(meetingValidator.findMeetingOrThrow(meetingId)).thenReturn(meeting);
			doThrow(new GatheringException(GatheringErrorCode.NOT_GATHERING_MEMBER))
					.when(retrospectiveValidator).validateMeetingRetrospectiveAccess(gatheringId, meetingId, userId);

			assertThatThrownBy(() -> meetingRetrospectiveService.createMeetingRetrospective(meetingId, request))
					.isInstanceOf(GatheringException.class)
					.hasFieldOrPropertyWithValue("errorCode", GatheringErrorCode.NOT_GATHERING_MEMBER);

			verify(retrospectiveRepository, never()).save(any());
		}
	}

	@Test
	@DisplayName("공동 회고 삭제를 정상적으로 수행한다")
	void deleteMeetingRetrospective_success() {
		Long meetingId = 1L;
		Long commentId = 10L;
		Long userId = 1L;

		Gathering gathering = Gathering.builder().id(1L).build();
		Meeting meeting = Meeting.builder().id(meetingId).gathering(gathering).build();
		User user = User.builder().id(userId).build();
		MeetingRetrospective retrospective = MeetingRetrospective.builder()
				.id(commentId)
				.meeting(meeting)
				.createdBy(user)
				.build();

		try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
			securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

			when(retrospectiveRepository.findByIdAndMeetingId(commentId, meetingId))
					.thenReturn(Optional.of(retrospective));
			doNothing().when(retrospectiveValidator)
					.validateMeetingRetrospectiveDeletePermission(retrospective, userId);

			meetingRetrospectiveService.deleteMeetingRetrospective(meetingId, commentId);

			verify(retrospectiveRepository).delete(retrospective);
		}
	}

	@Test
	@DisplayName("공동 회고가 없으면 삭제 시 예외가 발생한다")
	void deleteMeetingRetrospective_throwsWhenNotFound() {
		Long meetingId = 1L;
		Long commentId = 10L;
		Long userId = 1L;

		try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
			securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

			when(retrospectiveRepository.findByIdAndMeetingId(commentId, meetingId))
					.thenReturn(Optional.empty());

			assertThatThrownBy(() -> meetingRetrospectiveService.deleteMeetingRetrospective(meetingId, commentId))
					.isInstanceOf(RetrospectiveException.class)
					.hasFieldOrPropertyWithValue("errorCode", RetrospectiveErrorCode.MEETING_RETROSPECTIVE_NOT_FOUND);

			verify(retrospectiveRepository, never()).delete(any());
		}
	}

	@Test
	@DisplayName("공동 회고 삭제 권한이 없으면 예외가 발생한다")
	void deleteMeetingRetrospective_throwsWhenForbidden() {
		Long meetingId = 1L;
		Long commentId = 10L;
		Long userId = 2L;

		Gathering gathering = Gathering.builder().id(1L).build();
		Meeting meeting = Meeting.builder().id(meetingId).gathering(gathering).build();
		User user = User.builder().id(1L).build();
		MeetingRetrospective retrospective = MeetingRetrospective.builder()
				.id(commentId)
				.meeting(meeting)
				.createdBy(user)
				.build();

		try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
			securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

			when(retrospectiveRepository.findByIdAndMeetingId(commentId, meetingId))
					.thenReturn(Optional.of(retrospective));
			doThrow(new GatheringException(GatheringErrorCode.NOT_GATHERING_LEADER))
					.when(retrospectiveValidator).validateMeetingRetrospectiveDeletePermission(retrospective, userId);

			assertThatThrownBy(() -> meetingRetrospectiveService.deleteMeetingRetrospective(meetingId, commentId))
					.isInstanceOf(GatheringException.class)
					.hasFieldOrPropertyWithValue("errorCode", GatheringErrorCode.NOT_GATHERING_LEADER);

			verify(retrospectiveRepository, never()).delete(any());
		}
	}

	@Test
	@DisplayName("수집된 사전 의견 조회 - 첫 페이지")
	void getCollectedAnswers_firstPage_success() {
		Long meetingId = 1L;
		Long userId = 1L;
		int pageSize = 1;

		User leader = User.builder().id(userId).nickname("리더").profileImageUrl("leader.jpg").build();
		Meeting meeting = Meeting.builder().id(meetingId).meetingLeader(leader).build();
		Topic topic = Topic.builder().id(10L).title("주제").build();
		TopicAnswer answer = TopicAnswer.builder()
				.id(100L)
				.topic(topic)
				.user(leader)
				.content("답변")
				.isSubmitted(true)
				.build();

		try (MockedStatic<SecurityUtil> securityUtilMock = mockStatic(SecurityUtil.class)) {
			securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

			when(meetingValidator.findMeetingOrThrow(meetingId)).thenReturn(meeting);
			when(topicAnswerRepository.findDistinctUserIdsByMeetingIdFirstPage(eq(meetingId), any(Pageable.class)))
					.thenReturn(List.of(userId, 2L));
			when(topicAnswerRepository.findSubmittedAnswersByMeetingIdAndUserIds(meetingId, List.of(userId)))
					.thenReturn(List.of(answer));
			when(topicAnswerRepository.countSubmittedAnswersByMeetingId(meetingId)).thenReturn(2);
			when(storageService.getPresignedProfileImage("leader.jpg")).thenReturn("leader.jpg");

			CursorResponse<MemberAnswerResponse, CollectedAnswersCursor> response =
					meetingRetrospectiveService.getCollectedAnswers(meetingId, pageSize, null);

			assertThat(response.items()).hasSize(1);
			assertThat(response.items().get(0).userId()).isEqualTo(userId);
			assertThat(response.hasNext()).isTrue();
			assertThat(response.nextCursor().userId()).isEqualTo(userId);
			assertThat(response.totalCount()).isEqualTo(2);
		}
	}
}
