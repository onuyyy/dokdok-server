package com.dokdok.book.repository;

import com.dokdok.book.entity.BookReadingStatus;

import java.math.BigDecimal;

public interface PersonalBookListProjection {
    Long getBookId();
    String getTitle();
    String getPublisher();
    String getAuthors();
    BookReadingStatus getBookReadingStatus();
    String getThumbnail();
    BigDecimal getRating();
    String getGatherings();
    java.time.LocalDateTime getAddedAt();
}
