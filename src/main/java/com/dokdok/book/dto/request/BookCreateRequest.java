package com.dokdok.book.dto.request;

import com.dokdok.book.entity.Book;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record BookCreateRequest(
    @NotBlank(message = "제목은 필수입니다.") String title,
    @NotBlank(message = "저자는 필수입니다.") String authors,
    @NotBlank(message = "출판사는 필수입니다.") String publisher,
    @NotBlank(message = "ISBN은 필수입니다.") String isbn,
    String thumbnail
) {
    public Book of() {
        return Book.builder()
                .bookName(title)
                .author(authors)
                .publisher(publisher)
                .isbn(isbn)
                .thumbnail(thumbnail)
                .build();
    }

    public static BookCreateRequest from(Book book) {
        return BookCreateRequest.builder()
                .title(book.getBookName())
                .authors(book.getAuthor())
                .publisher(book.getPublisher())
                .isbn(book.getIsbn())
                .thumbnail(book.getThumbnail())
                .build();
    }
}
