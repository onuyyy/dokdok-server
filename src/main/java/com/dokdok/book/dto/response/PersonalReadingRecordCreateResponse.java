package com.dokdok.book.dto.response;

import com.dokdok.book.entity.PersonalReadingRecord;
import com.dokdok.book.entity.RecordType;
import lombok.Builder;

import java.util.Map;

@Builder
public record PersonalReadingRecordCreateResponse(
        Long recordId,
        RecordType recordType,
        String recordContent,
        Map<String, Object> meta,
        Long bookId
) {

    public static PersonalReadingRecordCreateResponse from(PersonalReadingRecord personalReadingRecordEntity) {
        return PersonalReadingRecordCreateResponse.builder()
                .recordId(personalReadingRecordEntity.getId())
                .recordType(personalReadingRecordEntity.getRecordType())
                .recordContent(personalReadingRecordEntity.getRecordContent())
                .meta(personalReadingRecordEntity.getMeta())
                .bookId(personalReadingRecordEntity.getPersonalBook().getBook().getId())
                .build();
    }
}
