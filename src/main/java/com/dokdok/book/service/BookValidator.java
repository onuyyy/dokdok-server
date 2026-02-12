package com.dokdok.book.service;

import com.dokdok.book.entity.Book;
import com.dokdok.book.entity.BookReview;
import com.dokdok.book.exception.BookErrorCode;
import com.dokdok.book.exception.BookException;
import com.dokdok.book.repository.BookRepository;
import com.dokdok.book.repository.BookReviewRepository;
import com.dokdok.book.entity.PersonalBook;
import com.dokdok.book.repository.PersonalBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BookValidator {

    private final BookRepository bookRepository;
    private final BookReviewRepository bookReviewRepository;
    private final PersonalBookRepository personalBookRepository;

    // 책 존재 여부를 검증하고 엔티티를 반환합니다.
    public Book validateAndGetBook(Long bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new BookException(BookErrorCode.BOOK_NOT_FOUND));
    }

    public void validateBook(Long bookId) {
        boolean exists = bookRepository.existsById(bookId);

        if (!exists) {
            throw new BookException(BookErrorCode.BOOK_NOT_FOUND);
        }
    }

    // 동일 사용자/책 리뷰 중복 생성을 막습니다.
    public void validateReviewNotExists(Long bookId, Long userId) {
        if (bookReviewRepository.existsByBookIdAndUserId(bookId, userId)) {
            throw new BookException(BookErrorCode.BOOK_REVIEW_ALREADY_EXISTS);
        }
    }

    public PersonalBook validateInBookShelf(Long userId, Long bookId) {
        return personalBookRepository.findTopByUserIdAndBookIdAndGatheringIsNullOrderByAddedAtDesc(userId, bookId)
                .orElseThrow(() -> new BookException(BookErrorCode.BOOK_NOT_IN_SHELF));
    }

    // 책장에 해당 책이 존재 여부 확인하는 메서드입니다.
    public PersonalBook validatePersonalBook(Long userId, Long personalBookId) {
        return personalBookRepository.findByIdAndUserId(personalBookId, userId)
                .orElseThrow(() -> new BookException(BookErrorCode.BOOK_NOT_IN_SHELF));
    }

    public void validateDuplicatePersonalBook(Long userId, Long bookId) {
        personalBookRepository.findTopByUserIdAndBookIdAndGatheringIsNullOrderByAddedAtDesc(userId, bookId)
                .ifPresent(personalBook ->
                {
                    throw new BookException(BookErrorCode.BOOK_ALREADY_EXISTS);
                });
    }

    public boolean isDuplicatePersonalBook(Long userId, Long bookId) {
        return personalBookRepository.findTopByUserIdAndBookIdAndGatheringIsNullOrderByAddedAtDesc(userId, bookId).isPresent();
    }

    // 삭제되지 않은 책 리뷰 존재 여부를 검증하고 반환합니다.
    public BookReview validateAndGetActiveReview(Long bookId, Long userId) {
        return bookReviewRepository.findByBookIdAndUserId(bookId, userId)
                .orElseThrow(() -> new BookException(BookErrorCode.BOOK_REVIEW_NOT_FOUND));
    }

    // 수정/삭제를 위한 활성(삭제되지 않은) 책 리뷰를 조회합니다.
    public BookReview validateAndGetReviewForUpdate(Long bookId, Long userId) {
        return bookReviewRepository.findByBookIdAndUserId(bookId, userId)
                .orElseThrow(() -> new BookException(BookErrorCode.BOOK_REVIEW_NOT_FOUND));
    }

    // 별점이 0.5 단위의 5점 만점인지 검증합니다.
    public void validateRating(BigDecimal rating) {
        if (rating == null) {
            return;
        }
        BigDecimal min = new BigDecimal("0.0");
        BigDecimal max = new BigDecimal("5.0");
        if (rating.compareTo(min) < 0 || rating.compareTo(max) > 0) {
            throw new BookException(BookErrorCode.BOOK_REVIEW_INVALID_RATING);
        }
        BigDecimal scaled = rating.multiply(BigDecimal.TEN);
        if (scaled.remainder(new BigDecimal("5")).compareTo(BigDecimal.ZERO) != 0) {
            throw new BookException(BookErrorCode.BOOK_REVIEW_INVALID_RATING);
        }
    }

}