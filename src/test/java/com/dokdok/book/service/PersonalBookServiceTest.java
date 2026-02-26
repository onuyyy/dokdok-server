package com.dokdok.book.service;

import com.dokdok.book.dto.request.BookCreateRequest;
import com.dokdok.book.dto.request.PersonalBookSortBy;
import com.dokdok.book.dto.request.PersonalBookSortOrder;
import com.dokdok.book.dto.response.PersonalBookCursorPageResponse;
import com.dokdok.book.dto.response.PersonalBookCreateResponse;
import com.dokdok.book.dto.response.PersonalBookDetailResponse;
import com.dokdok.book.dto.response.PersonalBookListResponse;
import com.dokdok.book.entity.Book;
import com.dokdok.book.entity.BookReadingStatus;
import com.dokdok.book.entity.PersonalBook;
import com.dokdok.book.exception.BookErrorCode;
import com.dokdok.book.exception.BookException;
import com.dokdok.book.repository.BookRepository;
import com.dokdok.book.repository.PersonalBookListProjection;
import com.dokdok.book.repository.PersonalBookRepository;
import com.dokdok.global.util.SecurityUtil;
import com.dokdok.user.entity.User;
import com.dokdok.user.exception.UserErrorCode;
import com.dokdok.user.exception.UserException;
import com.dokdok.user.service.UserValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PersonalBookService 테스트")
class PersonalBookServiceTest {

    @InjectMocks
    private PersonalBookService personalBookService;

    @Mock
    private PersonalBookRepository personalBookRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserValidator userValidator;

    @Mock
    private BookValidator bookValidator;

    private MockedStatic<SecurityUtil> securityUtilMock;

    @BeforeEach
    void setUp() {
        securityUtilMock = mockStatic(SecurityUtil.class);
    }

    @AfterEach
    void tearDown() {
        securityUtilMock.close();
    }

    @Test
    @DisplayName("개인 도서 등록 성공 시 READING 상태로 저장")
    void createBook_Success() {
        // given
        Long userId = 1L;
        BookCreateRequest request = BookCreateRequest.builder()
                .isbn("9788994757254")
                .title("테스트 책")
                .authors("작가")
                .publisher("출판사")
                .thumbnail("thumbnail-url")
                .build();

        User user = User.builder()
                .id(userId)
                .kakaoId(12345L)
                .nickname("tester")
                .build();

        Book book = Book.builder()
                .id(10L)
                .isbn(request.isbn())
                .bookName(request.title())
                .author(request.authors())
                .publisher(request.publisher())
                .build();

        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
        when(userValidator.findUserOrThrow(userId)).thenReturn(user);
        when(bookRepository.findByIsbn(request.isbn())).thenReturn(Optional.of(book));
        doNothing().when(bookValidator).validateDuplicatePersonalBook(userId, book.getId());

        // when
        PersonalBookCreateResponse response = personalBookService.createBook(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.isbn()).isEqualTo(request.isbn());
        assertThat(response.readingStatus()).isEqualTo(BookReadingStatus.READING);
        assertThat(response.addedAt()).isNotNull();

        ArgumentCaptor<PersonalBook> personalBookCaptor = ArgumentCaptor.forClass(PersonalBook.class);
        verify(personalBookRepository, times(1)).save(personalBookCaptor.capture());

        PersonalBook savedPersonalBook = personalBookCaptor.getValue();
        assertThat(savedPersonalBook.getUser()).isEqualTo(user);
        assertThat(savedPersonalBook.getBook()).isEqualTo(book);
        assertThat(savedPersonalBook.getReadingStatus()).isEqualTo(BookReadingStatus.READING);
        assertThat(savedPersonalBook.getAddedAt()).isNotNull();
        assertThat(response.addedAt()).isEqualTo(savedPersonalBook.getAddedAt());

        securityUtilMock.verify(SecurityUtil::getCurrentUserId, times(1));
        verify(userValidator, times(1)).findUserOrThrow(userId);
        verify(bookRepository, times(1)).findByIsbn(request.isbn());
        verify(bookRepository, never()).save(any(Book.class));
        verify(bookValidator, times(1)).validateDuplicatePersonalBook(userId, book.getId());
    }

    @Test
    @DisplayName("현재 사용자 정보가 없으면 UserException 발생")
    void createBook_UserNotFound() {
        // given
        Long userId = 99L;
        BookCreateRequest request = BookCreateRequest.builder()
                .isbn("9788994757254")
                .build();

        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
        when(userValidator.findUserOrThrow(userId)).thenThrow(new UserException(UserErrorCode.USER_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> personalBookService.createBook(request))
                .isInstanceOf(UserException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);

        securityUtilMock.verify(SecurityUtil::getCurrentUserId, times(1));
        verify(userValidator, times(1)).findUserOrThrow(userId);
        verify(bookRepository, never()).findByIsbn(anyString());
        verify(personalBookRepository, never()).save(any());
    }

    @Test
    @DisplayName("도서 정보를 찾지 못하면 새로운 책을 저장 후 개인 도서 등록")
    void createBook_CreateBookWhenNotFound() {
        // given
        Long userId = 1L;
        BookCreateRequest request = BookCreateRequest.builder()
                .isbn("9788994757254")
                .title("새로운 책")
                .authors("새로운 작가")
                .publisher("새로운 출판사")
                .thumbnail("new-thumbnail")
                .build();

        User user = User.builder()
                .id(userId)
                .kakaoId(12345L)
                .nickname("tester")
                .build();

        Book savedBook = Book.builder()
                .id(10L)
                .isbn(request.isbn())
                .bookName(request.title())
                .author(request.authors())
                .publisher(request.publisher())
                .thumbnail(request.thumbnail())
                .build();

        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
        when(userValidator.findUserOrThrow(userId)).thenReturn(user);
        when(bookRepository.findByIsbn(request.isbn())).thenReturn(Optional.empty());
        when(bookRepository.save(any(Book.class))).thenReturn(savedBook);
        doNothing().when(bookValidator).validateDuplicatePersonalBook(userId, savedBook.getId());

        // when
        PersonalBookCreateResponse response = personalBookService.createBook(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.isbn()).isEqualTo(request.isbn());
        assertThat(response.readingStatus()).isEqualTo(BookReadingStatus.READING);
        assertThat(response.addedAt()).isNotNull();

        securityUtilMock.verify(SecurityUtil::getCurrentUserId, times(1));
        verify(userValidator, times(1)).findUserOrThrow(userId);
        verify(bookRepository, times(1)).findByIsbn(request.isbn());
        verify(bookRepository, times(1)).save(any(Book.class));
        verify(bookValidator, times(1)).validateDuplicatePersonalBook(userId, savedBook.getId());
        verify(personalBookRepository, times(1)).save(any(PersonalBook.class));
    }

    @Test
    @DisplayName("같은 사용자가 같은 도서를 다시 등록하면 BookException 발생")
    void createBook_DuplicatePersonalBook() {
        // given
        Long userId = 1L;
        Long bookId = 10L;
        BookCreateRequest request = BookCreateRequest.builder()
                .isbn("9788994757254")
                .title("테스트 책")
                .authors("작가")
                .publisher("출판사")
                .thumbnail("thumbnail-url")
                .build();

        User user = User.builder()
                .id(userId)
                .kakaoId(12345L)
                .nickname("tester")
                .build();

        Book book = Book.builder()
                .id(bookId)
                .isbn(request.isbn())
                .bookName("테스트 책")
                .author("작가")
                .publisher("출판사")
                .build();

        PersonalBook existing = PersonalBook.create(user, book, BookReadingStatus.READING);

        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
        when(userValidator.findUserOrThrow(userId)).thenReturn(user);
        when(bookRepository.findByIsbn(request.isbn())).thenReturn(Optional.of(book));
        doThrow(new BookException(BookErrorCode.BOOK_ALREADY_EXISTS))
                .when(bookValidator).validateDuplicatePersonalBook(userId, bookId);

        // when & then
        assertThatThrownBy(() -> personalBookService.createBook(request))
                .isInstanceOf(BookException.class)
                .hasFieldOrPropertyWithValue("errorCode", BookErrorCode.BOOK_ALREADY_EXISTS);

        securityUtilMock.verify(SecurityUtil::getCurrentUserId, times(1));
        verify(userValidator, times(1)).findUserOrThrow(userId);
        verify(bookRepository, times(1)).findByIsbn(request.isbn());
        verify(bookValidator, times(1)).validateDuplicatePersonalBook(userId, bookId);
        verify(personalBookRepository, never()).save(any());
    }

    @Test
    @DisplayName("내 책장 목록 조회 시 PersonalBookListResponse로 매핑")
    void getPersonalBookList_Success() {
        // given
        Long userId = 1L;
        Long gatheringId = 5L;
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime addedAt = LocalDateTime.now();
        String thumbnail = "thumbnail-url";
        String gatherings = "[{\"gatheringId\":5,\"gatheringName\":\"독서 모임\"}]";
        BookReadingStatus readingStatus = BookReadingStatus.READING;

        User user = User.builder()
                .id(userId)
                .kakaoId(12345L)
                .nickname("tester")
                .build();

        Book book = Book.builder()
                .id(10L)
                .bookName("테스트 책")
                .publisher("테스트 출판사")
                .author("테스트 저자")
                .thumbnail(thumbnail)
                .build();

        PersonalBookListProjection projection = new PersonalBookListProjection() {
            @Override
            public Long getBookId() {
                return book.getId();
            }

            @Override
            public String getTitle() {
                return book.getBookName();
            }

            @Override
            public String getPublisher() {
                return book.getPublisher();
            }

            @Override
            public String getAuthors() {
                return book.getAuthor();
            }

            @Override
            public BookReadingStatus getBookReadingStatus() {
                return readingStatus;
            }

            @Override
            public String getThumbnail() {
                return thumbnail;
            }

            @Override
            public java.math.BigDecimal getRating() {
                return new java.math.BigDecimal("4.5");
            }

            @Override
            public String getGatherings() {
                return gatherings;
            }

            @Override
            public LocalDateTime getAddedAt() {
                return addedAt;
            }
        };

        Page<PersonalBookListProjection> page = new PageImpl<>(List.of(projection), pageable, 1);

        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
        when(userValidator.findUserOrThrow(userId)).thenReturn(user);
        when(personalBookRepository.findPersonalBooksByUserIdReadingStatusAndGatheringId(
                userId,
                gatheringId,
                readingStatus.name(),
                pageable
        )).thenReturn(page);

        // when
        Page<PersonalBookListResponse> responses = personalBookService.getPersonalBookList(readingStatus, gatheringId, pageable);

        // then
        assertThat(responses.getContent()).hasSize(1);
        PersonalBookListResponse response = responses.getContent().getFirst();
        assertThat(response.bookId()).isEqualTo(book.getId());
        assertThat(response.title()).isEqualTo(book.getBookName());
        assertThat(response.publisher()).isEqualTo(book.getPublisher());
        assertThat(response.authors()).isEqualTo(book.getAuthor());
        assertThat(response.bookReadingStatus()).isEqualTo(readingStatus);
        assertThat(response.thumbnail()).isEqualTo(thumbnail);
        assertThat(response.rating()).isEqualByComparingTo("4.5");
        assertThat(response.gatherings()).hasSize(1);
        assertThat(response.gatherings().getFirst().gatheringName()).isEqualTo("독서 모임");

        securityUtilMock.verify(SecurityUtil::getCurrentUserId, times(1));
        verify(userValidator, times(1)).findUserOrThrow(userId);
        verify(personalBookRepository, times(1)).findPersonalBooksByUserIdReadingStatusAndGatheringId(
                userId,
                gatheringId,
                readingStatus.name(),
                pageable
        );
    }

    @Test
    @DisplayName("내 책장 목록이 비어있으면 BookException 발생")
    void getPersonalBookList_Empty() {
        // given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        User user = User.builder()
                .id(userId)
                .kakaoId(12345L)
                .nickname("tester")
                .build();

        Page<PersonalBookListProjection> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
        when(userValidator.findUserOrThrow(userId)).thenReturn(user);
        when(personalBookRepository.findPersonalBooksByUserIdReadingStatusAndGatheringId(
                userId,
                null,
                null,
                pageable
        )).thenReturn(emptyPage);

        // when & then
        assertThatThrownBy(() -> personalBookService.getPersonalBookList(null, null, pageable))
                .isInstanceOf(BookException.class)
                .hasFieldOrPropertyWithValue("errorCode", BookErrorCode.BOOK_NOT_IN_SHELF);

        securityUtilMock.verify(SecurityUtil::getCurrentUserId, times(1));
        verify(userValidator, times(1)).findUserOrThrow(userId);
        verify(personalBookRepository, times(1)).findPersonalBooksByUserIdReadingStatusAndGatheringId(
                userId,
                null,
                null,
                pageable
        );
    }

    @Test
    @DisplayName("내 책장 단일 조회 시 PersonalBookDetailResponse로 매핑")
    void getPersonalBookDetail_Success() {
        // given
        Long userId = 1L;
        Long bookId = 10L;

        User user = User.builder()
                .id(userId)
                .kakaoId(12345L)
                .nickname("tester")
                .build();

        Book book = Book.builder()
                .id(bookId)
                .bookName("테스트 책")
                .publisher("테스트 출판사")
                .author("테스트 저자")
                .build();

        PersonalBook personalBook = PersonalBook.builder()
                .id(100L)
                .user(user)
                .book(book)
                .readingStatus(BookReadingStatus.COMPLETED)
                .build();

        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
        when(userValidator.findUserOrThrow(userId)).thenReturn(user);
        when(bookValidator.validateInBookShelf(userId, bookId)).thenReturn(personalBook);

        // when
        PersonalBookDetailResponse response = personalBookService.getPersonalBook(bookId);

        // then
        assertThat(response.personalBookId()).isEqualTo(personalBook.getId());
        assertThat(response.title()).isEqualTo(book.getBookName());
        assertThat(response.publisher()).isEqualTo(book.getPublisher());
        assertThat(response.authors()).isEqualTo(book.getAuthor());
        assertThat(response.bookReadingStatus()).isEqualTo(BookReadingStatus.COMPLETED);

        securityUtilMock.verify(SecurityUtil::getCurrentUserId, times(1));
        verify(userValidator, times(1)).findUserOrThrow(userId);
        verify(bookValidator, times(1)).validateInBookShelf(userId, bookId);
    }

    @Test
    @DisplayName("내 책장 단일 조회 시 책이 없으면 BookException 발생")
    void getPersonalBookDetail_NotFound() {
        // given
        Long userId = 1L;
        Long bookId = 10L;

        User user = User.builder()
                .id(userId)
                .kakaoId(12345L)
                .nickname("tester")
                .build();

        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
        when(userValidator.findUserOrThrow(userId)).thenReturn(user);
        when(bookValidator.validateInBookShelf(userId, bookId))
                .thenThrow(new BookException(BookErrorCode.BOOK_NOT_IN_SHELF));

        // when & then
        assertThatThrownBy(() -> personalBookService.getPersonalBook(bookId))
                .isInstanceOf(BookException.class)
                .hasFieldOrPropertyWithValue("errorCode", BookErrorCode.BOOK_NOT_IN_SHELF);

        securityUtilMock.verify(SecurityUtil::getCurrentUserId, times(1));
        verify(userValidator, times(1)).findUserOrThrow(userId);
        verify(bookValidator, times(1)).validateInBookShelf(userId, bookId);
    }

    @Test
    @DisplayName("내 책장에서 도서를 삭제하면 성공적으로 삭제된다")
    void deleteBook_Success() {
        // given
        Long userId = 1L;
        Long bookId = 10L;

        User user = User.builder()
                .id(userId)
                .kakaoId(12345L)
                .nickname("tester")
                .build();

        Book book = Book.builder()
                .id(bookId)
                .bookName("테스트 책")
                .publisher("테스트 출판사")
                .author("테스트 저자")
                .isbn("9788994757254")
                .build();

        PersonalBook personalBook = PersonalBook.builder()
                .id(100L)
                .user(user)
                .book(book)
                .readingStatus(BookReadingStatus.READING)
                .build();

        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
        when(userValidator.findUserOrThrow(userId)).thenReturn(user);
        when(bookValidator.validateInBookShelf(userId, bookId)).thenReturn(personalBook);

        // when
        personalBookService.deleteBook(bookId);

        // then
        securityUtilMock.verify(SecurityUtil::getCurrentUserId, times(1));
        verify(userValidator, times(1)).findUserOrThrow(userId);
        verify(bookValidator, times(1)).validateInBookShelf(userId, bookId);
        verify(personalBookRepository, times(1)).delete(personalBook);
    }

    @Test
    @DisplayName("내 책장에 없는 도서를 삭제하려 하면 BookException 발생")
    void deleteBook_NotFound() {
        // given
        Long userId = 1L;
        Long bookId = 10L;

        User user = User.builder()
                .id(userId)
                .kakaoId(12345L)
                .nickname("tester")
                .build();

        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
        when(userValidator.findUserOrThrow(userId)).thenReturn(user);
        when(bookValidator.validateInBookShelf(userId, bookId))
                .thenThrow(new BookException(BookErrorCode.BOOK_NOT_IN_SHELF));

        // when & then
        assertThatThrownBy(() -> personalBookService.deleteBook(bookId))
                .isInstanceOf(BookException.class)
                .hasFieldOrPropertyWithValue("errorCode", BookErrorCode.BOOK_NOT_IN_SHELF);

        securityUtilMock.verify(SecurityUtil::getCurrentUserId, times(1));
        verify(userValidator, times(1)).findUserOrThrow(userId);
        verify(bookValidator, times(1)).validateInBookShelf(userId, bookId);
        verify(personalBookRepository, never()).delete(any());
    }

    @Test
    @DisplayName("내 책장에서 도서를 다건 삭제하면 성공적으로 삭제된다")
    void deleteBooks_Success() {
        // given
        Long userId = 1L;
        List<Long> bookIds = List.of(10L, 11L);

        User user = User.builder()
                .id(userId)
                .kakaoId(12345L)
                .nickname("tester")
                .build();

        Book firstBook = Book.builder().id(10L).bookName("첫 번째 책").build();
        Book secondBook = Book.builder().id(11L).bookName("두 번째 책").build();

        PersonalBook firstPersonalBook = PersonalBook.builder()
                .id(100L)
                .user(user)
                .book(firstBook)
                .readingStatus(BookReadingStatus.READING)
                .build();
        PersonalBook secondPersonalBook = PersonalBook.builder()
                .id(101L)
                .user(user)
                .book(secondBook)
                .readingStatus(BookReadingStatus.READING)
                .build();

        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
        when(userValidator.findUserOrThrow(userId)).thenReturn(user);
        when(bookValidator.validateInBookShelf(userId, 10L)).thenReturn(firstPersonalBook);
        when(bookValidator.validateInBookShelf(userId, 11L)).thenReturn(secondPersonalBook);

        // when
        personalBookService.deleteBooks(bookIds);

        // then
        securityUtilMock.verify(SecurityUtil::getCurrentUserId, times(1));
        verify(userValidator, times(1)).findUserOrThrow(userId);
        verify(bookValidator, times(1)).validateInBookShelf(userId, 10L);
        verify(bookValidator, times(1)).validateInBookShelf(userId, 11L);
        verify(personalBookRepository, times(1)).delete(firstPersonalBook);
        verify(personalBookRepository, times(1)).delete(secondPersonalBook);
    }

    @Test
    @DisplayName("다건 삭제에서 중복 bookId는 한 번만 처리된다")
    void deleteBooks_DeduplicateIds() {
        // given
        Long userId = 1L;
        List<Long> bookIds = List.of(10L, 10L, 10L);

        User user = User.builder()
                .id(userId)
                .kakaoId(12345L)
                .nickname("tester")
                .build();

        Book book = Book.builder().id(10L).bookName("중복 책").build();
        PersonalBook personalBook = PersonalBook.builder()
                .id(100L)
                .user(user)
                .book(book)
                .readingStatus(BookReadingStatus.READING)
                .build();

        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
        when(userValidator.findUserOrThrow(userId)).thenReturn(user);
        when(bookValidator.validateInBookShelf(userId, 10L)).thenReturn(personalBook);

        // when
        personalBookService.deleteBooks(bookIds);

        // then
        verify(bookValidator, times(1)).validateInBookShelf(userId, 10L);
        verify(personalBookRepository, times(1)).delete(personalBook);
    }

    @Test
    @DisplayName("커서 목록 조회 시 별점 범위 필터가 적용된다")
    void getPersonalBookListCursor_FilterByRatingRange() {
        // given
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .kakaoId(12345L)
                .nickname("tester")
                .build();

        LocalDateTime now = LocalDateTime.now();
        PersonalBookListProjection high = projection(3L, "별점 5점", new BigDecimal("5.0"), now.minusDays(1));
        PersonalBookListProjection mid = projection(2L, "별점 3.5점", new BigDecimal("3.5"), now.minusDays(2));
        PersonalBookListProjection low = projection(1L, "별점 2점", new BigDecimal("2.0"), now.minusDays(3));
        PersonalBookListProjection unrated = projection(4L, "별점 없음", null, now.minusDays(4));

        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
        when(userValidator.findUserOrThrow(userId)).thenReturn(user);
        when(personalBookRepository.findPersonalBookAggregatesByUserIdAndGatheringIdAndReadingStatus(userId, null, null))
                .thenReturn(List.of(high, mid, low, unrated));
        when(personalBookRepository.countPersonalBookStatusByUserIdAndGatheringId(userId, null)).thenReturn(List.of());

        // when
        PersonalBookCursorPageResponse response = personalBookService.getPersonalBookListCursor(
                null,
                null,
                PersonalBookSortBy.RATING,
                PersonalBookSortOrder.DESC,
                new BigDecimal("3.0"),
                new BigDecimal("4.0"),
                null,
                null,
                null,
                10
        );

        // then
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().getFirst().bookId()).isEqualTo(2L);
        assertThat(response.getItems().getFirst().rating()).isEqualByComparingTo("3.5");
        assertThat(response.getTotalCount()).isEqualTo(1L);
        assertThat(response.isHasNext()).isFalse();
    }

    @Test
    @DisplayName("RATING 정렬에서 cursorRating/cursorAddedAt/cursorBookId 기준으로 다음 페이지를 조회한다")
    void getPersonalBookListCursor_ApplyRatingCursor() {
        // given
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .kakaoId(12345L)
                .nickname("tester")
                .build();

        LocalDateTime firstAddedAt = LocalDateTime.of(2026, 2, 1, 10, 0);
        LocalDateTime secondAddedAt = LocalDateTime.of(2026, 1, 25, 10, 0);
        LocalDateTime thirdAddedAt = LocalDateTime.of(2026, 1, 20, 10, 0);

        PersonalBookListProjection first = projection(30L, "별점 5점", new BigDecimal("5.0"), firstAddedAt);
        PersonalBookListProjection second = projection(20L, "별점 4점", new BigDecimal("4.0"), secondAddedAt);
        PersonalBookListProjection third = projection(10L, "별점 3점", new BigDecimal("3.0"), thirdAddedAt);

        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
        when(userValidator.findUserOrThrow(userId)).thenReturn(user);
        when(personalBookRepository.findPersonalBookAggregatesByUserIdAndGatheringIdAndReadingStatus(userId, null, null))
                .thenReturn(List.of(first, second, third));
        when(personalBookRepository.countPersonalBookStatusByUserIdAndGatheringId(userId, null)).thenReturn(List.of());

        // when
        PersonalBookCursorPageResponse response = personalBookService.getPersonalBookListCursor(
                null,
                null,
                PersonalBookSortBy.RATING,
                PersonalBookSortOrder.DESC,
                null,
                null,
                new BigDecimal("4.0"),
                OffsetDateTime.of(secondAddedAt, ZoneOffset.UTC),
                20L,
                10
        );

        // then
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().getFirst().bookId()).isEqualTo(10L);
        assertThat(response.getItems().getFirst().rating()).isEqualByComparingTo("3.0");
        assertThat(response.getTotalCount()).isEqualTo(3L);
        assertThat(response.isHasNext()).isFalse();
    }

    private PersonalBookListProjection projection(
            Long bookId,
            String title,
            BigDecimal rating,
            LocalDateTime addedAt
    ) {
        return new PersonalBookListProjection() {
            @Override
            public Long getBookId() {
                return bookId;
            }

            @Override
            public String getTitle() {
                return title;
            }

            @Override
            public String getPublisher() {
                return "출판사";
            }

            @Override
            public String getAuthors() {
                return "저자";
            }

            @Override
            public BookReadingStatus getBookReadingStatus() {
                return BookReadingStatus.READING;
            }

            @Override
            public String getThumbnail() {
                return "thumbnail";
            }

            @Override
            public BigDecimal getRating() {
                return rating;
            }

            @Override
            public String getGatherings() {
                return "[]";
            }

            @Override
            public LocalDateTime getAddedAt() {
                return addedAt;
            }
        };
    }
}
