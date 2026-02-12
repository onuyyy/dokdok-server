package com.dokdok.book.service;

import com.dokdok.book.dto.request.BookCreateRequest;
import com.dokdok.book.dto.response.PersonalBookCreateResponse;
import com.dokdok.book.dto.response.PersonalBookDetailResponse;
import com.dokdok.book.dto.response.PersonalBookListResponse;
import com.dokdok.book.dto.response.BookListCursor;
import com.dokdok.book.dto.response.CursorPageResponse;
import com.dokdok.book.entity.Book;
import com.dokdok.book.entity.BookReadingStatus;
import com.dokdok.book.entity.PersonalBook;
import com.dokdok.book.exception.BookErrorCode;
import com.dokdok.book.exception.BookException;
import com.dokdok.book.repository.BookRepository;
import com.dokdok.book.repository.PersonalBookListProjection;
import com.dokdok.book.repository.PersonalBookRepository;
import com.dokdok.gathering.entity.Gathering;
import com.dokdok.global.util.SecurityUtil;
import com.dokdok.user.entity.User;
import com.dokdok.user.service.UserValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PersonalBookService {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final PersonalBookRepository personalBookRepository;
    private final BookRepository bookRepository;
    private final UserValidator userValidator;
    private final BookValidator bookValidator;

    // 생성
    @Transactional
    public PersonalBookCreateResponse createBook(BookCreateRequest bookCreateRequest) {
        return createBook(bookCreateRequest, null);
    }

    @Transactional
    public PersonalBookDetailResponse updateReadingStatus(Long personalBookId) {
        // 사용자 유효성 검증
        User userEntity = userValidator.findUserOrThrow(SecurityUtil.getCurrentUserId());
        PersonalBook entity = bookValidator.validatePersonalBook(userEntity.getId(), personalBookId);

        entity.updateReadingStatus();

        return PersonalBookDetailResponse.from(entity);
    }

    @Transactional
    public PersonalBookCreateResponse createBook(BookCreateRequest bookCreateRequest, Gathering gathering) {
        // 사용자 유효성 검증
        User userEntity = userValidator.findUserOrThrow(SecurityUtil.getCurrentUserId());
        // 책 유효성 검증 && 없으면 book entity에 저장
        Book entity = bookRepository.findByIsbn(bookCreateRequest.isbn())
                .orElseGet(() -> bookRepository.save(bookCreateRequest.of()));

        bookValidator.validateDuplicatePersonalBook(userEntity.getId(), entity.getId());
        PersonalBook personalBookEntity = PersonalBook.create(
                userEntity,
                entity,
                BookReadingStatus.READING,
                gathering
        );

        personalBookRepository.save(personalBookEntity);

        return PersonalBookCreateResponse.from(personalBookEntity);
    }

    // List
    public Page<PersonalBookListResponse> getPersonalBookList(BookReadingStatus bookReadingStatus, Long gatheringId, Pageable pageable) {
        User userEntity = userValidator.findUserOrThrow(SecurityUtil.getCurrentUserId());
        String readingStatus = bookReadingStatus != null ? bookReadingStatus.name() : null;

        Page<PersonalBookListProjection> page = personalBookRepository.findPersonalBooksByUserIdReadingStatusAndGatheringId(
                userEntity.getId(),
                gatheringId,
                readingStatus,
                pageable
        );

        if (page.isEmpty()) {
            throw new BookException(BookErrorCode.BOOK_NOT_IN_SHELF);
        }

        return page.map(PersonalBookListResponse::from);
    }

    public CursorPageResponse<PersonalBookListResponse, BookListCursor> getPersonalBookListCursor(
            BookReadingStatus bookReadingStatus,
            Long gatheringId,
            OffsetDateTime cursorAddedAt,
            Long cursorBookId,
            Integer size
    ) {
        User userEntity = userValidator.findUserOrThrow(SecurityUtil.getCurrentUserId());
        String readingStatus = bookReadingStatus != null ? bookReadingStatus.name() : null;
        int pageSize = resolvePageSize(size);
        LocalDateTime cursorAddedAtValue = cursorAddedAt != null ? cursorAddedAt.toLocalDateTime() : null;

        List<PersonalBookListProjection> results = personalBookRepository
                .findPersonalBooksByUserIdReadingStatusAndGatheringIdCursor(
                        userEntity.getId(),
                        gatheringId,
                        readingStatus,
                        cursorAddedAtValue,
                        cursorBookId,
                        PageRequest.of(0, pageSize + 1)
                );
        long totalCount = personalBookRepository.countPersonalBooksByUserIdReadingStatusAndGatheringId(
                userEntity.getId(),
                gatheringId,
                readingStatus
        );

        boolean hasNext = results.size() > pageSize;
        List<PersonalBookListProjection> pageResults = hasNext ? results.subList(0, pageSize) : results;
        List<PersonalBookListResponse> items = pageResults.stream()
                .map(PersonalBookListResponse::from)
                .toList();

        BookListCursor nextCursor = null;
        if (hasNext && !pageResults.isEmpty()) {
            PersonalBookListProjection last = pageResults.get(pageResults.size() - 1);
            nextCursor = BookListCursor.from(last.getAddedAt(), last.getBookId());
        }

        return CursorPageResponse.of(items, pageSize, hasNext, nextCursor, totalCount);
    }

    public PersonalBookDetailResponse getPersonalBook(Long bookId) {
        User userEntity = userValidator.findUserOrThrow(SecurityUtil.getCurrentUserId());
        // 책 정보 GET Logic
        PersonalBook entity = bookValidator.validateInBookShelf(userEntity.getId(), bookId);

        return PersonalBookDetailResponse.from(entity);
    }

    @Transactional
    public void deleteBook(Long bookId) {
        User userEntity = userValidator.findUserOrThrow(SecurityUtil.getCurrentUserId());

        PersonalBook personalBook = bookValidator.validateInBookShelf(userEntity.getId(), bookId);

        personalBookRepository.delete(personalBook);
    }

    /**
     * 약속 참가 취소시에 PersonalBook에 들어가 있는 책을 삭제한다.
     * @param bookId 책 식별자
     * @param gatheringId 모임 식별자
     */
    @Transactional
    public void deleteBookForMeeting(Long bookId, Long gatheringId) {
        if (gatheringId == null) {
            return;
        }
        User userEntity = userValidator.findUserOrThrow(SecurityUtil.getCurrentUserId());
        personalBookRepository
                .findByUserIdAndBookIdAndGatheringId(userEntity.getId(), bookId, gatheringId)
                .ifPresent(personalBookRepository::delete);
    }

    private int resolvePageSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return size;
    }
}
