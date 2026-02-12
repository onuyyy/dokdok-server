package com.dokdok.book.repository;

import com.dokdok.book.entity.BookReviewKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookReviewKeywordRepository extends JpaRepository<BookReviewKeyword, Long> {

    @Query("""
                SELECT brk
                FROM BookReviewKeyword brk
                JOIN FETCH brk.keyword k
                WHERE brk.bookReview.id IN :bookReviewIds
            """)
    List<BookReviewKeyword> findByBookReviewIds(List<Long> bookReviewIds);
}
