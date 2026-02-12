package com.dokdok.history.listener;

import com.dokdok.book.entity.Book;
import com.dokdok.book.entity.BookReview;
import com.dokdok.book.entity.KeywordType;
import com.dokdok.book.repository.BookRepository;
import com.dokdok.book.repository.BookReviewRepository;
import com.dokdok.history.entity.BookReviewHistory;
import com.dokdok.history.entity.HistoryAction;
import com.dokdok.history.repository.BookReviewHistoryRepository;
import com.dokdok.keyword.entity.Keyword;
import com.dokdok.keyword.repository.KeywordRepository;
import com.dokdok.user.entity.User;
import com.dokdok.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BookReviewHistoryListenerTest {

    @Autowired
    private BookReviewRepository bookReviewRepository;

    @Autowired
    private BookReviewHistoryRepository bookReviewHistoryRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KeywordRepository keywordRepository;

    @Autowired
    private EntityManager entityManager;

    private Book book;
    private User user;
    private Keyword keyword1;
    private Keyword keyword2;
    private Keyword keyword3;

    @BeforeEach
    void setUp() {
        bookReviewHistoryRepository.deleteAll();
        bookReviewRepository.deleteAllInBatch();

        book = bookRepository.save(Book.builder()
                .isbn("1234567890123")
                .bookName("테스트 책")
                .author("테스트 저자")
                .build());

        user = userRepository.save(User.builder()
                .userEmail("test@test.com")
                .nickname("테스트유저")
                .kakaoId(123456789L)
                .build());

        keyword1 = keywordRepository.save(Keyword.builder()
                .keywordName("감동")
                .keywordType(KeywordType.BOOK)
                .level(1)
                .isSelectable(true)
                .build());

        keyword2 = keywordRepository.save(Keyword.builder()
                .keywordName("몰입")
                .keywordType(KeywordType.IMPRESSION)
                .level(1)
                .isSelectable(true)
                .build());

        keyword3 = keywordRepository.save(Keyword.builder()
                .keywordName("희망")
                .keywordType(KeywordType.BOOK)
                .level(1)
                .isSelectable(true)
                .build());
    }

    @Test
    @DisplayName("책 리뷰 생성 시 INSERT 히스토리가 저장된다")
    void createReview_savesInsertHistory() {
        // given
        BookReview review = BookReview.create(book, user, new BigDecimal("4.5"), List.of(keyword1, keyword2));

        // when
        bookReviewRepository.save(review);
        entityManager.flush();
        entityManager.clear();

        // then
        List<BookReviewHistory> histories = bookReviewHistoryRepository.findAll();
        assertThat(histories).hasSize(1);

        BookReviewHistory history = histories.get(0);
        assertThat(history.getAction()).isEqualTo(HistoryAction.INSERT);
        assertThat(history.getBookReviewId()).isEqualTo(review.getId());
        assertThat(history.getUserId()).isEqualTo(user.getId());
        assertThat(history.getSnapshot()).isNotNull();
        assertThat(history.getSnapshot().getBookId()).isEqualTo(book.getId());
        assertThat(history.getSnapshot().getRating()).isEqualTo(new BigDecimal("4.5"));
        assertThat(history.getSnapshot().getKeywordIds()).containsExactlyInAnyOrder(keyword1.getId(), keyword2.getId());
    }

    @Test
    @DisplayName("책 리뷰 rating 수정 시 UPDATE 히스토리가 저장된다")
    void updateReviewRating_savesUpdateHistory() {
        // given
        BookReview review = BookReview.create(book, user, new BigDecimal("4.5"), List.of(keyword1));
        bookReviewRepository.save(review);
        entityManager.flush();
        entityManager.clear();

        // when
        BookReview savedReview = bookReviewRepository.findById(review.getId()).orElseThrow();
        savedReview.updateReview(new BigDecimal("3.0"), List.of(keyword1));
        entityManager.flush();
        entityManager.clear();

        // then
        List<BookReviewHistory> histories = bookReviewHistoryRepository.findAll();
        assertThat(histories).hasSize(2);

        BookReviewHistory updateHistory = histories.stream()
                .filter(h -> h.getAction() == HistoryAction.UPDATE)
                .findFirst()
                .orElseThrow();

        assertThat(updateHistory.getSnapshot().getRating()).isEqualTo(new BigDecimal("3.0"));
    }

    @Test
    @DisplayName("책 리뷰 키워드 수정 시 UPDATE 히스토리가 저장된다")
    void updateReviewKeywords_savesUpdateHistory() {
        // given
        BookReview review = BookReview.create(book, user, new BigDecimal("4.5"), List.of(keyword1));
        bookReviewRepository.save(review);
        entityManager.flush();
        entityManager.clear();

        // when
        BookReview savedReview = bookReviewRepository.findById(review.getId()).orElseThrow();
        savedReview.updateReview(new BigDecimal("4.5"), List.of(keyword2, keyword3));
        entityManager.flush();
        entityManager.clear();

        // then
        List<BookReviewHistory> histories = bookReviewHistoryRepository.findAll();
        assertThat(histories).hasSize(2);

        BookReviewHistory updateHistory = histories.stream()
                .filter(h -> h.getAction() == HistoryAction.UPDATE)
                .findFirst()
                .orElseThrow();

        assertThat(updateHistory.getSnapshot().getKeywordIds()).containsExactlyInAnyOrder(keyword2.getId(), keyword3.getId());
    }

    @Test
    @DisplayName("책 리뷰 rating과 키워드 동시 수정 시 UPDATE 히스토리가 저장된다")
    void updateReviewRatingAndKeywords_savesUpdateHistory() {
        // given
        BookReview review = BookReview.create(book, user, new BigDecimal("4.5"), List.of(keyword1));
        bookReviewRepository.save(review);
        entityManager.flush();
        entityManager.clear();

        // when
        BookReview savedReview = bookReviewRepository.findById(review.getId()).orElseThrow();
        savedReview.updateReview(new BigDecimal("2.0"), List.of(keyword2));
        entityManager.flush();
        entityManager.clear();

        // then
        List<BookReviewHistory> histories = bookReviewHistoryRepository.findAll();
        assertThat(histories).hasSize(2);

        BookReviewHistory updateHistory = histories.stream()
                .filter(h -> h.getAction() == HistoryAction.UPDATE)
                .findFirst()
                .orElseThrow();

        assertThat(updateHistory.getSnapshot().getRating()).isEqualTo(new BigDecimal("2.0"));
        assertThat(updateHistory.getSnapshot().getKeywordIds()).containsExactly(keyword2.getId());
    }

    @Test
    @DisplayName("책 리뷰 변경이 없으면 UPDATE 히스토리가 저장되지 않는다")
    void updateReviewNoChange_doesNotSaveHistory() {
        // given
        BookReview review = BookReview.create(book, user, new BigDecimal("4.5"), List.of(keyword1));
        bookReviewRepository.save(review);
        entityManager.flush();
        entityManager.clear();

        // when
        BookReview savedReview = bookReviewRepository.findById(review.getId()).orElseThrow();
        savedReview.updateReview(new BigDecimal("4.5"), List.of(keyword1));
        entityManager.flush();
        entityManager.clear();

        // then
        List<BookReviewHistory> histories = bookReviewHistoryRepository.findAll();
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getAction()).isEqualTo(HistoryAction.INSERT);
    }

    @Test
    @DisplayName("책 리뷰 soft delete 시 DELETE 히스토리가 저장된다")
    void deleteReview_savesDeleteHistory() {
        // given
        BookReview review = BookReview.create(book, user, new BigDecimal("4.5"), List.of(keyword1));
        bookReviewRepository.save(review);
        entityManager.flush();
        entityManager.clear();

        // when
        BookReview savedReview = bookReviewRepository.findById(review.getId()).orElseThrow();
        savedReview.deleteReview();
        entityManager.flush();
        entityManager.clear();

        // then
        List<BookReviewHistory> histories = bookReviewHistoryRepository.findAll();
        assertThat(histories).hasSize(2);

        BookReviewHistory deleteHistory = histories.stream()
                .filter(h -> h.getAction() == HistoryAction.DELETE)
                .findFirst()
                .orElseThrow();

        assertThat(deleteHistory.getBookReviewId()).isEqualTo(review.getId());
        assertThat(deleteHistory.getUserId()).isEqualTo(user.getId());
    }
}