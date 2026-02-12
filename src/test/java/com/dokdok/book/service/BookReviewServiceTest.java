package com.dokdok.book.service;

import com.dokdok.book.dto.request.BookReviewRequest;
import com.dokdok.book.dto.response.BookReviewResponse;
import com.dokdok.book.entity.Book;
import com.dokdok.book.entity.BookReview;
import com.dokdok.book.entity.BookReviewKeyword;
import com.dokdok.book.entity.KeywordType;
import com.dokdok.book.exception.BookErrorCode;
import com.dokdok.book.exception.BookException;
import com.dokdok.book.repository.BookReviewRepository;
import com.dokdok.global.exception.GlobalErrorCode;
import com.dokdok.global.exception.GlobalException;
import com.dokdok.global.response.CursorResponse;
import com.dokdok.global.util.SecurityUtil;
import com.dokdok.history.dto.BookReviewHistoryCursor;
import com.dokdok.history.dto.BookReviewHistoryResponse;
import com.dokdok.history.dto.BookReviewSnapshot;
import com.dokdok.history.entity.BookReviewHistory;
import com.dokdok.history.entity.HistoryAction;
import com.dokdok.history.repository.BookReviewHistoryRepository;
import com.dokdok.keyword.entity.Keyword;
import com.dokdok.keyword.service.KeywordValidator;
import com.dokdok.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookReviewServiceTest {

    @Mock
    private BookReviewRepository bookReviewRepository;

    @Mock
    private BookReviewHistoryRepository reviewHistoryRepository;

    @Mock
    private BookValidator bookValidator;

    @Mock
    private KeywordValidator keywordValidator;

    @InjectMocks
    private BookReviewService bookReviewService;

    @Test
    @DisplayName("책 리뷰를 정상적으로 생성한다")
    void createReview_success() {
        Book book = Book.builder().id(1L).build();
        Keyword keyword = Keyword.builder().id(2L).keywordName("감동").keywordType(KeywordType.BOOK).build();
        Keyword keywordSecond = Keyword.builder().id(4L).keywordName("몰입").keywordType(KeywordType.IMPRESSION).build();
        User user = User.builder().id(3L).build();
        BookReviewRequest request = new BookReviewRequest(new BigDecimal("4.5"), List.of(2L, 4L));

        BookReview saved = BookReview.builder()
                .id(10L)
                .book(book)
                .user(user)
                .rating(new BigDecimal("4.5"))
                .keywords(List.of(
                        BookReviewKeyword.builder().keyword(keyword).build(),
                        BookReviewKeyword.builder().keyword(keywordSecond).build()
                ))
                .build();

        try (MockedStatic<SecurityUtil> securityUtilMock = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(3L);
            securityUtilMock.when(SecurityUtil::getCurrentUserEntity).thenReturn(user);

            doNothing().when(bookValidator).validateReviewNotExists(1L, 3L);
            when(bookValidator.validateAndGetBook(1L)).thenReturn(book);
            when(keywordValidator.validateAndGetSelectableKeyword(2L)).thenReturn(keyword);
            when(keywordValidator.validateAndGetSelectableKeyword(4L)).thenReturn(keywordSecond);
            when(bookReviewRepository.save(any(BookReview.class))).thenReturn(saved);

            BookReviewResponse response = bookReviewService.createReview(1L, request);

            assertThat(response.reviewId()).isEqualTo(10L);
            assertThat(response.bookId()).isEqualTo(1L);
            assertThat(response.userId()).isEqualTo(3L);
            assertThat(response.rating()).isEqualTo(new BigDecimal("4.5"));
            assertThat(response.keywords()).containsExactly(
                    new BookReviewResponse.KeywordInfo(2L, "감동", KeywordType.BOOK),
                    new BookReviewResponse.KeywordInfo(4L, "몰입", KeywordType.IMPRESSION)
            );

            verify(bookValidator).validateReviewNotExists(1L, 3L);
            verify(bookValidator).validateAndGetBook(1L);
            verify(keywordValidator).validateAndGetSelectableKeyword(2L);
            verify(keywordValidator).validateAndGetSelectableKeyword(4L);
            verify(bookReviewRepository).save(any(BookReview.class));
        }
    }

    @Test
    @DisplayName("책이 없으면 예외가 발생한다")
    void createReview_throwsWhenBookMissing() {
        BookReviewRequest request = new BookReviewRequest(new BigDecimal("4.5"), List.of(2L));

        try (MockedStatic<SecurityUtil> securityUtilMock = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(3L);

            doThrow(new BookException(BookErrorCode.BOOK_NOT_FOUND))
                    .when(bookValidator).validateAndGetBook(1L);

            assertThatThrownBy(() -> bookReviewService.createReview(1L, request))
                    .isInstanceOf(BookException.class)
                    .hasFieldOrPropertyWithValue("errorCode", BookErrorCode.BOOK_NOT_FOUND);

            verify(keywordValidator, never()).validateAndGetSelectableKeyword(any());
            verify(bookReviewRepository, never()).save(any(BookReview.class));
        }
    }

    @Test
    @DisplayName("선택 불가 키워드를 요청하면 예외가 발생한다")
    void createReview_throwsWhenKeywordNotSelectable() {
        BookReviewRequest request = new BookReviewRequest(new BigDecimal("4.5"), List.of(2L));
        Book book = Book.builder().id(1L).build();

        try (MockedStatic<SecurityUtil> securityUtilMock = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(3L);

            when(bookValidator.validateAndGetBook(1L)).thenReturn(book);
            doThrow(new BookException(BookErrorCode.KEYWORD_NOT_SELECTABLE))
                    .when(keywordValidator).validateAndGetSelectableKeyword(2L);

            assertThatThrownBy(() -> bookReviewService.createReview(1L, request))
                    .isInstanceOf(BookException.class)
                    .hasFieldOrPropertyWithValue("errorCode", BookErrorCode.KEYWORD_NOT_SELECTABLE);

            verify(bookReviewRepository, never()).save(any(BookReview.class));
        }
    }

    @Test
    @DisplayName("인증 정보가 없으면 예외가 발생한다")
    void createReview_throwsWhenUnauthenticated() {
        BookReviewRequest request = new BookReviewRequest(new BigDecimal("4.5"), List.of(2L));

        try (MockedStatic<SecurityUtil> securityUtilMock = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId)
                    .thenThrow(new GlobalException(GlobalErrorCode.UNAUTHORIZED));

            assertThatThrownBy(() -> bookReviewService.createReview(1L, request))
                    .isInstanceOf(GlobalException.class)
                    .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.UNAUTHORIZED);

            verify(bookReviewRepository, never()).save(any(BookReview.class));
        }
    }

    @Test
    @DisplayName("별점이 유효하지 않으면 생성 시 예외가 발생한다")
    void createReview_throwsWhenInvalidRating() {
        BookReviewRequest request = new BookReviewRequest(new BigDecimal("4.7"), List.of(2L));

        try (MockedStatic<SecurityUtil> securityUtilMock = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(3L);

            doThrow(new BookException(BookErrorCode.BOOK_REVIEW_INVALID_RATING))
                    .when(bookValidator).validateRating(request.rating());

            assertThatThrownBy(() -> bookReviewService.createReview(1L, request))
                    .isInstanceOf(BookException.class)
                    .hasFieldOrPropertyWithValue("errorCode", BookErrorCode.BOOK_REVIEW_INVALID_RATING);

            verify(bookValidator, never()).validateAndGetBook(any());
            verify(keywordValidator, never()).validateAndGetSelectableKeyword(any());
            verify(bookReviewRepository, never()).save(any(BookReview.class));
        }
    }

    @Test
    @DisplayName("리뷰가 이미 존재하면 생성 시 예외가 발생한다")
    void createReview_throwsWhenAlreadyExists() {
        Book book = Book.builder().id(1L).build();
        Keyword keyword = Keyword.builder().id(2L).build();
        User user = User.builder().id(3L).build();
        BookReviewRequest request = new BookReviewRequest(new BigDecimal("4.5"), List.of(2L));

        try (MockedStatic<SecurityUtil> securityUtilMock = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(3L);
            securityUtilMock.when(SecurityUtil::getCurrentUserEntity).thenReturn(user);

            when(bookValidator.validateAndGetBook(1L)).thenReturn(book);
            when(keywordValidator.validateAndGetSelectableKeyword(2L)).thenReturn(keyword);
            doThrow(new BookException(BookErrorCode.BOOK_REVIEW_ALREADY_EXISTS))
                    .when(bookValidator).validateReviewNotExists(1L, 3L);

            assertThatThrownBy(() -> bookReviewService.createReview(1L, request))
                    .isInstanceOf(BookException.class)
                    .hasFieldOrPropertyWithValue("errorCode", BookErrorCode.BOOK_REVIEW_ALREADY_EXISTS);

            verify(bookReviewRepository, never()).save(any(BookReview.class));
        }
    }

    @Test
    @DisplayName("내 책 리뷰를 정상적으로 조회한다")
    void getMyReview_success() {
        Book book = Book.builder().id(1L).build();
        Keyword keyword = Keyword.builder().id(2L).keywordName("감동").keywordType(KeywordType.BOOK).build();
        Keyword keywordSecond = Keyword.builder().id(4L).keywordName("몰입").keywordType(KeywordType.IMPRESSION).build();
        User user = User.builder().id(3L).build();
        BookReview review = BookReview.builder()
                .id(10L)
                .book(book)
                .user(user)
                .rating(new BigDecimal("4.5"))
                .keywords(List.of(
                        BookReviewKeyword.builder().keyword(keyword).build(),
                        BookReviewKeyword.builder().keyword(keywordSecond).build()
                ))
                .build();

        try (MockedStatic<SecurityUtil> securityUtilMock = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(3L);

            when(bookValidator.validateAndGetActiveReview(1L, 3L))
                    .thenReturn(review);

            BookReviewResponse response = bookReviewService.getMyReview(1L);

            assertThat(response.reviewId()).isEqualTo(10L);
            assertThat(response.bookId()).isEqualTo(1L);
            assertThat(response.userId()).isEqualTo(3L);
            assertThat(response.rating()).isEqualTo(new BigDecimal("4.5"));
            assertThat(response.keywords()).containsExactly(
                    new BookReviewResponse.KeywordInfo(2L, "감동", KeywordType.BOOK),
                    new BookReviewResponse.KeywordInfo(4L, "몰입", KeywordType.IMPRESSION)
            );
        }
    }

    @Test
    @DisplayName("내 책 리뷰가 없으면 예외가 발생한다")
    void getMyReview_throwsWhenReviewMissing() {
        try (MockedStatic<SecurityUtil> securityUtilMock = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(3L);

            when(bookValidator.validateAndGetActiveReview(1L, 3L))
                    .thenThrow(new BookException(BookErrorCode.BOOK_REVIEW_NOT_FOUND));

            assertThatThrownBy(() -> bookReviewService.getMyReview(1L))
                    .isInstanceOf(BookException.class)
                    .hasFieldOrPropertyWithValue("errorCode", BookErrorCode.BOOK_REVIEW_NOT_FOUND);
        }
    }

    @Test
    @DisplayName("내 책 리뷰를 수정한다")
    void updateMyReview_success() {
        Book book = Book.builder().id(1L).build();
        Keyword keyword = Keyword.builder().id(2L).keywordName("감동").keywordType(KeywordType.BOOK).build();
        Keyword newKeyword = Keyword.builder().id(5L).keywordName("희망").keywordType(KeywordType.BOOK).build();
        Keyword newKeywordSecond = Keyword.builder().id(8L).keywordName("위로").keywordType(KeywordType.IMPRESSION).build();
        User user = User.builder().id(3L).build();
        BookReview review = BookReview.builder()
                .id(10L)
                .book(book)
                .user(user)
                .rating(new BigDecimal("4.5"))
                .keywords(new ArrayList<>(List.of(
                        BookReviewKeyword.builder().keyword(keyword).build()
                )))
                .build();
        BookReviewRequest request = new BookReviewRequest(new BigDecimal("3.5"), List.of(5L, 8L));

        try (MockedStatic<SecurityUtil> securityUtilMock = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(3L);

            when(bookValidator.validateAndGetReviewForUpdate(1L, 3L)).thenReturn(review);
            when(keywordValidator.validateAndGetSelectableKeyword(5L)).thenReturn(newKeyword);
            when(keywordValidator.validateAndGetSelectableKeyword(8L)).thenReturn(newKeywordSecond);

            BookReviewResponse response = bookReviewService.updateMyReview(1L, request);

            assertThat(review.getRating()).isEqualTo(new BigDecimal("3.5"));
            assertThat(review.getKeywords()).hasSize(2);
            assertThat(response.keywords()).containsExactly(
                    new BookReviewResponse.KeywordInfo(5L, "희망", KeywordType.BOOK),
                    new BookReviewResponse.KeywordInfo(8L, "위로", KeywordType.IMPRESSION)
            );
            assertThat(response.reviewId()).isEqualTo(10L);
        }
    }

    @Test
    @DisplayName("내 책 리뷰가 없으면 수정 시 예외가 발생한다")
    void updateMyReview_throwsWhenReviewMissing() {
        BookReviewRequest request = new BookReviewRequest(new BigDecimal("3.5"), List.of(5L));

        try (MockedStatic<SecurityUtil> securityUtilMock = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(3L);

            when(bookValidator.validateAndGetReviewForUpdate(1L, 3L))
                    .thenThrow(new BookException(BookErrorCode.BOOK_REVIEW_NOT_FOUND));

            assertThatThrownBy(() -> bookReviewService.updateMyReview(1L, request))
                    .isInstanceOf(BookException.class)
                    .hasFieldOrPropertyWithValue("errorCode", BookErrorCode.BOOK_REVIEW_NOT_FOUND);
        }
    }

    @Test
    @DisplayName("선택 불가 키워드면 수정 시 예외가 발생한다")
    void updateMyReview_throwsWhenKeywordNotSelectable() {
        Book book = Book.builder().id(1L).build();
        Keyword keyword = Keyword.builder().id(2L).build();
        User user = User.builder().id(3L).build();
        BookReview review = BookReview.builder()
                .id(10L)
                .book(book)
                .user(user)
                .rating(new BigDecimal("4.5"))
                .keywords(new ArrayList<>(List.of(
                        BookReviewKeyword.builder().keyword(keyword).build()
                )))
                .build();
        BookReviewRequest request = new BookReviewRequest(new BigDecimal("3.5"), List.of(5L));

        try (MockedStatic<SecurityUtil> securityUtilMock = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(3L);

            when(bookValidator.validateAndGetReviewForUpdate(1L, 3L)).thenReturn(review);
            when(keywordValidator.validateAndGetSelectableKeyword(5L))
                    .thenThrow(new BookException(BookErrorCode.KEYWORD_NOT_SELECTABLE));

            assertThatThrownBy(() -> bookReviewService.updateMyReview(1L, request))
                    .isInstanceOf(BookException.class)
                    .hasFieldOrPropertyWithValue("errorCode", BookErrorCode.KEYWORD_NOT_SELECTABLE);
        }
    }

    @Test
    @DisplayName("삭제된 리뷰면 수정 시 예외가 발생한다")
    void updateMyReview_throwsWhenReviewDeleted() {
        BookReviewRequest request = new BookReviewRequest(new BigDecimal("3.5"), List.of(5L));

        try (MockedStatic<SecurityUtil> securityUtilMock = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(3L);

            when(bookValidator.validateAndGetReviewForUpdate(1L, 3L))
                    .thenThrow(new BookException(BookErrorCode.BOOK_REVIEW_NOT_FOUND));

            assertThatThrownBy(() -> bookReviewService.updateMyReview(1L, request))
                    .isInstanceOf(BookException.class)
                    .hasFieldOrPropertyWithValue("errorCode", BookErrorCode.BOOK_REVIEW_NOT_FOUND);
        }
    }

    @Test
    @DisplayName("별점이 유효하지 않으면 수정 시 예외가 발생한다")
    void updateMyReview_throwsWhenInvalidRating() {
        BookReviewRequest request = new BookReviewRequest(new BigDecimal("0.3"), List.of(2L));

        try (MockedStatic<SecurityUtil> securityUtilMock = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(3L);

            doThrow(new BookException(BookErrorCode.BOOK_REVIEW_INVALID_RATING))
                    .when(bookValidator).validateRating(request.rating());

            assertThatThrownBy(() -> bookReviewService.updateMyReview(1L, request))
                    .isInstanceOf(BookException.class)
                    .hasFieldOrPropertyWithValue("errorCode", BookErrorCode.BOOK_REVIEW_INVALID_RATING);

            verify(bookValidator, never()).validateAndGetReviewForUpdate(any(), any());
            verify(keywordValidator, never()).validateAndGetSelectableKeyword(any());
        }
    }

    @Test
    @DisplayName("내 책 리뷰를 삭제한다")
    void deleteMyReview_success() {
        Book book = Book.builder().id(1L).build();
        Keyword keyword = Keyword.builder().id(2L).build();
        User user = User.builder().id(3L).build();
        BookReview review = BookReview.builder()
                .id(10L)
                .book(book)
                .user(user)
                .rating(new BigDecimal("4.5"))
                .keywords(new ArrayList<>(List.of(
                        BookReviewKeyword.builder().keyword(keyword).build()
                )))
                .build();

        try (MockedStatic<SecurityUtil> securityUtilMock = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(3L);

            when(bookValidator.validateAndGetReviewForUpdate(1L, 3L)).thenReturn(review);

            bookReviewService.deleteMyReview(1L);

            assertThat(review.isDeleted()).isTrue();
        }
    }

    @Test
    @DisplayName("삭제된 리뷰를 다시 삭제하면 예외가 발생한다")
    void deleteMyReview_throwsWhenReviewDeleted() {
        try (MockedStatic<SecurityUtil> securityUtilMock = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(3L);

            when(bookValidator.validateAndGetReviewForUpdate(1L, 3L))
                    .thenThrow(new BookException(BookErrorCode.BOOK_REVIEW_NOT_FOUND));

            assertThatThrownBy(() -> bookReviewService.deleteMyReview(1L))
                    .isInstanceOf(BookException.class)
                    .hasFieldOrPropertyWithValue("errorCode", BookErrorCode.BOOK_REVIEW_NOT_FOUND);
        }
    }

    // === getReviewHistory 테스트 ===

    private BookReviewHistory createHistory(Long id, HistoryAction action, BigDecimal rating,
                                            List<Long> keywordIds, LocalDateTime updatedAt) {
        return BookReviewHistory.builder()
                .id(id)
                .bookReviewId(10L)
                .userId(3L)
                .action(action)
                .snapshot(BookReviewSnapshot.builder()
                        .bookId(1L)
                        .rating(rating)
                        .keywordIds(keywordIds)
                        .createdAt(LocalDateTime.of(2026, 1, 27, 21, 47, 58))
                        .updatedAt(updatedAt)
                        .build())
                .createdAt(updatedAt)
                .build();
    }

    @Test
    @DisplayName("리뷰 변경 이력을 정상적으로 조회한다")
    void getReviewHistory_success() {
        BookReview review = BookReview.builder().id(10L).build();
        Keyword bookKeyword = Keyword.builder().id(42L).keywordName("사랑").keywordType(KeywordType.BOOK).build();
        Keyword impressionKeyword = Keyword.builder().id(9L).keywordName("즐거운").keywordType(KeywordType.IMPRESSION).build();

        BookReviewHistory history1 = createHistory(1L, HistoryAction.INSERT, new BigDecimal("4.5"),
                List.of(42L, 9L), LocalDateTime.of(2026, 1, 27, 21, 47, 58));
        BookReviewHistory history2 = createHistory(2L, HistoryAction.UPDATE, new BigDecimal("4.0"),
                List.of(42L), LocalDateTime.of(2026, 1, 27, 22, 0, 14));

        try (MockedStatic<SecurityUtil> securityUtilMock = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(3L);

            when(bookValidator.validateAndGetActiveReview(1L, 3L)).thenReturn(review);
            when(reviewHistoryRepository.findByBookReviewIdAndUserId(10L, 3L))
                    .thenReturn(List.of(history1, history2));
            when(keywordValidator.getKeywordMapByIds(any()))
                    .thenReturn(Map.of(42L, bookKeyword, 9L, impressionKeyword));

            CursorResponse<BookReviewHistoryResponse, BookReviewHistoryCursor> response =
                    bookReviewService.getReviewHistory(1L, 5, null);

            assertThat(response.items()).hasSize(2);
            assertThat(response.items().get(0).bookReviewHistoryId()).isEqualTo(2L);
            assertThat(response.items().get(1).bookReviewHistoryId()).isEqualTo(1L);
            assertThat(response.hasNext()).isFalse();
            assertThat(response.nextCursor()).isNull();
            assertThat(response.totalCount()).isEqualTo(2);
        }
    }

    @Test
    @DisplayName("리뷰 변경 이력이 updatedAt 기준 최신순으로 정렬된다")
    void getReviewHistory_sortedByUpdatedAtDesc() {
        BookReview review = BookReview.builder().id(10L).build();
        Keyword keyword = Keyword.builder().id(42L).keywordName("사랑").keywordType(KeywordType.BOOK).build();

        BookReviewHistory history1 = createHistory(1L, HistoryAction.INSERT, new BigDecimal("4.5"),
                List.of(42L), LocalDateTime.of(2026, 1, 27, 21, 0, 0));
        BookReviewHistory history2 = createHistory(2L, HistoryAction.UPDATE, new BigDecimal("4.0"),
                List.of(42L), LocalDateTime.of(2026, 1, 27, 23, 0, 0));
        BookReviewHistory history3 = createHistory(3L, HistoryAction.UPDATE, new BigDecimal("3.5"),
                List.of(42L), LocalDateTime.of(2026, 1, 27, 22, 0, 0));

        try (MockedStatic<SecurityUtil> securityUtilMock = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(3L);

            when(bookValidator.validateAndGetActiveReview(1L, 3L)).thenReturn(review);
            when(reviewHistoryRepository.findByBookReviewIdAndUserId(10L, 3L))
                    .thenReturn(List.of(history1, history2, history3));
            when(keywordValidator.getKeywordMapByIds(any()))
                    .thenReturn(Map.of(42L, keyword));

            CursorResponse<BookReviewHistoryResponse, BookReviewHistoryCursor> response =
                    bookReviewService.getReviewHistory(1L, 5, null);

            assertThat(response.items().get(0).bookReviewHistoryId()).isEqualTo(2L);
            assertThat(response.items().get(1).bookReviewHistoryId()).isEqualTo(3L);
            assertThat(response.items().get(2).bookReviewHistoryId()).isEqualTo(1L);
        }
    }

    @Test
    @DisplayName("커서 기반 페이징이 정상 동작한다")
    void getReviewHistory_cursorPaging() {
        BookReview review = BookReview.builder().id(10L).build();
        Keyword keyword = Keyword.builder().id(42L).keywordName("사랑").keywordType(KeywordType.BOOK).build();

        List<BookReviewHistory> histories = new ArrayList<>();
        for (long i = 1; i <= 7; i++) {
            histories.add(createHistory(i, HistoryAction.UPDATE, new BigDecimal("4.0"),
                    List.of(42L), LocalDateTime.of(2026, 1, 27, 20, 0, 0).plusHours(i)));
        }

        try (MockedStatic<SecurityUtil> securityUtilMock = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(3L);

            when(bookValidator.validateAndGetActiveReview(1L, 3L)).thenReturn(review);
            when(reviewHistoryRepository.findByBookReviewIdAndUserId(10L, 3L)).thenReturn(histories);
            when(keywordValidator.getKeywordMapByIds(any())).thenReturn(Map.of(42L, keyword));

            // 첫 페이지 (최신순이므로 id: 7, 6, 5, 4, 3)
            CursorResponse<BookReviewHistoryResponse, BookReviewHistoryCursor> firstPage =
                    bookReviewService.getReviewHistory(1L, 5, null);

            assertThat(firstPage.items()).hasSize(5);
            assertThat(firstPage.items().get(0).bookReviewHistoryId()).isEqualTo(7L);
            assertThat(firstPage.items().get(4).bookReviewHistoryId()).isEqualTo(3L);
            assertThat(firstPage.hasNext()).isTrue();
            assertThat(firstPage.nextCursor().historyId()).isEqualTo(3L);
            assertThat(firstPage.totalCount()).isEqualTo(7);

            // 두 번째 페이지 (커서 3L 이후: 2, 1)
            CursorResponse<BookReviewHistoryResponse, BookReviewHistoryCursor> secondPage =
                    bookReviewService.getReviewHistory(1L, 5, 3L);

            assertThat(secondPage.items()).hasSize(2);
            assertThat(secondPage.items().get(0).bookReviewHistoryId()).isEqualTo(2L);
            assertThat(secondPage.items().get(1).bookReviewHistoryId()).isEqualTo(1L);
            assertThat(secondPage.hasNext()).isFalse();
            assertThat(secondPage.nextCursor()).isNull();
            assertThat(secondPage.totalCount()).isNull();
        }
    }

    @Test
    @DisplayName("이력이 없으면 빈 목록을 반환한다")
    void getReviewHistory_emptyList() {
        BookReview review = BookReview.builder().id(10L).build();

        try (MockedStatic<SecurityUtil> securityUtilMock = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(3L);

            when(bookValidator.validateAndGetActiveReview(1L, 3L)).thenReturn(review);
            when(reviewHistoryRepository.findByBookReviewIdAndUserId(10L, 3L)).thenReturn(List.of());
            when(keywordValidator.getKeywordMapByIds(any())).thenReturn(Map.of());

            CursorResponse<BookReviewHistoryResponse, BookReviewHistoryCursor> response =
                    bookReviewService.getReviewHistory(1L, 5, null);

            assertThat(response.items()).isEmpty();
            assertThat(response.hasNext()).isFalse();
            assertThat(response.nextCursor()).isNull();
        }
    }

    @Test
    @DisplayName("리뷰가 없으면 이력 조회 시 예외가 발생한다")
    void getReviewHistory_throwsWhenReviewNotFound() {
        try (MockedStatic<SecurityUtil> securityUtilMock = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(3L);

            when(bookValidator.validateAndGetActiveReview(1L, 3L))
                    .thenThrow(new BookException(BookErrorCode.BOOK_REVIEW_NOT_FOUND));

            assertThatThrownBy(() -> bookReviewService.getReviewHistory(1L, 5, null))
                    .isInstanceOf(BookException.class)
                    .hasFieldOrPropertyWithValue("errorCode", BookErrorCode.BOOK_REVIEW_NOT_FOUND);

            verify(reviewHistoryRepository, never()).findByBookReviewIdAndUserId(any(), any());
        }
    }

    @Test
    @DisplayName("존재하지 않는 커서 ID로 조회하면 빈 목록을 반환한다")
    void getReviewHistory_invalidCursorReturnsEmpty() {
        BookReview review = BookReview.builder().id(10L).build();
        Keyword keyword = Keyword.builder().id(42L).keywordName("사랑").keywordType(KeywordType.BOOK).build();

        BookReviewHistory history1 = createHistory(1L, HistoryAction.INSERT, new BigDecimal("4.5"),
                List.of(42L), LocalDateTime.of(2026, 1, 27, 21, 0, 0));

        try (MockedStatic<SecurityUtil> securityUtilMock = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(3L);

            when(bookValidator.validateAndGetActiveReview(1L, 3L)).thenReturn(review);
            when(reviewHistoryRepository.findByBookReviewIdAndUserId(10L, 3L)).thenReturn(List.of(history1));
            when(keywordValidator.getKeywordMapByIds(any())).thenReturn(Map.of());

            CursorResponse<BookReviewHistoryResponse, BookReviewHistoryCursor> response =
                    bookReviewService.getReviewHistory(1L, 5, 999L);

            assertThat(response.items()).isEmpty();
            assertThat(response.hasNext()).isFalse();
        }
    }

    @Test
    @DisplayName("응답에 책 키워드와 감상 키워드가 분리되어 반환된다")
    void getReviewHistory_keywordsGroupedByType() {
        BookReview review = BookReview.builder().id(10L).build();
        Keyword bookKeyword1 = Keyword.builder().id(42L).keywordName("사랑").keywordType(KeywordType.BOOK).build();
        Keyword bookKeyword2 = Keyword.builder().id(43L).keywordName("관계").keywordType(KeywordType.BOOK).build();
        Keyword impressionKeyword = Keyword.builder().id(9L).keywordName("즐거운").keywordType(KeywordType.IMPRESSION).build();

        BookReviewHistory history = createHistory(1L, HistoryAction.INSERT, new BigDecimal("4.0"),
                List.of(42L, 43L, 9L), LocalDateTime.of(2026, 1, 27, 21, 0, 0));

        try (MockedStatic<SecurityUtil> securityUtilMock = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(3L);

            when(bookValidator.validateAndGetActiveReview(1L, 3L)).thenReturn(review);
            when(reviewHistoryRepository.findByBookReviewIdAndUserId(10L, 3L)).thenReturn(List.of(history));
            when(keywordValidator.getKeywordMapByIds(List.of(42L, 43L, 9L)))
                    .thenReturn(Map.of(42L, bookKeyword1, 43L, bookKeyword2, 9L, impressionKeyword));

            CursorResponse<BookReviewHistoryResponse, BookReviewHistoryCursor> response =
                    bookReviewService.getReviewHistory(1L, 5, null);

            BookReviewHistoryResponse item = response.items().get(0);
            assertThat(item.bookKeywords()).hasSize(2);
            assertThat(item.impressionKeywords()).hasSize(1);
            assertThat(item.impressionKeywords().get(0).name()).isEqualTo("즐거운");
        }
    }

    @Test
    @DisplayName("응답 날짜가 LocalDateTime 형식으로 응답된다")
    void getReviewHistory_dateFormatted() {
        BookReview review = BookReview.builder().id(10L).build();
        Keyword keyword = Keyword.builder().id(42L).keywordName("사랑").keywordType(KeywordType.BOOK).build();

        LocalDateTime expectedDateTime = LocalDateTime.of(2025, 12, 8, 15, 30, 0);
        BookReviewHistory history = createHistory(1L, HistoryAction.INSERT, new BigDecimal("4.5"),
                List.of(42L), expectedDateTime);

        try (MockedStatic<SecurityUtil> securityUtilMock = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(3L);

            when(bookValidator.validateAndGetActiveReview(1L, 3L)).thenReturn(review);
            when(reviewHistoryRepository.findByBookReviewIdAndUserId(10L, 3L)).thenReturn(List.of(history));
            when(keywordValidator.getKeywordMapByIds(any())).thenReturn(Map.of(42L, keyword));

            CursorResponse<BookReviewHistoryResponse, BookReviewHistoryCursor> response =
                    bookReviewService.getReviewHistory(1L, 5, null);

            assertThat(response.items().get(0).createdAt()).isEqualTo(expectedDateTime);
        }
    }
}
