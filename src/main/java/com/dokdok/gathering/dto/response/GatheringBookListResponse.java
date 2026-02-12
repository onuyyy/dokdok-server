package com.dokdok.gathering.dto.response;

import com.dokdok.book.entity.Book;
import com.dokdok.gathering.entity.GatheringBook;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "모임 책장 책 정보")
public record GatheringBookListResponse(
        @Schema(description = "책 ID", example = "1")
        Long bookId,

        @Schema(description = "책 제목", example = "클린 코드")
        String bookName,

        @Schema(description = "저자", example = "Robert C. Martin")
        String author,

        @Schema(description = "책 표지 이미지 URL", example = "https://example.com/books/clean-code.jpg")
        String thumbnail,

        @Schema(description = "모임 멤버들의 평균 평점 (평점이 없으면 null)", example = "4.25", nullable = true)
        Double ratingAverage
) {

    public static GatheringBookListResponse from(
            GatheringBook gatheringBook,
            Double ratingAverage
    ) {
        Book book = gatheringBook.getBook();
        return new GatheringBookListResponse(
                book.getId(),
                book.getBookName(),
                book.getAuthor(),
                book.getThumbnail(),
                ratingAverage
        );
    }
}
