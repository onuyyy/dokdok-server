package com.dokdok.book.service;

import com.dokdok.book.dto.request.PersonalReadingRecordCreateRequest;
import com.dokdok.book.dto.request.PersonalReadingRecordUpdateRequest;
import com.dokdok.book.dto.response.CursorPageResponse;
import com.dokdok.book.dto.response.PersonalBookGatheringResponse;
import com.dokdok.book.dto.response.PersonalReadingRecordCreateResponse;
import com.dokdok.book.dto.response.PersonalReadingRecordListResponse;
import com.dokdok.book.dto.response.ReadingRecordCursor;
import com.dokdok.book.entity.Book;
import com.dokdok.book.entity.BookReadingStatus;
import com.dokdok.book.entity.PersonalBook;
import com.dokdok.book.entity.PersonalReadingRecord;
import com.dokdok.book.entity.RecordType;
import com.dokdok.book.exception.BookErrorCode;
import com.dokdok.book.exception.BookException;
import com.dokdok.book.exception.RecordErrorCode;
import com.dokdok.book.exception.RecordException;
import com.dokdok.book.repository.PersonalBookGatheringProjection;
import com.dokdok.book.repository.PersonalBookRepository;
import com.dokdok.book.repository.PersonalReadingRecordRepository;
import com.dokdok.global.util.SecurityUtil;
import com.dokdok.user.entity.User;
import com.dokdok.user.service.UserValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PersonalReadingRecordService 테스트")
class PersonalReadingRecordServiceTest {

    @InjectMocks
    private PersonalReadingRecordService personalReadingRecordService;

    @Mock
    private PersonalReadingRecordRepository personalReadingRecordRepository;

    @Mock
    private PersonalBookRepository personalBookRepository;

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
    @DisplayName("메모 기록 생성 시 meta는 null로 저장된다")
    void createMemoRecord_Success() {
        // given
        Long userId = 1L;
        Long personalBookId = 100L;
        Long bookId = 10L;
        PersonalReadingRecordCreateRequest request = new PersonalReadingRecordCreateRequest(
                RecordType.MEMO,
                "메모 내용",
                new HashMap<>()
        );

        User user = User.builder()
                .id(userId)
                .kakaoId(12345L)
                .nickname("tester")
                .build();

        Book book = Book.builder()
                .id(bookId)
                .isbn("9788994757254")
                .bookName("테스트 책")
                .author("저자")
                .publisher("출판사")
                .build();

        PersonalBook personalBook = PersonalBook.builder()
                .id(personalBookId)
                .user(user)
                .book(book)
                .readingStatus(BookReadingStatus.READING)
                .build();

        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
        when(userValidator.findUserOrThrow(userId)).thenReturn(user);
        when(bookValidator.validatePersonalBook(userId, personalBookId)).thenReturn(personalBook);

        // when
        PersonalReadingRecordCreateResponse response = personalReadingRecordService.create(personalBookId, request);

        // then
        assertThat(response.recordType()).isEqualTo(RecordType.MEMO);
        assertThat(response.recordContent()).isEqualTo(request.recordContent());
        assertThat(response.meta()).isNull();
        assertThat(response.bookId()).isEqualTo(bookId);

        ArgumentCaptor<com.dokdok.book.entity.PersonalReadingRecord> recordCaptor =
                ArgumentCaptor.forClass(com.dokdok.book.entity.PersonalReadingRecord.class);
        verify(personalReadingRecordRepository, times(1)).save(recordCaptor.capture());

        com.dokdok.book.entity.PersonalReadingRecord savedRecord = recordCaptor.getValue();
        assertThat(savedRecord.getRecordType()).isEqualTo(RecordType.MEMO);
        assertThat(savedRecord.getMeta()).isNull();
        assertThat(savedRecord.getPersonalBook()).isEqualTo(personalBook);

        securityUtilMock.verify(SecurityUtil::getCurrentUserId, times(1));
        verify(userValidator, times(1)).findUserOrThrow(userId);
        verify(bookValidator, times(1)).validatePersonalBook(userId, personalBookId);
    }

    @Test
    @DisplayName("인용 기록 생성 시 meta가 정규화되어 저장된다")
    void createQuoteRecord_Success() {
        // given
        Long userId = 1L;
        Long personalBookId = 200L;
        Long bookId = 20L;
        Map<String, Object> meta = new HashMap<>();
        meta.put("page", "12");
        meta.put("excerpt", "인용 내용");

        PersonalReadingRecordCreateRequest request = new PersonalReadingRecordCreateRequest(
                RecordType.QUOTE,
                "인용 기록",
                meta
        );

        User user = User.builder()
                .id(userId)
                .kakaoId(98765L)
                .nickname("reader")
                .build();

        Book book = Book.builder()
                .id(bookId)
                .isbn("9781234567890")
                .bookName("다른 책")
                .author("다른 저자")
                .publisher("다른 출판사")
                .build();

        PersonalBook personalBook = PersonalBook.builder()
                .id(personalBookId)
                .user(user)
                .book(book)
                .readingStatus(BookReadingStatus.READING)
                .build();

        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
        when(userValidator.findUserOrThrow(userId)).thenReturn(user);
        when(bookValidator.validatePersonalBook(userId, personalBookId)).thenReturn(personalBook);

        // when
        PersonalReadingRecordCreateResponse response = personalReadingRecordService.create(personalBookId, request);

        // then
        assertThat(response.recordType()).isEqualTo(RecordType.QUOTE);
        assertThat(response.recordContent()).isEqualTo(request.recordContent());
        assertThat(response.meta()).isNotNull();
        assertThat(response.meta().get("page")).isEqualTo("12");
        assertThat(response.meta().get("excerpt")).isEqualTo("인용 내용");
        assertThat(response.bookId()).isEqualTo(bookId);

        ArgumentCaptor<com.dokdok.book.entity.PersonalReadingRecord> recordCaptor =
                ArgumentCaptor.forClass(com.dokdok.book.entity.PersonalReadingRecord.class);
        verify(personalReadingRecordRepository, times(1)).save(recordCaptor.capture());

        com.dokdok.book.entity.PersonalReadingRecord savedRecord = recordCaptor.getValue();
        assertThat(savedRecord.getMeta()).isNotNull();
        assertThat(savedRecord.getMeta().get("page")).isEqualTo("12");
        assertThat(savedRecord.getMeta().get("excerpt")).isEqualTo("인용 내용");

        securityUtilMock.verify(SecurityUtil::getCurrentUserId, times(1));
        verify(userValidator, times(1)).findUserOrThrow(userId);
        verify(bookValidator, times(1)).validatePersonalBook(userId, personalBookId);
    }

    @Test
    @DisplayName("인용 기록 meta가 없으면 RecordException이 발생한다")
    void createQuoteRecord_MissingMeta() {
        // given
        Long userId = 1L;
        Long personalBookId = 300L;
        Long bookId = 30L;
        PersonalReadingRecordCreateRequest request = new PersonalReadingRecordCreateRequest(
                RecordType.QUOTE,
                "인용 기록",
                null
        );

        User user = User.builder()
                .id(userId)
                .kakaoId(123L)
                .nickname("reader")
                .build();

        PersonalBook personalBook = PersonalBook.builder()
                .id(personalBookId)
                .user(user)
                .book(Book.builder().id(bookId).isbn("9780000000000").bookName("책").author("저자").publisher("출판").build())
                .readingStatus(BookReadingStatus.READING)
                .build();

        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
        when(userValidator.findUserOrThrow(userId)).thenReturn(user);
        when(bookValidator.validatePersonalBook(userId, personalBookId)).thenReturn(personalBook);

        // when & then
        assertThatThrownBy(() -> personalReadingRecordService.create(personalBookId, request))
                .isInstanceOf(RecordException.class)
                .hasFieldOrPropertyWithValue("errorCode", RecordErrorCode.INVALID_RECORD_REQUEST);

        verify(personalReadingRecordRepository, never()).save(any());
        securityUtilMock.verify(SecurityUtil::getCurrentUserId, times(1));
        verify(userValidator, times(1)).findUserOrThrow(userId);
        verify(bookValidator, times(1)).validatePersonalBook(userId, personalBookId);
    }

    @Test
    @DisplayName("독서 기록을 수정하면 내용과 meta가 업데이트된다")
    void updateRecord_Success() {
        // given
        Long userId = 1L;
        Long personalBookId = 400L;
        Long bookId = 40L;
        Long recordId = 5L;

        Map<String, Object> meta = new HashMap<>();
        meta.put("page", "30");
        meta.put("excerpt", "수정된 인용문");

        PersonalReadingRecordUpdateRequest request = new PersonalReadingRecordUpdateRequest(
                RecordType.QUOTE,
                "수정된 기록 내용",
                meta
        );

        User user = User.builder()
                .id(userId)
                .kakaoId(222L)
                .nickname("editor")
                .build();

        PersonalBook personalBook = PersonalBook.builder()
                .id(personalBookId)
                .user(user)
                .book(Book.builder().id(bookId).isbn("9781111111111").bookName("책").author("저자").publisher("출판").build())
                .readingStatus(BookReadingStatus.READING)
                .build();

        PersonalReadingRecord record = PersonalReadingRecord.builder()
                .id(recordId)
                .personalBook(personalBook)
                .user(user)
                .recordType(RecordType.MEMO)
                .recordContent("이전 내용")
                .build();

        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
        when(userValidator.findUserOrThrow(userId)).thenReturn(user);
        when(bookValidator.validatePersonalBook(userId, personalBookId)).thenReturn(personalBook);
        when(personalReadingRecordRepository.findByIdAndPersonalBook_IdAndUserId(recordId, personalBookId, userId))
                .thenReturn(Optional.of(record));

        // when
        PersonalReadingRecordCreateResponse response = personalReadingRecordService.update(personalBookId, recordId, request);

        // then
        assertThat(response.recordId()).isEqualTo(recordId);
        assertThat(response.recordContent()).isEqualTo(request.recordContent());
        assertThat(response.recordType()).isEqualTo(RecordType.QUOTE);
        assertThat(response.meta()).isNotNull();
        assertThat(response.meta().get("page")).isEqualTo("30");
        assertThat(response.meta().get("excerpt")).isEqualTo("수정된 인용문");

        assertThat(record.getRecordContent()).isEqualTo(request.recordContent());
        assertThat(record.getRecordType()).isEqualTo(RecordType.QUOTE);
        assertThat(record.getMeta().get("page")).isEqualTo("30");

        verify(personalReadingRecordRepository, times(1))
                .findByIdAndPersonalBook_IdAndUserId(recordId, personalBookId, userId);
        verify(personalReadingRecordRepository, never()).save(any());
        securityUtilMock.verify(SecurityUtil::getCurrentUserId, times(1));
        verify(userValidator, times(1)).findUserOrThrow(userId);
        verify(bookValidator, times(1)).validatePersonalBook(userId, personalBookId);
    }

    @Test
    @DisplayName("존재하지 않는 기록을 수정하려 하면 예외가 발생한다")
    void updateRecord_NotFound() {
        // given
        Long userId = 1L;
        Long personalBookId = 500L;
        Long bookId = 50L;
        Long recordId = 999L;

        PersonalReadingRecordUpdateRequest request = new PersonalReadingRecordUpdateRequest(
                RecordType.MEMO,
                "내용",
                null
        );

        User user = User.builder()
                .id(userId)
                .kakaoId(333L)
                .nickname("reader")
                .build();

        PersonalBook personalBook = PersonalBook.builder()
                .id(personalBookId)
                .user(user)
                .book(Book.builder().id(bookId).isbn("9782222222222").bookName("책").author("저자").publisher("출판").build())
                .readingStatus(BookReadingStatus.READING)
                .build();

        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
        when(userValidator.findUserOrThrow(userId)).thenReturn(user);
        when(bookValidator.validatePersonalBook(userId, personalBookId)).thenReturn(personalBook);
        when(personalReadingRecordRepository.findByIdAndPersonalBook_IdAndUserId(recordId, personalBookId, userId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> personalReadingRecordService.update(personalBookId, recordId, request))
                .isInstanceOf(RecordException.class)
                .hasFieldOrPropertyWithValue("errorCode", RecordErrorCode.RECORD_NOT_FOUND);

        verify(personalReadingRecordRepository, times(1))
                .findByIdAndPersonalBook_IdAndUserId(recordId, personalBookId, userId);
        verify(personalReadingRecordRepository, never()).save(any());
        securityUtilMock.verify(SecurityUtil::getCurrentUserId, times(1));
        verify(userValidator, times(1)).findUserOrThrow(userId);
        verify(bookValidator, times(1)).validatePersonalBook(userId, personalBookId);
    }

    @Test
    @DisplayName("인용 기록 수정 시 meta가 없으면 RecordException이 발생한다")
    void updateQuoteRecord_MissingMeta() {
        // given
        Long userId = 1L;
        Long personalBookId = 600L;
        Long bookId = 60L;
        Long recordId = 7L;

        PersonalReadingRecordUpdateRequest request = new PersonalReadingRecordUpdateRequest(
                RecordType.QUOTE,
                "인용 수정",
                null
        );

        User user = User.builder()
                .id(userId)
                .kakaoId(444L)
                .nickname("reader")
                .build();

        PersonalBook personalBook = PersonalBook.builder()
                .id(personalBookId)
                .user(user)
                .book(Book.builder().id(bookId).isbn("9783333333333").bookName("책").author("저자").publisher("출판").build())
                .readingStatus(BookReadingStatus.READING)
                .build();

        PersonalReadingRecord record = PersonalReadingRecord.builder()
                .id(recordId)
                .personalBook(personalBook)
                .user(user)
                .recordType(RecordType.QUOTE)
                .recordContent("기존 내용")
                .build();

        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
        when(userValidator.findUserOrThrow(userId)).thenReturn(user);
        when(bookValidator.validatePersonalBook(userId, personalBookId)).thenReturn(personalBook);
        when(personalReadingRecordRepository.findByIdAndPersonalBook_IdAndUserId(recordId, personalBookId, userId))
                .thenReturn(Optional.of(record));

        // when & then
        assertThatThrownBy(() -> personalReadingRecordService.update(personalBookId, recordId, request))
                .isInstanceOf(RecordException.class)
                .hasFieldOrPropertyWithValue("errorCode", RecordErrorCode.INVALID_RECORD_REQUEST);

        verify(personalReadingRecordRepository, times(1))
                .findByIdAndPersonalBook_IdAndUserId(recordId, personalBookId, userId);
        verify(personalReadingRecordRepository, never()).save(any());
        securityUtilMock.verify(SecurityUtil::getCurrentUserId, times(1));
        verify(userValidator, times(1)).findUserOrThrow(userId);
        verify(bookValidator, times(1)).validatePersonalBook(userId, personalBookId);
    }

    @Test
    @DisplayName("독서 기록을 삭제하면 deletedAt이 설정된다")
    void deleteRecord_Success() {
        // given
        Long userId = 1L;
        Long personalBookId = 700L;
        Long bookId = 70L;
        Long recordId = 8L;

        User user = User.builder()
                .id(userId)
                .kakaoId(555L)
                .nickname("deleter")
                .build();

        PersonalBook personalBook = PersonalBook.builder()
                .id(personalBookId)
                .user(user)
                .book(Book.builder().id(bookId).isbn("9784444444444").bookName("책").author("저자").publisher("출판").build())
                .readingStatus(BookReadingStatus.READING)
                .build();

        PersonalReadingRecord record = PersonalReadingRecord.builder()
                .id(recordId)
                .personalBook(personalBook)
                .user(user)
                .recordType(RecordType.MEMO)
                .recordContent("삭제할 기록")
                .build();

        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
        when(userValidator.findUserOrThrow(userId)).thenReturn(user);
        when(bookValidator.validatePersonalBook(userId, personalBookId)).thenReturn(personalBook);
        when(personalReadingRecordRepository.findByIdAndPersonalBook_IdAndUserId(recordId, personalBookId, userId))
                .thenReturn(Optional.of(record));

        // when
        personalReadingRecordService.delete(personalBookId, recordId);

        // then
        assertThat(record.isDeleted()).isTrue();
        assertThat(record.getDeletedAt()).isNotNull();

        verify(personalReadingRecordRepository, times(1))
                .findByIdAndPersonalBook_IdAndUserId(recordId, personalBookId, userId);
        verify(personalReadingRecordRepository, never()).save(any());
        securityUtilMock.verify(SecurityUtil::getCurrentUserId, times(1));
        verify(userValidator, times(1)).findUserOrThrow(userId);
        verify(bookValidator, times(1)).validatePersonalBook(userId, personalBookId);
    }

    @Test
    @DisplayName("삭제할 기록을 찾지 못하면 RecordException이 발생한다")
    void deleteRecord_NotFound() {
        // given
        Long userId = 1L;
        Long personalBookId = 800L;
        Long bookId = 80L;
        Long recordId = 99L;

        User user = User.builder()
                .id(userId)
                .kakaoId(666L)
                .nickname("deleter")
                .build();

        PersonalBook personalBook = PersonalBook.builder()
                .id(personalBookId)
                .user(user)
                .book(Book.builder().id(bookId).isbn("9785555555555").bookName("책").author("저자").publisher("출판").build())
                .readingStatus(BookReadingStatus.READING)
                .build();

        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
        when(userValidator.findUserOrThrow(userId)).thenReturn(user);
        when(bookValidator.validatePersonalBook(userId, personalBookId)).thenReturn(personalBook);
        when(personalReadingRecordRepository.findByIdAndPersonalBook_IdAndUserId(recordId, personalBookId, userId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> personalReadingRecordService.delete(personalBookId, recordId))
                .isInstanceOf(RecordException.class)
                .hasFieldOrPropertyWithValue("errorCode", RecordErrorCode.RECORD_NOT_FOUND);

        verify(personalReadingRecordRepository, times(1))
                .findByIdAndPersonalBook_IdAndUserId(recordId, personalBookId, userId);
        securityUtilMock.verify(SecurityUtil::getCurrentUserId, times(1));
        verify(userValidator, times(1)).findUserOrThrow(userId);
        verify(bookValidator, times(1)).validatePersonalBook(userId, personalBookId);
    }

    @Test
    @DisplayName("이미 삭제된 기록 삭제 시 RecordException이 발생한다")
    void deleteRecord_AlreadyDeleted() {
        // given
        Long userId = 1L;
        Long personalBookId = 900L;
        Long bookId = 90L;
        Long recordId = 77L;

        User user = User.builder()
                .id(userId)
                .kakaoId(777L)
                .nickname("deleter")
                .build();

        PersonalBook personalBook = PersonalBook.builder()
                .id(personalBookId)
                .user(user)
                .book(Book.builder().id(bookId).isbn("9786666666666").bookName("책").author("저자").publisher("출판").build())
                .readingStatus(BookReadingStatus.READING)
                .build();

        PersonalReadingRecord record = PersonalReadingRecord.builder()
                .id(recordId)
                .personalBook(personalBook)
                .user(user)
                .recordType(RecordType.QUOTE)
                .recordContent("삭제된 기록")
                .build();

        record.delete();

        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
        when(userValidator.findUserOrThrow(userId)).thenReturn(user);
        when(bookValidator.validatePersonalBook(userId, personalBookId)).thenReturn(personalBook);
        when(personalReadingRecordRepository.findByIdAndPersonalBook_IdAndUserId(recordId, personalBookId, userId))
                .thenReturn(Optional.of(record));

        // when & then
        assertThatThrownBy(() -> personalReadingRecordService.delete(personalBookId, recordId))
                .isInstanceOf(RecordException.class)
                .hasFieldOrPropertyWithValue("errorCode", RecordErrorCode.RECORD_ALREADY_DELETED);

        verify(personalReadingRecordRepository, times(1))
                .findByIdAndPersonalBook_IdAndUserId(recordId, personalBookId, userId);
        securityUtilMock.verify(SecurityUtil::getCurrentUserId, times(1));
        verify(userValidator, times(1)).findUserOrThrow(userId);
        verify(bookValidator, times(1)).validatePersonalBook(userId, personalBookId);
    }

    @Nested
    @DisplayName("GET /api/book/{personalBookId}/gatherings - 책에 연결된 모임 목록 조회")
    class GetGatheringsForBookTest {

        private final Long userId = 1L;
        private final Long personalBookId = 10L;
        private final Long bookId = 100L;
        private User user;
        private PersonalBook personalBook;

        @BeforeEach
        void setUp() {
            user = User.builder().id(userId).kakaoId(1L).nickname("tester").build();
            Book book = Book.builder().id(bookId).isbn("9780000000001").bookName("책").author("저자").publisher("출판사").build();
            personalBook = PersonalBook.builder()
                    .id(personalBookId)
                    .user(user)
                    .book(book)
                    .readingStatus(BookReadingStatus.READING)
                    .build();
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
        }

        @Test
        @DisplayName("모임이 존재하면 gatheringId와 gatheringName을 반환한다")
        void getGatheringsForBook_ReturnsList() {
            // given
            PersonalBookGatheringProjection p1 = mockProjection(1L, "독서모임A");
            PersonalBookGatheringProjection p2 = mockProjection(2L, "독서모임B");

            when(bookValidator.validatePersonalBook(userId, personalBookId)).thenReturn(personalBook);
            when(personalBookRepository.findActiveGatheringsWithMeetingsByUserAndBook(userId, personalBookId))
                    .thenReturn(List.of(p1, p2));

            // when
            List<PersonalBookGatheringResponse> result = personalReadingRecordService.getGatheringsForBook(personalBookId);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).gatheringId()).isEqualTo(1L);
            assertThat(result.get(0).gatheringName()).isEqualTo("독서모임A");
            assertThat(result.get(1).gatheringId()).isEqualTo(2L);
            assertThat(result.get(1).gatheringName()).isEqualTo("독서모임B");
        }

        @Test
        @DisplayName("연결된 모임이 없으면 빈 리스트를 반환한다")
        void getGatheringsForBook_EmptyList() {
            // given
            when(bookValidator.validatePersonalBook(userId, personalBookId)).thenReturn(personalBook);
            when(personalBookRepository.findActiveGatheringsWithMeetingsByUserAndBook(userId, personalBookId))
                    .thenReturn(List.of());

            // when
            List<PersonalBookGatheringResponse> result = personalReadingRecordService.getGatheringsForBook(personalBookId);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("책장에 없는 책이면 BookException이 발생한다")
        void getGatheringsForBook_BookNotInShelf_ThrowsException() {
            // given
            when(bookValidator.validatePersonalBook(userId, personalBookId))
                    .thenThrow(new BookException(BookErrorCode.BOOK_NOT_IN_SHELF));

            // when & then
            assertThatThrownBy(() -> personalReadingRecordService.getGatheringsForBook(personalBookId))
                    .isInstanceOf(BookException.class)
                    .hasFieldOrPropertyWithValue("errorCode", BookErrorCode.BOOK_NOT_IN_SHELF);

            verify(personalBookRepository, never()).findActiveGatheringsWithMeetingsByUserAndBook(any(), any());
        }

        private PersonalBookGatheringProjection mockProjection(Long gatheringId, String gatheringName) {
            PersonalBookGatheringProjection projection = mock(PersonalBookGatheringProjection.class);
            when(projection.getGatheringId()).thenReturn(gatheringId);
            when(projection.getGatheringName()).thenReturn(gatheringName);
            return projection;
        }
    }

    @Nested
    @DisplayName("GET /api/book/{personalBookId}/records - 독서 기록 목록 조회")
    class GetRecordsTest {

        private User user;
        private PersonalBook personalBook;
        private final Long userId = 1L;
        private final Long personalBookId = 10L;
        private final Long bookId = 100L;

        @BeforeEach
        void setUpGetRecords() {
            user = User.builder().id(userId).kakaoId(1000L).nickname("reader").build();
            personalBook = PersonalBook.builder()
                    .id(personalBookId)
                    .user(user)
                    .book(Book.builder().id(bookId).isbn("9780000000001").bookName("책").author("저자").publisher("출판사").build())
                    .readingStatus(BookReadingStatus.READING)
                    .build();
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
            when(userValidator.findUserOrThrow(userId)).thenReturn(user);
            when(bookValidator.validatePersonalBook(userId, personalBookId)).thenReturn(personalBook);
        }

        private PersonalReadingRecord makeRecord(Long id, RecordType type, LocalDateTime createdAt) {
            return PersonalReadingRecord.builder()
                    .id(id)
                    .personalBook(personalBook)
                    .user(user)
                    .recordType(type)
                    .recordContent("내용 " + id)
                    .createdAt(createdAt)
                    .build();
        }

        @Test
        @DisplayName("필터 없이 전체 조회 - 기본 정렬 DESC로 조회된다")
        void getRecords_NoFilter_DefaultDesc() {
            PersonalReadingRecord r1 = makeRecord(1L, RecordType.MEMO, LocalDateTime.now());
            PersonalReadingRecord r2 = makeRecord(2L, RecordType.QUOTE, LocalDateTime.now().minusDays(1));

            when(personalReadingRecordRepository.findRecords(
                    eq(personalBookId), eq(userId), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(r1, r2)));
            when(personalReadingRecordRepository.countRecords(
                    eq(personalBookId), eq(userId), isNull(), isNull()))
                    .thenReturn(2L);

            CursorPageResponse<PersonalReadingRecordListResponse, ReadingRecordCursor> result =
                    personalReadingRecordService.getRecords(personalBookId, null, null, null, null, null, Sort.Direction.DESC);

            assertThat(result.getItems()).hasSize(2);
            assertThat(result.isHasNext()).isFalse();
            assertThat(result.getNextCursor()).isNull();
            assertThat(result.getTotalCount()).isEqualTo(2L);

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(personalReadingRecordRepository).findRecords(
                    eq(personalBookId), eq(userId), isNull(), isNull(), pageableCaptor.capture());
            Sort.Order order = pageableCaptor.getValue().getSort().getOrderFor("createdAt");
            assertThat(order).isNotNull();
            assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
        }

        @Test
        @DisplayName("gatheringId 필터 - 해당 gatheringId가 repository에 전달된다")
        void getRecords_GatheringIdFilter() {
            Long gatheringId = 3L;
            PersonalReadingRecord r1 = makeRecord(1L, RecordType.MEMO, LocalDateTime.now());

            when(personalReadingRecordRepository.findRecords(
                    eq(personalBookId), eq(userId), eq(gatheringId), isNull(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(r1)));
            when(personalReadingRecordRepository.countRecords(
                    eq(personalBookId), eq(userId), eq(gatheringId), isNull()))
                    .thenReturn(1L);

            CursorPageResponse<PersonalReadingRecordListResponse, ReadingRecordCursor> result =
                    personalReadingRecordService.getRecords(personalBookId, gatheringId, null, null, null, null, Sort.Direction.DESC);

            assertThat(result.getItems()).hasSize(1);
            verify(personalReadingRecordRepository).findRecords(
                    eq(personalBookId), eq(userId), eq(gatheringId), isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("recordType=MEMO 필터 - MEMO 타입이 repository에 전달된다")
        void getRecords_RecordTypeFilter_Memo() {
            PersonalReadingRecord r1 = makeRecord(1L, RecordType.MEMO, LocalDateTime.now());

            when(personalReadingRecordRepository.findRecords(
                    eq(personalBookId), eq(userId), isNull(), eq(RecordType.MEMO), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(r1)));
            when(personalReadingRecordRepository.countRecords(
                    eq(personalBookId), eq(userId), isNull(), eq(RecordType.MEMO)))
                    .thenReturn(1L);

            CursorPageResponse<PersonalReadingRecordListResponse, ReadingRecordCursor> result =
                    personalReadingRecordService.getRecords(personalBookId, null, RecordType.MEMO, null, null, null, Sort.Direction.DESC);

            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).recordType()).isEqualTo(RecordType.MEMO);
            verify(personalReadingRecordRepository).findRecords(
                    eq(personalBookId), eq(userId), isNull(), eq(RecordType.MEMO), any(Pageable.class));
        }

        @Test
        @DisplayName("gatheringId + recordType=QUOTE + sort=ASC 복합 필터 - findRecords에 모두 전달된다")
        void getRecords_Combined_GatheringId_RecordType_SortAsc() {
            Long gatheringId = 3L;
            PersonalReadingRecord r1 = makeRecord(1L, RecordType.QUOTE, LocalDateTime.now());

            when(personalReadingRecordRepository.findRecords(
                    eq(personalBookId), eq(userId), eq(gatheringId), eq(RecordType.QUOTE), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(r1)));
            when(personalReadingRecordRepository.countRecords(
                    eq(personalBookId), eq(userId), eq(gatheringId), eq(RecordType.QUOTE)))
                    .thenReturn(1L);

            CursorPageResponse<PersonalReadingRecordListResponse, ReadingRecordCursor> result =
                    personalReadingRecordService.getRecords(personalBookId, gatheringId, RecordType.QUOTE, null, null, null, Sort.Direction.ASC);

            assertThat(result.getItems()).hasSize(1);

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(personalReadingRecordRepository).findRecords(
                    eq(personalBookId), eq(userId), eq(gatheringId), eq(RecordType.QUOTE), pageableCaptor.capture());
            Sort.Order order = pageableCaptor.getValue().getSort().getOrderFor("createdAt");
            assertThat(order).isNotNull();
            assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
        }

        @Test
        @DisplayName("hasNext=true일 때 nextCursor가 생성된다")
        void getRecords_HasNextTrue_NextCursorCreated() {
            int pageSize = 2;
            List<PersonalReadingRecord> records = new ArrayList<>();
            for (long i = 1; i <= pageSize + 1; i++) {
                records.add(makeRecord(i, RecordType.MEMO, LocalDateTime.now().minusHours(i)));
            }

            when(personalReadingRecordRepository.findRecords(
                    eq(personalBookId), eq(userId), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(records));
            when(personalReadingRecordRepository.countRecords(
                    eq(personalBookId), eq(userId), isNull(), isNull()))
                    .thenReturn(5L);

            CursorPageResponse<PersonalReadingRecordListResponse, ReadingRecordCursor> result =
                    personalReadingRecordService.getRecords(personalBookId, null, null, null, null, pageSize, Sort.Direction.DESC);

            assertThat(result.isHasNext()).isTrue();
            assertThat(result.getNextCursor()).isNotNull();
            assertThat(result.getItems()).hasSize(pageSize);
        }

        @Test
        @DisplayName("cursor 전달 시 DESC - findRecordsByCursor를 호출한다")
        void getRecords_WithCursor_Desc_CallsCursorRepository() {
            LocalDateTime cursorAt = LocalDateTime.of(2026, 1, 22, 10, 25, 40);
            OffsetDateTime cursorCreatedAt = OffsetDateTime.of(cursorAt, ZoneOffset.UTC);
            Long cursorRecordId = 5L;

            PersonalReadingRecord r1 = makeRecord(3L, RecordType.MEMO, cursorAt.minusHours(1));

            when(personalReadingRecordRepository.findRecordsByCursor(
                    eq(personalBookId), eq(userId), isNull(), isNull(),
                    eq(cursorAt), eq(cursorRecordId), any(Pageable.class)))
                    .thenReturn(List.of(r1));
            when(personalReadingRecordRepository.countRecords(
                    eq(personalBookId), eq(userId), isNull(), isNull()))
                    .thenReturn(1L);

            CursorPageResponse<PersonalReadingRecordListResponse, ReadingRecordCursor> result =
                    personalReadingRecordService.getRecords(
                            personalBookId, null, null, cursorCreatedAt, cursorRecordId, null, Sort.Direction.DESC);

            assertThat(result.getItems()).hasSize(1);
            verify(personalReadingRecordRepository).findRecordsByCursor(
                    eq(personalBookId), eq(userId), isNull(), isNull(),
                    eq(cursorAt), eq(cursorRecordId), any(Pageable.class));
            verify(personalReadingRecordRepository, never()).findRecordsByCursorAsc(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("cursor 전달 시 ASC - findRecordsByCursorAsc를 호출한다")
        void getRecords_WithCursor_Asc_CallsCursorAscRepository() {
            LocalDateTime cursorAt = LocalDateTime.of(2026, 1, 22, 10, 25, 40);
            OffsetDateTime cursorCreatedAt = OffsetDateTime.of(cursorAt, ZoneOffset.UTC);
            Long cursorRecordId = 5L;

            PersonalReadingRecord r1 = makeRecord(7L, RecordType.QUOTE, cursorAt.plusHours(1));

            when(personalReadingRecordRepository.findRecordsByCursorAsc(
                    eq(personalBookId), eq(userId), isNull(), isNull(),
                    eq(cursorAt), eq(cursorRecordId), any(Pageable.class)))
                    .thenReturn(List.of(r1));
            when(personalReadingRecordRepository.countRecords(
                    eq(personalBookId), eq(userId), isNull(), isNull()))
                    .thenReturn(1L);

            CursorPageResponse<PersonalReadingRecordListResponse, ReadingRecordCursor> result =
                    personalReadingRecordService.getRecords(
                            personalBookId, null, null, cursorCreatedAt, cursorRecordId, null, Sort.Direction.ASC);

            assertThat(result.getItems()).hasSize(1);
            verify(personalReadingRecordRepository).findRecordsByCursorAsc(
                    eq(personalBookId), eq(userId), isNull(), isNull(),
                    eq(cursorAt), eq(cursorRecordId), any(Pageable.class));
            verify(personalReadingRecordRepository, never()).findRecordsByCursor(any(), any(), any(), any(), any(), any(), any());
        }
    }

}
