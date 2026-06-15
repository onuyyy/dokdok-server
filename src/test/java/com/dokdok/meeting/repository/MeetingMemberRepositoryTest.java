package com.dokdok.meeting.repository;

import com.dokdok.book.entity.Book;
import com.dokdok.gathering.entity.Gathering;
import com.dokdok.meeting.entity.Meeting;
import com.dokdok.meeting.entity.MeetingMember;
import com.dokdok.meeting.entity.MeetingStatus;
import com.dokdok.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * QA: GET /api/gatherings/{id}/meetings?filter=JOINED 500 에러 회귀 테스트.
 *
 * 컨트롤러 기본 정렬(@PageableDefault(sort = {"meetingStartDate", "id"}))이
 * 주입된 Pageable 로 JOINED 쿼리를 실행할 때 Hibernate 가 정렬 속성을 해석하지 못해
 * 500 이 발생하던 케이스를 재현/검증한다.
 */
@DataJpaTest
@ActiveProfiles("test")
class MeetingMemberRepositoryTest {

    @Autowired
    private MeetingMemberRepository meetingMemberRepository;

    @Autowired
    private TestEntityManager em;

    /** 컨트롤러 MeetingListController 의 @PageableDefault 와 동일한 기본 정렬 */
    private static final Pageable DEFAULT_PAGEABLE =
            PageRequest.of(0, 5, Sort.by("meetingStartDate", "id"));

    @Test
    @DisplayName("기본 정렬이 적용된 Pageable 로 참여한(JOINED) 완료 약속을 조회한다")
    void findMeetingsByUserIdAndStatus_withDefaultSort_returnsJoinedMeeting() {
        // given
        User user = persistUser("참여자");
        Gathering gathering = persistGathering(user);
        Book book = persistBook("이펙티브 자바");
        Meeting meeting = persistMeeting(gathering, book, MeetingStatus.DONE,
                LocalDateTime.of(2026, 1, 1, 19, 0));
        persistMeetingMember(meeting, user, null); // canceledAt = null → 참여 중

        em.flush();
        em.clear();

        // when : 수정 전에는 정렬 속성 해석 실패로 예외(500)가 발생한다
        Page<Meeting> page = meetingMemberRepository.findMeetingsByUserIdAndStatus(
                user.getId(), gathering.getId(), MeetingStatus.DONE, DEFAULT_PAGEABLE);

        // then
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().getId()).isEqualTo(meeting.getId());
    }

    @Test
    @DisplayName("참여를 취소한(canceledAt != null) 약속은 조회되지 않는다")
    void findMeetingsByUserIdAndStatus_excludesCanceledMembership() {
        // given
        User user = persistUser("취소자");
        Gathering gathering = persistGathering(user);
        Book book = persistBook("클린 코드");
        Meeting meeting = persistMeeting(gathering, book, MeetingStatus.DONE,
                LocalDateTime.of(2026, 2, 1, 19, 0));
        persistMeetingMember(meeting, user, LocalDateTime.of(2026, 2, 2, 10, 0)); // 취소됨

        em.flush();
        em.clear();

        // when
        Page<Meeting> page = meetingMemberRepository.findMeetingsByUserIdAndStatus(
                user.getId(), gathering.getId(), MeetingStatus.DONE, DEFAULT_PAGEABLE);

        // then
        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getContent()).isEmpty();
    }

    private User persistUser(String name) {
        return em.persist(User.builder()
                .userName(name)
                .nickname(name)
                .userEmail(name + "@test.com")
                .kakaoId(System.nanoTime())
                .build());
    }

    private Gathering persistGathering(User leader) {
        return em.persist(Gathering.builder()
                .gatheringLeader(leader)
                .gatheringName("테스트 모임")
                .invitationLink("invite-" + System.nanoTime())
                .build());
    }

    private Book persistBook(String name) {
        return em.persist(Book.builder()
                .bookName(name)
                .author("저자")
                .publisher("출판사")
                .isbn("978-" + System.nanoTime())
                .build());
    }

    private Meeting persistMeeting(Gathering gathering, Book book, MeetingStatus status,
                                   LocalDateTime startDate) {
        return em.persist(Meeting.builder()
                .gathering(gathering)
                .book(book)
                .meetingName("테스트 약속")
                .meetingStatus(status)
                .meetingStartDate(startDate)
                .meetingEndDate(startDate.plusHours(2))
                .build());
    }

    private void persistMeetingMember(Meeting meeting, User user, LocalDateTime canceledAt) {
        em.persist(MeetingMember.builder()
                .meeting(meeting)
                .user(user)
                .canceledAt(canceledAt)
                .build());
    }
}