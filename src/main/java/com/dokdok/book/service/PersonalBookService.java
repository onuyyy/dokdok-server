package com.dokdok.book.service;

import com.dokdok.book.dto.request.BookCreateRequest;
import com.dokdok.book.dto.request.PersonalBookSortBy;
import com.dokdok.book.dto.request.PersonalBookSortOrder;
import com.dokdok.book.dto.response.BookListCursor;
import com.dokdok.book.dto.response.PersonalBookCursorPageResponse;
import com.dokdok.book.dto.response.PersonalBookCreateResponse;
import com.dokdok.book.dto.response.PersonalBookDetailResponse;
import com.dokdok.book.dto.response.PersonalBookListResponse;
import com.dokdok.book.dto.response.PersonalBookStatusCountsResponse;
import com.dokdok.book.entity.Book;
import com.dokdok.book.entity.BookReadingStatus;
import com.dokdok.book.entity.PersonalBook;
import com.dokdok.book.exception.BookErrorCode;
import com.dokdok.book.exception.BookException;
import com.dokdok.book.repository.BookRepository;
import com.dokdok.book.repository.PersonalBookListProjection;
import com.dokdok.book.repository.PersonalBookRepository;
import com.dokdok.book.repository.PersonalBookStatusCountProjection;
import com.dokdok.gathering.entity.Gathering;
import com.dokdok.global.util.SecurityUtil;
import com.dokdok.user.entity.User;
import com.dokdok.user.service.UserValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;

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

    public PersonalBookCursorPageResponse getPersonalBookListCursor(
            BookReadingStatus bookReadingStatus,
            Long gatheringId,
            PersonalBookSortBy sortBy,
            PersonalBookSortOrder sortOrder,
            BigDecimal minRating,
            BigDecimal maxRating,
            BigDecimal cursorRating,
            OffsetDateTime cursorAddedAt,
            Long cursorBookId,
            Integer size
    ) {
        User userEntity = userValidator.findUserOrThrow(SecurityUtil.getCurrentUserId());
        String readingStatus = bookReadingStatus != null ? bookReadingStatus.name() : null;
        PersonalBookSortBy resolvedSortBy = sortBy != null ? sortBy : PersonalBookSortBy.TIME;
        PersonalBookSortOrder resolvedSortOrder = sortOrder != null ? sortOrder : PersonalBookSortOrder.DESC;
        Comparator<PersonalBookListProjection> comparator = resolveComparator(resolvedSortBy, resolvedSortOrder);
        int pageSize = resolvePageSize(size);
        LocalDateTime cursorAddedAtValue = cursorAddedAt != null ? cursorAddedAt.toLocalDateTime() : null;

        List<PersonalBookListProjection> filtered = personalBookRepository
                .findPersonalBookAggregatesByUserIdAndGatheringIdAndReadingStatus(
                        userEntity.getId(),
                        gatheringId,
                        readingStatus
                )
                .stream()
                .filter(item -> isWithinRatingRange(item.getRating(), minRating, maxRating))
                .toList();

        List<PersonalBookListProjection> sorted = filtered.stream()
                .sorted(comparator)
                .toList();
        long totalCount = sorted.size();

        List<PersonalBookListProjection> afterCursor = applyCursor(
                sorted,
                comparator,
                resolvedSortBy,
                cursorRating,
                cursorAddedAtValue,
                cursorBookId
        );

        boolean hasNext = afterCursor.size() > pageSize;
        List<PersonalBookListProjection> pageResults = hasNext ? afterCursor.subList(0, pageSize) : afterCursor;
        List<PersonalBookListResponse> items = pageResults.stream()
                .map(PersonalBookListResponse::from)
                .toList();

        BookListCursor nextCursor = null;
        if (hasNext && !pageResults.isEmpty()) {
            PersonalBookListProjection last = pageResults.get(pageResults.size() - 1);
            nextCursor = BookListCursor.from(last.getRating(), last.getAddedAt(), last.getBookId());
        }

        PersonalBookStatusCountsResponse statusCounts = buildStatusCounts(userEntity.getId(), gatheringId);

        return PersonalBookCursorPageResponse.of(items, statusCounts, pageSize, hasNext, nextCursor, totalCount);
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

    @Transactional
    public void deleteBooks(List<Long> bookIds) {
        User userEntity = userValidator.findUserOrThrow(SecurityUtil.getCurrentUserId());

        List<Long> distinctBookIds = bookIds.stream()
                .distinct()
                .toList();

        for (Long bookId : distinctBookIds) {
            PersonalBook personalBook = bookValidator.validateInBookShelf(userEntity.getId(), bookId);
            personalBookRepository.delete(personalBook);
        }
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

    private Comparator<PersonalBookListProjection> resolveComparator(
            PersonalBookSortBy sortBy,
            PersonalBookSortOrder sortOrder
    ) {
        if (sortBy == PersonalBookSortBy.RATING) {
            return resolveRatingComparator(sortOrder);
        }
        return resolveTimeComparator(sortOrder);
    }

    private Comparator<PersonalBookListProjection> resolveTimeComparator(PersonalBookSortOrder sortOrder) {
        if (sortOrder == PersonalBookSortOrder.ASC) {
            return Comparator
                    .comparing(PersonalBookListProjection::getAddedAt)
                    .thenComparing(PersonalBookListProjection::getBookId);
        }
        return Comparator
                .comparing(PersonalBookListProjection::getAddedAt, Comparator.reverseOrder())
                .thenComparing(PersonalBookListProjection::getBookId, Comparator.reverseOrder());
    }

    private Comparator<PersonalBookListProjection> resolveRatingComparator(PersonalBookSortOrder sortOrder) {
        if (sortOrder == PersonalBookSortOrder.ASC) {
            return Comparator
                    .comparing(PersonalBookListProjection::getRating, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(PersonalBookListProjection::getAddedAt)
                    .thenComparing(PersonalBookListProjection::getBookId);
        }
        return Comparator
                .comparing(PersonalBookListProjection::getRating, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(PersonalBookListProjection::getAddedAt, Comparator.reverseOrder())
                .thenComparing(PersonalBookListProjection::getBookId, Comparator.reverseOrder());
    }

    private List<PersonalBookListProjection> applyCursor(
            List<PersonalBookListProjection> sorted,
            Comparator<PersonalBookListProjection> comparator,
            PersonalBookSortBy sortBy,
            BigDecimal cursorRating,
            LocalDateTime cursorAddedAt,
            Long cursorBookId
    ) {
        if (cursorAddedAt == null || cursorBookId == null) {
            return sorted;
        }

        BigDecimal resolvedCursorRating = cursorRating;
        if (sortBy == PersonalBookSortBy.RATING && resolvedCursorRating == null) {
            resolvedCursorRating = sorted.stream()
                    .filter(item -> cursorAddedAt.equals(item.getAddedAt()) && cursorBookId.equals(item.getBookId()))
                    .map(PersonalBookListProjection::getRating)
                    .findFirst()
                    .orElse(null);
        }

        PersonalBookListProjection cursor = CursorProjection.of(cursorBookId, cursorAddedAt, resolvedCursorRating);
        return sorted.stream()
                .filter(item -> comparator.compare(item, cursor) > 0)
                .toList();
    }

    private boolean isWithinRatingRange(
            BigDecimal rating,
            BigDecimal minRating,
            BigDecimal maxRating
    ) {
        if (minRating == null && maxRating == null) {
            return true;
        }
        if (rating == null) {
            return false;
        }

        boolean passMin = minRating == null || rating.compareTo(minRating) >= 0;
        boolean passMax = maxRating == null || rating.compareTo(maxRating) <= 0;
        return passMin && passMax;
    }

    private PersonalBookStatusCountsResponse buildStatusCounts(Long userId, Long gatheringId) {
        EnumMap<BookReadingStatus, Long> counts = new EnumMap<>(BookReadingStatus.class);
        counts.put(BookReadingStatus.READING, 0L);
        counts.put(BookReadingStatus.COMPLETED, 0L);
        counts.put(BookReadingStatus.PENDING, 0L);

        List<PersonalBookStatusCountProjection> statusCounts = personalBookRepository
                .countPersonalBookStatusByUserIdAndGatheringId(userId, gatheringId);

        for (PersonalBookStatusCountProjection statusCount : statusCounts) {
            try {
                BookReadingStatus status = BookReadingStatus.valueOf(statusCount.getReadingStatus());
                counts.put(status, statusCount.getCount());
            } catch (IllegalArgumentException ignored) {
                // no-op
            }
        }

        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        return PersonalBookStatusCountsResponse.builder()
                .reading(counts.get(BookReadingStatus.READING))
                .completed(counts.get(BookReadingStatus.COMPLETED))
                .pending(counts.get(BookReadingStatus.PENDING))
                .total(total)
                .build();
    }

    private record CursorProjection(
            Long bookId,
            LocalDateTime addedAt,
            BigDecimal rating
    ) implements PersonalBookListProjection {
        private static CursorProjection of(Long bookId, LocalDateTime addedAt, BigDecimal rating) {
            return new CursorProjection(bookId, addedAt, rating);
        }

        @Override
        public Long getBookId() {
            return bookId;
        }

        @Override
        public LocalDateTime getAddedAt() {
            return addedAt;
        }

        @Override
        public BigDecimal getRating() {
            return rating;
        }

        @Override
        public String getTitle() {
            return null;
        }

        @Override
        public String getPublisher() {
            return null;
        }

        @Override
        public String getAuthors() {
            return null;
        }

        @Override
        public BookReadingStatus getBookReadingStatus() {
            return null;
        }

        @Override
        public String getThumbnail() {
            return null;
        }

        @Override
        public String getGatherings() {
            return null;
        }
    }
}
