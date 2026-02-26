package com.dokdok.book.repository;

public interface PersonalBookStatusCountProjection {
    String getReadingStatus();
    long getCount();
}
