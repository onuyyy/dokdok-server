package com.dokdok.book.service;

import com.dokdok.book.dto.response.BookDetailResponse;
import com.dokdok.book.dto.response.BookSearchCursor;
import com.dokdok.book.dto.response.CursorPageResponse;
import com.dokdok.book.dto.response.KakaoBookResponse;
import com.dokdok.book.entity.Book;
import com.dokdok.book.exception.BookException;
import com.dokdok.book.repository.BookRepository;
import com.dokdok.book.webClient.KakaoBookClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.dokdok.book.exception.BookErrorCode.BOOK_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class BookService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int DEFAULT_PAGE = 1;

    private final KakaoBookClient kakaoBookClient;
    private final BookRepository bookRepository;

    // 외부 API로 책 검색 후 가지고오기 or 저장하기
    public CursorPageResponse<KakaoBookResponse.Document, BookSearchCursor> searchBook(
            String query,
            Integer cursorPage,
            Integer size
    ) {
        int pageSize = resolvePageSize(size);
        int page = resolvePage(cursorPage);
        KakaoBookResponse response = kakaoBookClient.searchBooks(query, page, pageSize);
        List<KakaoBookResponse.Document> items = response != null && response.documents() != null
                ? response.documents()
                : List.of();
        boolean hasNext = response != null && response.meta() != null && !response.meta().isEnd();
        BookSearchCursor nextCursor = hasNext ? new BookSearchCursor(page + 1) : null;
        long totalCount = response != null && response.meta() != null ? response.meta().totalCount() : 0L;

        return CursorPageResponse.of(items, pageSize, hasNext, nextCursor, totalCount);
    }

    public BookDetailResponse getBook(String isbn) {
        Book entity = bookRepository.findByIsbn(isbn)
                .orElseThrow(() -> new BookException(BOOK_NOT_FOUND));
        return BookDetailResponse.from(entity);
    }

    private int resolvePageSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return size;
    }

    private int resolvePage(Integer page) {
        if (page == null || page < 1) {
            return DEFAULT_PAGE;
        }
        return page;
    }

}
