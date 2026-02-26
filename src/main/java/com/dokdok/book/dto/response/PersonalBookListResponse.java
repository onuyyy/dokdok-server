package com.dokdok.book.dto.response;

import com.dokdok.book.entity.BookReadingStatus;
import com.dokdok.book.entity.PersonalBook;
import com.dokdok.book.repository.PersonalBookListProjection;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record PersonalBookListResponse(
        Long bookId,
        String title,
        String publisher,
        String authors,
        BookReadingStatus bookReadingStatus,
        String thumbnail,
        BigDecimal rating,
        List<PersonalBookGatheringResponse> gatherings
) {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static PersonalBookListResponse from(PersonalBook entity) {
        return PersonalBookListResponse.builder()
                .bookId(entity.getBook().getId())
                .title(entity.getBook().getBookName())
                .publisher(entity.getBook().getPublisher())
                .authors(entity.getBook().getAuthor())
                .bookReadingStatus(entity.getReadingStatus())
                .thumbnail(entity.getBook().getThumbnail())
                .rating(null)
                .gatherings(entity.getGathering() == null
                        ? List.of()
                        : List.of(new PersonalBookGatheringResponse(
                        entity.getGathering().getId(),
                        entity.getGathering().getGatheringName()
                )))
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
                .rating(projection.getRating())
                .gatherings(parseGatherings(projection.getGatherings()))
                .build();
    }

    private static List<PersonalBookGatheringResponse> parseGatherings(String gatheringsJson) {
        if (gatheringsJson == null || gatheringsJson.isBlank()) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(gatheringsJson, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException("모임 정보 파싱에 실패했습니다.", e);
        }
    }

}
