package com.dokdok.meeting.repository;

import com.dokdok.book.entity.Book;
import com.dokdok.gathering.entity.Gathering;
import com.dokdok.meeting.entity.Meeting;
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
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * QA: 모임 책장(GET /api/gatherings/{id}/books)에 약속을 통해 등록된 책이 반영되지 않는 문제 회귀 테스트.
 *
 * 책장은 별도 테이블이 아니라 해당 모임의 CONFIRMED/DONE 약속에 달린 책에서 도출되어야 한다.
 * GatheringService.getGatheringBooks 가 사용하는
 * MeetingRepository.findDistinctBooksByGatheringIdAndStatuses 를 실제 DB로 검증한다.
 */
@DataJpaTest
@ActiveProfiles("test")
class MeetingRepositoryBookshelfTest {

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private TestEntityManager em;

    /** GatheringService.getGatheringBooks 와 동일한 상태 집합 */
    private static final List<MeetingStatus> SHELF_STATUSES =
            List.of(MeetingStatus.CONFIRMED, MeetingStatus.DONE);

    private static final Pageable PAGEABLE = PageRequest.of(0, 10);

    @Test
    @DisplayName("완료된(DONE) 약속에 등록된 책이 모임 책장에 반영된다")
    void findDistinctBooks_includesBookFromDoneMeeting() {
        // given
        User leader = persistUser("모임장");
        Gathering gathering = persistGathering(leader);
        Book book = persistBook("이펙티브 자바");
        persistMeeting(gathering, book, MeetingStatus.DONE);

        em.flush();
        em.clear();

        // when
        Page<Book> books = meetingRepository.findDistinctBooksByGatheringIdAndStatuses(
                gathering.getId(), SHELF_STATUSES, PAGEABLE);

        // then
        assertThat(books.getTotalElements()).isEqualTo(1);
        assertThat(books.getContent())
                .extracting(Book::getId)
                .containsExactly(book.getId());
    }

    @Test
    @DisplayName("확정된(CONFIRMED) 약속의 책도 책장에 반영된다")
    void findDistinctBooks_includesBookFromConfirmedMeeting() {
        // given
        User leader = persistUser("모임장");
        Gathering gathering = persistGathering(leader);
        Book book = persistBook("클린 코드");
        persistMeeting(gathering, book, MeetingStatus.CONFIRMED);

        em.flush();
        em.clear();

        // when
        Page<Book> books = meetingRepository.findDistinctBooksByGatheringIdAndStatuses(
                gathering.getId(), SHELF_STATUSES, PAGEABLE);

        // then
        assertThat(books.getContent())
                .extracting(Book::getId)
                .containsExactly(book.getId());
    }

    @Test
    @DisplayName("같은 책으로 여러 약속이 있어도 책장에는 중복 없이 한 번만 노출된다")
    void findDistinctBooks_deduplicatesSameBook() {
        // given
        User leader = persistUser("모임장");
        Gathering gathering = persistGathering(leader);
        Book book = persistBook("리팩터링");
        persistMeeting(gathering, book, MeetingStatus.DONE);
        persistMeeting(gathering, book, MeetingStatus.CONFIRMED);

        em.flush();
        em.clear();

        // when
        Page<Book> books = meetingRepository.findDistinctBooksByGatheringIdAndStatuses(
                gathering.getId(), SHELF_STATUSES, PAGEABLE);

        // then
        assertThat(books.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("아직 진행 전(PENDING) 약속의 책은 책장에 반영되지 않는다")
    void findDistinctBooks_excludesPendingMeetingBook() {
        // given
        User leader = persistUser("모임장");
        Gathering gathering = persistGathering(leader);
        Book book = persistBook("토비의 스프링");
        persistMeeting(gathering, book, MeetingStatus.PENDING);

        em.flush();
        em.clear();

        // when
        Page<Book> books = meetingRepository.findDistinctBooksByGatheringIdAndStatuses(
                gathering.getId(), SHELF_STATUSES, PAGEABLE);

        // then
        assertThat(books.getContent()).isEmpty();
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

    private void persistMeeting(Gathering gathering, Book book, MeetingStatus status) {
        em.persist(Meeting.builder()
                .gathering(gathering)
                .book(book)
                .meetingName("테스트 약속")
                .meetingStatus(status)
                .meetingStartDate(LocalDateTime.of(2026, 1, 1, 19, 0))
                .meetingEndDate(LocalDateTime.of(2026, 1, 1, 21, 0))
                .build());
    }
}
