package com.dokdok.book.repository;

import com.dokdok.book.entity.BookReview;
import com.dokdok.gathering.dto.response.BookRatingAverage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookReviewRepository extends JpaRepository<BookReview, Long> {
    Optional<BookReview> findByBookIdAndUserId(Long bookId, Long userId);

    boolean existsByBookIdAndUserId(Long bookId, Long userId);

    @Query("""
            SELECT br
            FROM BookReview br
            WHERE br.user.id IN :userIds
            AND br.createdAt = (
                    SELECT MAX(br2.createdAt)
                    FROM BookReview br2
                    WHERE br2.user.id = br.user.id
                )
            AND EXISTS (SELECT ta
                        FROM TopicAnswer ta
                        JOIN ta.topic t
                        WHERE ta.user.id = br.user.id
                        AND t.meeting.id = :meetingId
                        AND ta.isSubmitted = true)
            """)
    List<BookReview> findByUserIdIn(
            @Param("userIds") List<Long> userIds,
            @Param("meetingId") Long meetingId
    );

    @Query("""
        SELECT new com.dokdok.gathering.dto.response.BookRatingAverage(
                b.id,
                AVG(br.rating)
            )
        FROM BookReview br
        JOIN br.book b
        WHERE br.user.id IN :meetingMemberIds
        AND b.id IN :bookIds
        GROUP BY br.book
    """)
    List<BookRatingAverage> findMeetingBookReviews(List<Long> bookIds, List<Long> meetingMemberIds);

}