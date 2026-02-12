package com.dokdok.book.repository;

import com.dokdok.book.entity.BookReadingStatus;

public interface PersonalBookListProjection {
    Long getBookId();
    String getTitle();
    String getPublisher();
    String getAuthors();
    BookReadingStatus getBookReadingStatus();
    String getThumbnail();
    String getGatheringName();
    java.time.LocalDateTime getAddedAt();
}
