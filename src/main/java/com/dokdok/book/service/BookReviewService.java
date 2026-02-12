package com.dokdok.book.service;

import com.dokdok.book.dto.request.BookReviewRequest;
import com.dokdok.book.dto.response.BookReviewResponse;
import com.dokdok.book.entity.Book;
import com.dokdok.book.entity.BookReview;
import com.dokdok.book.repository.BookReviewRepository;
import com.dokdok.global.response.CursorResponse;
import com.dokdok.global.util.SecurityUtil;
import com.dokdok.history.dto.BookReviewHistoryCursor;
import com.dokdok.history.dto.BookReviewHistoryResponse;
import com.dokdok.history.entity.BookReviewHistory;
import com.dokdok.history.repository.BookReviewHistoryRepository;
import com.dokdok.keyword.entity.Keyword;
import com.dokdok.keyword.service.KeywordValidator;
import com.dokdok.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookReviewService {

    private final BookReviewRepository bookReviewRepository;
    private final BookReviewHistoryRepository reviewHistoryRepository;
    private final BookValidator bookValidator;
    private final KeywordValidator keywordValidator;

    @Transactional
    public BookReviewResponse createReview(Long bookId, BookReviewRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        bookValidator.validateRating(request.rating());

        Book book = bookValidator.validateAndGetBook(bookId);

        List<Keyword> keywords = request.keywordIds().stream()
                .map(keywordValidator::validateAndGetSelectableKeyword)
                .collect(Collectors.toList());

        bookValidator.validateReviewNotExists(bookId, userId);

        User user = SecurityUtil.getCurrentUserEntity();

        BookReview review = BookReview.create(book, user, request.rating(), keywords);

        return BookReviewResponse.from(bookReviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    public BookReviewResponse getMyReview(Long bookId) {
        Long userId = SecurityUtil.getCurrentUserId();

        BookReview review = bookValidator.validateAndGetActiveReview(bookId, userId);

        return BookReviewResponse.from(review);
    }

    @Transactional
    public BookReviewResponse updateMyReview(Long bookId, BookReviewRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        bookValidator.validateRating(request.rating());

        BookReview review = bookValidator.validateAndGetReviewForUpdate(bookId, userId);

        List<Keyword> keywords = request.keywordIds().stream()
                .map(keywordValidator::validateAndGetSelectableKeyword)
                .collect(Collectors.toList());

        review.updateReview(request.rating(), keywords);

        return BookReviewResponse.from(review);
    }

    @Transactional
    public void deleteMyReview(Long bookId) {
        Long userId = SecurityUtil.getCurrentUserId();

        BookReview review = bookValidator.validateAndGetReviewForUpdate(bookId, userId);
        review.deleteReview();
    }

    @Transactional(readOnly = true)
    public CursorResponse<BookReviewHistoryResponse, BookReviewHistoryCursor> getReviewHistory(
            Long bookId, int pageSize, Long cursorHistoryId) {
        Long userId = SecurityUtil.getCurrentUserId();

        BookReview review = bookValidator.validateAndGetActiveReview(bookId, userId);

        List<BookReviewHistory> histories = reviewHistoryRepository.findByBookReviewIdAndUserId(review.getId(), userId);

        List<BookReviewHistory> sorted = sortByUpdatedAtDesc(histories);
        List<BookReviewHistory> afterCursor = applyHistoryCursor(sorted, cursorHistoryId);

        boolean hasNext = afterCursor.size() > pageSize;
        List<BookReviewHistory> paged = hasNext ? afterCursor.subList(0, pageSize) : afterCursor;

        List<BookReviewHistoryResponse> items = toHistoryResponses(paged);

        BookReviewHistoryCursor nextCursor = hasNext
                ? BookReviewHistoryCursor.from(paged.get(paged.size() - 1))
                : null;

        Integer totalCount = (cursorHistoryId == null) ? sorted.size() : null;

        return CursorResponse.of(items, pageSize, hasNext, nextCursor, totalCount);
    }

    private List<BookReviewHistory> sortByUpdatedAtDesc(List<BookReviewHistory> histories) {
        return histories.stream()
                .sorted((a, b) -> b.getSnapshot().getUpdatedAt().compareTo(a.getSnapshot().getUpdatedAt()))
                .collect(Collectors.toList());
    }

    private List<BookReviewHistory> applyHistoryCursor(List<BookReviewHistory> sorted, Long cursorHistoryId) {
        if (cursorHistoryId == null) {
            return sorted;
        }
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getId().equals(cursorHistoryId)) {
                return sorted.subList(i + 1, sorted.size());
            }
        }
        return List.of();
    }

    private List<BookReviewHistoryResponse> toHistoryResponses(List<BookReviewHistory> histories) {
        List<Long> allKeywordIds = histories.stream()
                .flatMap(h -> h.getSnapshot().getKeywordIds().stream())
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Keyword> keywordMap = keywordValidator.getKeywordMapByIds(allKeywordIds);

        return histories.stream()
                .map(history -> BookReviewHistoryResponse.from(history, keywordMap))
                .collect(Collectors.toList());
    }

}
