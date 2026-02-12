package com.dokdok.book.service;

import com.dokdok.book.dto.request.PersonalReadingRecordCreateRequest;
import com.dokdok.book.dto.request.PersonalReadingRecordUpdateRequest;
import com.dokdok.book.dto.response.PersonalReadingRecordCreateResponse;
import com.dokdok.book.entity.Book;
import com.dokdok.book.entity.BookReadingStatus;
import com.dokdok.book.entity.PersonalBook;
import com.dokdok.book.entity.PersonalReadingRecord;
import com.dokdok.book.entity.RecordType;
import com.dokdok.book.exception.RecordErrorCode;
import com.dokdok.book.exception.RecordException;
import com.dokdok.book.repository.PersonalReadingRecordRepository;
import com.dokdok.global.util.SecurityUtil;
import com.dokdok.user.entity.User;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PersonalReadingRecordService 테스트")
class PersonalReadingRecordServiceTest {

    @InjectMocks
    private PersonalReadingRecordService personalReadingRecordService;

    @Mock
    private PersonalReadingRecordRepository personalReadingRecordRepository;

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

}
