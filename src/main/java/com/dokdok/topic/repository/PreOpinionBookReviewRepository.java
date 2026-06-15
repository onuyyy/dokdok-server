package com.dokdok.topic.repository;

import com.dokdok.topic.entity.PreOpinionBookReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PreOpinionBookReviewRepository extends JpaRepository<PreOpinionBookReview, Long> {

    @Query("""
            SELECT DISTINCT review
            FROM PreOpinionBookReview review
            LEFT JOIN FETCH review.keywords reviewKeyword
            LEFT JOIN FETCH reviewKeyword.keyword
            JOIN FETCH review.book
            JOIN FETCH review.user
            WHERE review.meeting.id = :meetingId
            AND review.user.id = :userId
            """)
    Optional<PreOpinionBookReview> findByMeetingIdAndUserId(
            @Param("meetingId") Long meetingId,
            @Param("userId") Long userId
    );

    @Query("""
            SELECT DISTINCT review
            FROM PreOpinionBookReview review
            LEFT JOIN FETCH review.keywords reviewKeyword
            LEFT JOIN FETCH reviewKeyword.keyword
            JOIN FETCH review.book
            JOIN FETCH review.user
            WHERE review.meeting.id = :meetingId
            AND review.user.id IN :userIds
            """)
    List<PreOpinionBookReview> findByMeetingIdAndUserIdIn(
            @Param("meetingId") Long meetingId,
            @Param("userIds") List<Long> userIds
    );

    @Query("""
            SELECT DISTINCT review
            FROM PreOpinionBookReview review
            LEFT JOIN FETCH review.keywords reviewKeyword
            LEFT JOIN FETCH reviewKeyword.keyword
            JOIN FETCH review.book
            JOIN FETCH review.user
            WHERE review.meeting.id = :meetingId
            """)
    List<PreOpinionBookReview> findByMeetingId(@Param("meetingId") Long meetingId);
}
