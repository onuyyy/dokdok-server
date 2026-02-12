package com.dokdok.topic.repository;

import com.dokdok.topic.entity.TopicAnswer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TopicAnswerRepository extends JpaRepository<TopicAnswer, Long> {
    Optional<TopicAnswer> findByTopicIdAndUserId(Long topicId, Long userId);

    @Query("""
            SELECT ta
            FROM TopicAnswer ta
            WHERE ta.topic.id = :topicId
            AND ta.user.id = :userId
            """)
    TopicAnswer findPreOpinion(Long topicId, Long userId);

    boolean existsByTopicIdAndUserId(Long topicId, Long userId);

    @Query("""
                SELECT CASE WHEN COUNT(ta) > 0 THEN true ELSE false END
                FROM TopicAnswer ta
                JOIN ta.topic t
                JOIN t.meeting m
                WHERE m.id = :meetingId
                AND ta.user.id = :userId
                AND ta.isSubmitted = true
            """)
    boolean existsByMeetingIdAndUserId(@Param("meetingId") Long meetingId,
                                       @Param("userId") Long userId);


    @Query("""
                    SELECT ta
                    FROM TopicAnswer ta
                    JOIN FETCH ta.topic t
                    WHERE t.meeting.id = :meetingId
                    AND ta.user.id = :userId
                    ORDER BY t.id
            """)
    List<TopicAnswer> findByMeetingIdUserId(Long meetingId, Long userId);

    @Query("""
                    SELECT ta
                    FROM TopicAnswer ta
                    JOIN FETCH ta.topic t
                    JOIN FETCH t.meeting m
                    WHERE m.id IN :meetingIds
                    AND ta.user.id = :userId
                    ORDER BY m.id, t.id
            """)
    List<TopicAnswer> findByMeetingIdsUserId(
            @Param("meetingIds") List<Long> meetingIds,
            @Param("userId") Long userId
    );

    @Query("""
            SELECT ta
            FROM TopicAnswer ta
            JOIN FETCH ta.user u
            WHERE ta.topic.id = :topicId
            AND ta.isSubmitted = true
            """)
    List<TopicAnswer> findSubmittedByTopicId(@Param("topicId") Long topicId);

    @Modifying
    @Query("""
            UPDATE TopicAnswer ta
            SET ta.deletedAt = CURRENT_TIMESTAMP
            WHERE ta.topic.id IN (
                SELECT t.id
                FROM Topic t
                WHERE t.meeting.id = :meetingId
            )
            AND ta.deletedAt IS NULL
            """)
    void softDeleteByMeetingId(@Param("meetingId") Long meetingId);

    @Query("""
                    SELECT ta
                    FROM TopicAnswer ta
                    LEFT JOIN FETCH ta.user u
                    JOIN FETCH ta.topic t
                    WHERE t.meeting.id = :meetingId
                    AND ta.isSubmitted = true
                    ORDER BY ta.createdAt DESC
            """
    )
    List<TopicAnswer> findByMeetingId(Long meetingId);

    @Query("""
        SELECT ta
        FROM TopicAnswer ta
        JOIN FETCH ta.topic t
        WHERE ta.user.id = :userId
            AND t.meeting.id = :meetingId
    """)
    List<TopicAnswer> findByTopicAnswers(
            @Param("meetingId") Long meetingId,
            @Param("userId") Long userId
    );

    // === 수집된 사전 의견 조회용 쿼리 ===

    /**
     * 첫 페이지: 제출된 답변이 있는 userId 목록 조회 (distinct, 오름차순)
     */
    @Query("""                                                                                                                                                                       
          SELECT DISTINCT u.id
          FROM TopicAnswer ta
          JOIN ta.user u
          JOIN ta.topic t
          WHERE t.meeting.id = :meetingId
          AND ta.isSubmitted = true
          ORDER BY u.id ASC
          """)
    List<Long> findDistinctUserIdsByMeetingIdFirstPage(
            @Param("meetingId") Long meetingId,
            Pageable pageable
    );

    /**
     * 다음 페이지: 커서 이후 userId 목록 조회
     */
    @Query("""                                                                                                                                                                       
          SELECT DISTINCT u.id
          FROM TopicAnswer ta
          JOIN ta.user u
          JOIN ta.topic t
          WHERE t.meeting.id = :meetingId
          AND ta.isSubmitted = true
          AND u.id > :cursorUserId
          ORDER BY u.id ASC
          """)
    List<Long> findDistinctUserIdsByMeetingIdAfterCursor(
            @Param("meetingId") Long meetingId,
            @Param("cursorUserId") Long cursorUserId,
            Pageable pageable
    );

    /**
     * userId 목록으로 제출된 답변 조회 (user, topic fetch)
     */
    @Query("""                                                                                                                                                                       
          SELECT ta
          FROM TopicAnswer ta
          JOIN FETCH ta.user u
          JOIN FETCH ta.topic t
          WHERE t.meeting.id = :meetingId
          AND ta.isSubmitted = true
          AND u.id IN :userIds
          ORDER BY u.id ASC, t.confirmOrder ASC NULLS LAST, t.id ASC
          """)
    List<TopicAnswer> findSubmittedAnswersByMeetingIdAndUserIds(
            @Param("meetingId") Long meetingId,
            @Param("userIds") List<Long> userIds
    );

    /**
     * 제출된 답변 총 개수
     */
    @Query("""                                                                                                                                                                       
          SELECT COUNT(ta)
          FROM TopicAnswer ta
          JOIN ta.topic t
          WHERE t.meeting.id = :meetingId
          AND ta.isSubmitted = true
          """)
    int countSubmittedAnswersByMeetingId(@Param("meetingId") Long meetingId);
}
