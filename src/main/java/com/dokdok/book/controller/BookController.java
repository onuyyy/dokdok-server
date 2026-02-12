package com.dokdok.book.controller;

import com.dokdok.book.api.BookApi;
import com.dokdok.book.dto.request.BookCreateRequest;
import com.dokdok.book.dto.response.*;
import com.dokdok.book.entity.BookReadingStatus;
import com.dokdok.book.service.BookService;
import com.dokdok.book.service.PersonalBookService;
import com.dokdok.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/book")
public class BookController implements BookApi {

    private final BookService bookService;
    private final PersonalBookService personalBookService;

    @Override
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<CursorPageResponse<KakaoBookResponse.Document, BookSearchCursor>>> searchBook(
            @RequestParam String query,
            @RequestParam(required = false) Integer cursorPage,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.success(bookService.searchBook(query, cursorPage, size), "책 정보 조회 성공");
    }

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<PersonalBookCreateResponse>> createBook(@Valid @RequestBody BookCreateRequest bookCreateRequest) {
        PersonalBookCreateResponse book = personalBookService.createBook(bookCreateRequest);
        return ApiResponse.created(book, "내 책장에 책 등록 성공");
    }

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<CursorPageResponse<PersonalBookListResponse, BookListCursor>>> getMyBooks(
            BookReadingStatus readingStatus,
            Long gatheringId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime cursorAddedAt,
            @RequestParam(required = false) Long cursorBookId,
            @RequestParam(required = false) Integer size
    ) {
        CursorPageResponse<PersonalBookListResponse, BookListCursor> response = personalBookService
                .getPersonalBookListCursor(readingStatus, gatheringId, cursorAddedAt, cursorBookId, size);
        return ApiResponse.success(response, "책 리스트 조회 성공");
    }

    @Override
    @GetMapping("/{bookId}")
    public ResponseEntity<ApiResponse<PersonalBookDetailResponse>> getMyBook(@PathVariable Long bookId) {
        PersonalBookDetailResponse personalBook = personalBookService.getPersonalBook(bookId);
        return ApiResponse.success(personalBook, "책 상세 정보 조회 성공");
    }

    @Override
    @DeleteMapping("/{bookId}")
    public ResponseEntity<ApiResponse<Void>> deleteMyBook(@PathVariable Long bookId) {
        personalBookService.deleteBook(bookId);
        return ApiResponse.deleted("책 삭제 성공");
    }


    @Override
    @GetMapping("/reading")
    public ResponseEntity<ApiResponse<PageResponse<PersonalBookListResponse>>> getMyReadingBooks(Pageable pageable) {
        Page<PersonalBookListResponse> personalBookList = personalBookService.getPersonalBookList(BookReadingStatus.READING, null, pageable);
        PageResponse<PersonalBookListResponse> response = PageResponse.from(personalBookList);
        return ApiResponse.success(response, "읽고 있는 책 리스트 조회 성공");
    }

    @Override
    public ResponseEntity<ApiResponse<PersonalBookDetailResponse>> updateReadingBook(Long bookId, Long personalBookId) {
        PersonalBookDetailResponse personalBook = personalBookService.updateReadingStatus(personalBookId);
        return ApiResponse.success(personalBook, "읽는 상태 업데이트 성공");
    }
}
