package com.dokdok.topic.service;

import com.dokdok.book.entity.BookReview;
import com.dokdok.book.entity.BookReviewKeyword;
import com.dokdok.book.entity.KeywordType;
import com.dokdok.book.repository.BookReviewKeywordRepository;
import com.dokdok.book.repository.BookReviewRepository;
import com.dokdok.gathering.entity.GatheringMember;
import com.dokdok.gathering.entity.GatheringRole;
import com.dokdok.gathering.repository.GatheringMemberRepository;
import com.dokdok.gathering.service.GatheringValidator;
import com.dokdok.global.exception.GlobalException;
import com.dokdok.keyword.entity.Keyword;
import com.dokdok.meeting.entity.MeetingMember;
import com.dokdok.meeting.entity.MeetingMemberRole;
import com.dokdok.meeting.repository.MeetingMemberRepository;
import com.dokdok.meeting.service.MeetingValidator;
import com.dokdok.storage.service.StorageService;
import com.dokdok.topic.dto.response.PreOpinionResponse;
import com.dokdok.topic.entity.Topic;
import com.dokdok.topic.entity.TopicAnswer;
import com.dokdok.topic.entity.TopicType;
import com.dokdok.topic.exception.TopicErrorCode;
import com.dokdok.topic.exception.TopicException;
import com.dokdok.topic.repository.TopicAnswerRepository;
import com.dokdok.topic.repository.TopicRepository;
import com.dokdok.user.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PreOpinionServiceTest {

    @Mock
    private GatheringMemberRepository gatheringMemberRepository;

    @Mock
    private GatheringValidator gatheringValidator;

    @Mock
    private MeetingValidator meetingValidator;

    @Mock
    private TopicValidator topicValidator;

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private MeetingMemberRepository meetingMemberRepository;

    @Mock
    private TopicAnswerRepository topicAnswerRepository;

    @Mock
    private BookReviewRepository bookReviewRepository;

    @Mock
    private BookReviewKeywordRepository bookReviewKeywordRepository;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private PreOpinionService preOpinionService;

    private static final Long GATHERING_ID = 1L;
    private static final Long MEETING_ID = 10L;
    private static final String PRESIGNED_URL = "https://presigned-url.com/profile.jpg";

    @BeforeEach
    void setUpSecurityContext() {
        User user = User.builder()
                .id(1L)
                .nickname("tester")
                .kakaoId(1L)
                .build();
        com.dokdok.oauth2.CustomOAuth2User principal = com.dokdok.oauth2.CustomOAuth2User.builder()
                .user(user)
                .attributes(Collections.emptyMap())
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

    private void stubValidators() {
        doNothing().when(gatheringValidator).validateGathering(GATHERING_ID);
        doNothing().when(meetingValidator).validateMeetingInGathering(MEETING_ID, GATHERING_ID);
        doNothing().when(meetingValidator).validateMeetingMember(MEETING_ID, 1L);
        doNothing().when(topicValidator).validateUserHasWrittenAnswer(MEETING_ID, 1L);
    }

    private User createUser(Long id, String nickname) {
        return User.builder()
                .id(id)
                .nickname(nickname)
                .kakaoId(id)
                .profileImageUrl("profile_" + id + ".jpg")
                .build();
    }

    private MeetingMember createMeetingMember(Long id, User user, MeetingMemberRole role) {
        return MeetingMember.builder()
                .id(id)
                .user(user)
                .meetingRole(role)
                .build();
    }

    private GatheringMember createGatheringMember(Long id, User user, GatheringRole role) {
        return GatheringMember.builder()
                .id(id)
                .user(user)
                .role(role)
                .build();
    }

    private Topic createTopic(Long id, String title, TopicType topicType, Integer confirmOrder) {
        return Topic.builder()
                .id(id)
                .title(title)
                .description(title + " 설명")
                .topicType(topicType)
                .confirmOrder(confirmOrder)
                .build();
    }

    private TopicAnswer createTopicAnswer(Long id, Topic topic, User user, String content, LocalDateTime createdAt) {
        TopicAnswer answer = TopicAnswer.builder()
                .id(id)
                .topic(topic)
                .user(user)
                .content(content)
                .isSubmitted(true)
                .build();
        ReflectionTestUtils.setField(answer, "createdAt", createdAt);
        return answer;
    }

    private BookReview createBookReview(Long id, User user, BigDecimal rating) {
        return BookReview.builder()
                .id(id)
                .user(user)
                .rating(rating)
                .build();
    }

    private Keyword createKeyword(Long id, String name, KeywordType type) {
        return Keyword.builder()
                .id(id)
                .keywordName(name)
                .keywordType(type)
                .level(1)
                .build();
    }

    private BookReviewKeyword createBookReviewKeyword(Long id, BookReview review, Keyword keyword) {
        return BookReviewKeyword.builder()
                .id(id)
                .bookReview(review)
                .keyword(keyword)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("사전의견 조회 성공 - 모임장과 일반 멤버의 응답을 정상 반환한다")
    void findPreOpinions_success() {
        // given
        stubValidators();

        User user1 = createUser(1L, "모임장");
        User user2 = createUser(2L, "일반멤버");

        MeetingMember mm1 = createMeetingMember(1L, user1, MeetingMemberRole.MEMBER);
        MeetingMember mm2 = createMeetingMember(2L, user2, MeetingMemberRole.MEMBER);

        Topic topic = createTopic(100L, "토론 주제", TopicType.DISCUSSION, 1);

        TopicAnswer answer1 = createTopicAnswer(
                200L, topic, user1, "찬성합니다",
                LocalDateTime.of(2025, 1, 1, 10, 0)
        );

        BookReview review1 = createBookReview(300L, user1, new BigDecimal("4.5"));

        GatheringMember gm1 = createGatheringMember(1L, user1, GatheringRole.LEADER);
        GatheringMember gm2 = createGatheringMember(2L, user2, GatheringRole.MEMBER);

        given(topicRepository.findConfirmedTopics(MEETING_ID)).willReturn(List.of(topic));
        given(meetingMemberRepository.findAllByMeetingId(MEETING_ID)).willReturn(List.of(mm1, mm2));
        given(gatheringMemberRepository.findAllMembersByGatheringId(GATHERING_ID)).willReturn(List.of(gm1, gm2));
        given(bookReviewRepository.findByUserIdIn(anyList(), anyLong())).willReturn(List.of(review1));
        given(bookReviewKeywordRepository.findByBookReviewIds(anyList())).willReturn(List.of());
        given(topicAnswerRepository.findByMeetingId(MEETING_ID)).willReturn(List.of(answer1));
        given(storageService.getPresignedProfileImage(anyString())).willReturn(PRESIGNED_URL);

        // when
        PreOpinionResponse response = preOpinionService.findPreOpinions(GATHERING_ID, MEETING_ID);

        // then
        assertThat(response.topics()).hasSize(1);
        PreOpinionResponse.TopicInfo topicInfo = response.topics().get(0);
        assertThat(topicInfo.topicType()).isEqualTo(TopicType.DISCUSSION);
        assertThat(topicInfo.topicTypeLabel()).isEqualTo("토론형");
        assertThat(topicInfo.title()).isEqualTo("토론 주제");
        assertThat(topicInfo.confirmOrder()).isEqualTo(1);

        assertThat(response.members()).hasSize(2);

        // user1 answered -> sorted first
        PreOpinionResponse.MemberPreOpinion member1Opinion = response.members().get(0);
        assertThat(member1Opinion.memberInfo().userId()).isEqualTo(1L);
        assertThat(member1Opinion.memberInfo().role()).isEqualTo("GATHERING_LEADER");
        assertThat(member1Opinion.bookReview()).isNotNull();
        assertThat(member1Opinion.topicOpinions()).isNotEmpty();
        assertThat(member1Opinion.isSubmitted()).isTrue();

        // user2 did not answer -> sorted last
        PreOpinionResponse.MemberPreOpinion member2Opinion = response.members().get(1);
        assertThat(member2Opinion.memberInfo().userId()).isEqualTo(2L);
        assertThat(member2Opinion.memberInfo().role()).isEqualTo("MEMBER");
        assertThat(member2Opinion.bookReview()).isNull();
        assertThat(member2Opinion.topicOpinions()).isEmpty();
        assertThat(member2Opinion.isSubmitted()).isFalse();
    }

    @Test
    @DisplayName("사전의견 조회 시 답변 시간 순으로 멤버가 정렬된다")
    void findPreOpinions_membersOrderedByAnswerTime() {
        // given
        stubValidators();

        User user1 = createUser(1L, "멤버1");
        User user2 = createUser(2L, "멤버2");
        User user3 = createUser(3L, "멤버3");

        MeetingMember mm1 = createMeetingMember(1L, user1, MeetingMemberRole.MEMBER);
        MeetingMember mm2 = createMeetingMember(2L, user2, MeetingMemberRole.MEMBER);
        MeetingMember mm3 = createMeetingMember(3L, user3, MeetingMemberRole.MEMBER);

        Topic topic = createTopic(100L, "자유 주제", TopicType.FREE, 1);

        // member3 answered first, member1 second, member2 never
        TopicAnswer answer3 = createTopicAnswer(
                201L, topic, user3, "가장 빨리 답변",
                LocalDateTime.of(2025, 1, 1, 9, 0)
        );
        TopicAnswer answer1 = createTopicAnswer(
                202L, topic, user1, "두번째 답변",
                LocalDateTime.of(2025, 1, 1, 11, 0)
        );

        GatheringMember gm1 = createGatheringMember(1L, user1, GatheringRole.MEMBER);
        GatheringMember gm2 = createGatheringMember(2L, user2, GatheringRole.MEMBER);
        GatheringMember gm3 = createGatheringMember(3L, user3, GatheringRole.MEMBER);

        given(topicRepository.findConfirmedTopics(MEETING_ID)).willReturn(List.of(topic));
        given(meetingMemberRepository.findAllByMeetingId(MEETING_ID)).willReturn(List.of(mm1, mm2, mm3));
        given(gatheringMemberRepository.findAllMembersByGatheringId(GATHERING_ID)).willReturn(List.of(gm1, gm2, gm3));
        given(bookReviewRepository.findByUserIdIn(anyList(), anyLong())).willReturn(List.of());
        given(bookReviewKeywordRepository.findByBookReviewIds(anyList())).willReturn(List.of());
        given(topicAnswerRepository.findByMeetingId(MEETING_ID)).willReturn(List.of(answer3, answer1));
        given(storageService.getPresignedProfileImage(anyString())).willReturn(PRESIGNED_URL);

        // when
        PreOpinionResponse response = preOpinionService.findPreOpinions(GATHERING_ID, MEETING_ID);

        // then - ordered: member3 (earliest) -> member1 (second) -> member2 (no answer)
        assertThat(response.members()).hasSize(3);
        assertThat(response.members().get(0).memberInfo().userId()).isEqualTo(3L);
        assertThat(response.members().get(1).memberInfo().userId()).isEqualTo(1L);
        assertThat(response.members().get(2).memberInfo().userId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("약속장(미팅 리더)이면서 모임장이 아닌 멤버는 MEETING_LEADER 역할을 가진다")
    void findPreOpinions_meetingLeaderRole() {
        // given
        stubValidators();

        User user1 = createUser(1L, "약속장");

        MeetingMember mm1 = createMeetingMember(1L, user1, MeetingMemberRole.LEADER);

        Topic topic = createTopic(100L, "주제", TopicType.FREE, 1);

        GatheringMember gm1 = createGatheringMember(1L, user1, GatheringRole.MEMBER);

        given(topicRepository.findConfirmedTopics(MEETING_ID)).willReturn(List.of(topic));
        given(meetingMemberRepository.findAllByMeetingId(MEETING_ID)).willReturn(List.of(mm1));
        given(gatheringMemberRepository.findAllMembersByGatheringId(GATHERING_ID)).willReturn(List.of(gm1));
        given(bookReviewRepository.findByUserIdIn(anyList(), anyLong())).willReturn(List.of());
        given(bookReviewKeywordRepository.findByBookReviewIds(anyList())).willReturn(List.of());
        given(topicAnswerRepository.findByMeetingId(MEETING_ID)).willReturn(List.of());
        given(storageService.getPresignedProfileImage(anyString())).willReturn(PRESIGNED_URL);

        // when
        PreOpinionResponse response = preOpinionService.findPreOpinions(GATHERING_ID, MEETING_ID);

        // then
        assertThat(response.members()).hasSize(1);
        assertThat(response.members().get(0).memberInfo().role()).isEqualTo("MEETING_LEADER");
    }

    @Test
    @DisplayName("모임장이면서 약속장인 멤버는 GATHERING_LEADER 역할이 우선한다")
    void findPreOpinions_gatheringLeaderTakesPrecedence() {
        // given
        stubValidators();

        User user1 = createUser(1L, "모임장겸약속장");

        MeetingMember mm1 = createMeetingMember(1L, user1, MeetingMemberRole.LEADER);

        Topic topic = createTopic(100L, "주제", TopicType.FREE, 1);

        GatheringMember gm1 = createGatheringMember(1L, user1, GatheringRole.LEADER);

        given(topicRepository.findConfirmedTopics(MEETING_ID)).willReturn(List.of(topic));
        given(meetingMemberRepository.findAllByMeetingId(MEETING_ID)).willReturn(List.of(mm1));
        given(gatheringMemberRepository.findAllMembersByGatheringId(GATHERING_ID)).willReturn(List.of(gm1));
        given(bookReviewRepository.findByUserIdIn(anyList(), anyLong())).willReturn(List.of());
        given(bookReviewKeywordRepository.findByBookReviewIds(anyList())).willReturn(List.of());
        given(topicAnswerRepository.findByMeetingId(MEETING_ID)).willReturn(List.of());
        given(storageService.getPresignedProfileImage(anyString())).willReturn(PRESIGNED_URL);

        // when
        PreOpinionResponse response = preOpinionService.findPreOpinions(GATHERING_ID, MEETING_ID);

        // then
        assertThat(response.members()).hasSize(1);
        assertThat(response.members().get(0).memberInfo().role()).isEqualTo("GATHERING_LEADER");
    }

    @Test
    @DisplayName("독서 리뷰가 없는 멤버는 bookReview가 null이다")
    void findPreOpinions_memberWithNoBookReview() {
        // given
        stubValidators();

        User user1 = createUser(1L, "리뷰없는멤버");

        MeetingMember mm1 = createMeetingMember(1L, user1, MeetingMemberRole.MEMBER);

        Topic topic = createTopic(100L, "주제", TopicType.FREE, 1);

        GatheringMember gm1 = createGatheringMember(1L, user1, GatheringRole.MEMBER);

        given(topicRepository.findConfirmedTopics(MEETING_ID)).willReturn(List.of(topic));
        given(meetingMemberRepository.findAllByMeetingId(MEETING_ID)).willReturn(List.of(mm1));
        given(gatheringMemberRepository.findAllMembersByGatheringId(GATHERING_ID)).willReturn(List.of(gm1));
        given(bookReviewRepository.findByUserIdIn(anyList(), anyLong())).willReturn(List.of());
        given(bookReviewKeywordRepository.findByBookReviewIds(anyList())).willReturn(List.of());
        given(topicAnswerRepository.findByMeetingId(MEETING_ID)).willReturn(List.of());
        given(storageService.getPresignedProfileImage(anyString())).willReturn(PRESIGNED_URL);

        // when
        PreOpinionResponse response = preOpinionService.findPreOpinions(GATHERING_ID, MEETING_ID);

        // then
        assertThat(response.members()).hasSize(1);
        assertThat(response.members().get(0).bookReview()).isNull();
    }

    @Test
    @DisplayName("독서 리뷰 키워드가 KeywordInfo로 올바르게 매핑된다")
    void findPreOpinions_keywordInfoMappedCorrectly() {
        // given
        stubValidators();

        User user1 = createUser(1L, "리뷰작성자");

        MeetingMember mm1 = createMeetingMember(1L, user1, MeetingMemberRole.MEMBER);

        Topic topic = createTopic(100L, "주제", TopicType.FREE, 1);

        TopicAnswer answer1 = createTopicAnswer(
                200L, topic, user1, "답변 내용",
                LocalDateTime.of(2025, 1, 1, 10, 0)
        );

        BookReview review1 = createBookReview(300L, user1, new BigDecimal("4.0"));

        Keyword keyword1 = createKeyword(10L, "판타지", KeywordType.BOOK);
        Keyword keyword2 = createKeyword(20L, "감동적인", KeywordType.IMPRESSION);

        BookReviewKeyword brk1 = createBookReviewKeyword(1L, review1, keyword1);
        BookReviewKeyword brk2 = createBookReviewKeyword(2L, review1, keyword2);

        GatheringMember gm1 = createGatheringMember(1L, user1, GatheringRole.MEMBER);

        given(topicRepository.findConfirmedTopics(MEETING_ID)).willReturn(List.of(topic));
        given(meetingMemberRepository.findAllByMeetingId(MEETING_ID)).willReturn(List.of(mm1));
        given(gatheringMemberRepository.findAllMembersByGatheringId(GATHERING_ID)).willReturn(List.of(gm1));
        given(bookReviewRepository.findByUserIdIn(anyList(), anyLong())).willReturn(List.of(review1));
        given(bookReviewKeywordRepository.findByBookReviewIds(List.of(300L))).willReturn(List.of(brk1, brk2));
        given(topicAnswerRepository.findByMeetingId(MEETING_ID)).willReturn(List.of(answer1));
        given(storageService.getPresignedProfileImage(anyString())).willReturn(PRESIGNED_URL);

        // when
        PreOpinionResponse response = preOpinionService.findPreOpinions(GATHERING_ID, MEETING_ID);

        // then
        assertThat(response.members()).hasSize(1);

        PreOpinionResponse.BookReviewInfo bookReviewInfo = response.members().get(0).bookReview();
        assertThat(bookReviewInfo).isNotNull();
        assertThat(bookReviewInfo.rating()).isEqualByComparingTo(new BigDecimal("4.0"));
        assertThat(bookReviewInfo.keywordInfo()).hasSize(2);

        PreOpinionResponse.KeywordInfo ki1 = bookReviewInfo.keywordInfo().stream()
                .filter(ki -> ki.id().equals(10L))
                .findFirst()
                .orElseThrow();
        assertThat(ki1.name()).isEqualTo("판타지");
        assertThat(ki1.type()).isEqualTo(KeywordType.BOOK);

        PreOpinionResponse.KeywordInfo ki2 = bookReviewInfo.keywordInfo().stream()
                .filter(ki -> ki.id().equals(20L))
                .findFirst()
                .orElseThrow();
        assertThat(ki2.name()).isEqualTo("감동적인");
        assertThat(ki2.type()).isEqualTo(KeywordType.IMPRESSION);
    }


    @Test
    @DisplayName("내 사전의견 전체 삭제 시 모든 답변에 softDelete가 호출된다")
    void deleteMyAnswer_callsSoftDeleteOnAll() {
        Topic topic1 = Topic.builder().id(12L).build();
        Topic topic2 = Topic.builder().id(13L).build();
        User user = User.builder().id(1L).build();
        TopicAnswer answer1 = TopicAnswer.builder()
                .id(100L).topic(topic1).user(user).content("답변1").isSubmitted(false).build();
        TopicAnswer answer2 = TopicAnswer.builder()
                .id(101L).topic(topic2).user(user).content("답변2").isSubmitted(false).build();

        doNothing().when(gatheringValidator).validateGathering(1L);
        doNothing().when(meetingValidator).validateMeetingInGathering(1L, 1L);
        doNothing().when(meetingValidator).validateMeetingMember(1L, 1L);
        given(topicValidator.getTopicAnswers(1L, 1L)).willReturn(List.of(answer1, answer2));

        preOpinionService.deleteMyAnswer(1L, 1L);

        assertThat(answer1.isDeleted()).isTrue();
        assertThat(answer2.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("모임 검증 실패 시 답변 삭제가 실패한다")
    void deleteMyAnswer_throwsWhenGatheringValidationFails() {
        doThrow(new com.dokdok.gathering.exception.GatheringException(
                com.dokdok.gathering.exception.GatheringErrorCode.GATHERING_NOT_FOUND))
                .when(gatheringValidator).validateGathering(1L);

        assertThatThrownBy(() -> preOpinionService.deleteMyAnswer(1L, 1L))
                .isInstanceOf(com.dokdok.gathering.exception.GatheringException.class);

        verifyNoInteractions(topicAnswerRepository);
    }

    @Test
    @DisplayName("미팅 검증 실패 시 답변 삭제가 실패한다")
    void deleteMyAnswer_throwsWhenMeetingValidationFails() {
        doNothing().when(gatheringValidator).validateGathering(1L);
        doThrow(new com.dokdok.meeting.exception.MeetingException(
                com.dokdok.meeting.exception.MeetingErrorCode.MEETING_NOT_FOUND))
                .when(meetingValidator).validateMeetingInGathering(1L, 1L);

        assertThatThrownBy(() -> preOpinionService.deleteMyAnswer(1L, 1L))
                .isInstanceOf(com.dokdok.meeting.exception.MeetingException.class);

        verifyNoInteractions(topicAnswerRepository);
    }

    @Test
    @DisplayName("미팅 멤버 검증 실패 시 답변 삭제가 실패한다")
    void deleteMyAnswer_throwsWhenMeetingMemberValidationFails() {
        doNothing().when(gatheringValidator).validateGathering(1L);
        doNothing().when(meetingValidator).validateMeetingInGathering(1L, 1L);
        doThrow(new com.dokdok.meeting.exception.MeetingException(
                com.dokdok.meeting.exception.MeetingErrorCode.MEETING_MEMBER_NOT_FOUND))
                .when(meetingValidator).validateMeetingMember(1L, 1L);

        assertThatThrownBy(() -> preOpinionService.deleteMyAnswer(1L, 1L))
                .isInstanceOf(com.dokdok.meeting.exception.MeetingException.class);

        verifyNoInteractions(topicAnswerRepository);
    }

    @Test
    @DisplayName("답변이 없으면 삭제 시 예외가 발생한다")
    void deleteMyAnswer_throwsWhenAnswerNotFound() {
        doNothing().when(gatheringValidator).validateGathering(1L);
        doNothing().when(meetingValidator).validateMeetingInGathering(1L, 1L);
        doNothing().when(meetingValidator).validateMeetingMember(1L, 1L);
        given(topicValidator.getTopicAnswers(1L, 1L))
                .willThrow(new TopicException(TopicErrorCode.TOPIC_ANSWER_NOT_FOUND));

        assertThatThrownBy(() -> preOpinionService.deleteMyAnswer(1L, 1L))
                .isInstanceOf(TopicException.class);
    }

    @Test
    @DisplayName("이미 삭제된 답변은 다시 삭제할 수 없다")
    void deleteMyAnswer_throwsWhenAlreadyDeleted() {
        Topic topic = Topic.builder().id(12L).build();
        User user = User.builder().id(1L).build();
        TopicAnswer answer = TopicAnswer.builder()
                .id(100L)
                .topic(topic)
                .user(user)
                .content("삭제된 내용")
                .isSubmitted(false)
                .build();
        answer.softDelete();

        doNothing().when(gatheringValidator).validateGathering(1L);
        doNothing().when(meetingValidator).validateMeetingInGathering(1L, 1L);
        doNothing().when(meetingValidator).validateMeetingMember(1L, 1L);
        given(topicValidator.getTopicAnswers(1L, 1L)).willReturn(List.of(answer));

        assertThatThrownBy(() -> preOpinionService.deleteMyAnswer(1L, 1L))
                .isInstanceOf(TopicException.class)
                .hasFieldOrPropertyWithValue("errorCode", TopicErrorCode.TOPIC_ANSWER_ALREADY_DELETED);
    }

    @Test
    @DisplayName("인증 정보가 없으면 삭제 시 예외가 발생한다")
    void deleteMyAnswer_throwsWhenUnauthenticated() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> preOpinionService.deleteMyAnswer(1L, 1L))
                .isInstanceOf(GlobalException.class);
    }
}