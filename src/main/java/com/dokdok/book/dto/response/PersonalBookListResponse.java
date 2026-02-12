package com.dokdok.book.dto.response;

import com.dokdok.book.entity.BookReadingStatus;
import com.dokdok.book.entity.PersonalBook;
import com.dokdok.book.repository.PersonalBookListProjection;
import lombok.Builder;

@Builder
public record PersonalBookListResponse(
        Long bookId,
        String title,
        String publisher,
        String authors,
        BookReadingStatus bookReadingStatus,
        String thumbnail,
        String gatheringName
) {
    public static PersonalBookListResponse from(PersonalBook entity) {
        return PersonalBookListResponse.builder()
                .bookId(entity.getBook().getId())
                .title(entity.getBook().getBookName())
                .publisher(entity.getBook().getPublisher())
                .authors(entity.getBook().getAuthor())
                .bookReadingStatus(entity.getReadingStatus())
                .thumbnail(entity.getBook().getThumbnail())
                .gatheringName(entity.getGathering() != null ? entity.getGathering().getGatheringName() : null)
                .build();
    }

    public static PersonalBookListResponse from(PersonalBookListProjection projection) {
        return PersonalBookListResponse.builder()
                .bookId(projection.getBookId())
                .title(projection.getTitle())
                .publisher(projection.getPublisher())
                .authors(projection.getAuthors())
                .bookReadingStatus(projection.getBookReadingStatus())
                .thumbnail(projection.getThumbnail())
                .gatheringName(projection.getGatheringName())
                .build();
    }

}
