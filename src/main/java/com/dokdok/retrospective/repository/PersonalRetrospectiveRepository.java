package com.dokdok.retrospective.repository;

import com.dokdok.retrospective.entity.PersonalMeetingRetrospective;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Repository
public interface PersonalRetrospectiveRepository extends JpaRepository<PersonalMeetingRetrospective, Long> {

    boolean existsByMeetingIdAndUserId(Long meetingId, Long userId);

    Optional<PersonalMeetingRetrospective> findByIdAndUser_Id(Long retrospectiveId, Long userId);

    @Query("""
            SELECT pmr
            FROM PersonalMeetingRetrospective pmr
            JOIN FETCH pmr.meeting m
            JOIN FETCH m.gathering g
            JOIN FETCH m.book b
            WHERE b.id = :bookId
            AND pmr.user.id = :userId
            ORDER BY pmr.createdAt DESC
            """)
    List<PersonalMeetingRetrospective> findByBookAndUser(
            @Param("bookId") Long bookId,
            @Param("userId") Long userId
    );

    @Query("""
            SELECT pmr
            FROM PersonalMeetingRetrospective pmr
            JOIN FETCH pmr.meeting m
            JOIN FETCH m.gathering g
            JOIN FETCH m.book b
            WHERE b.id = :bookId
            AND pmr.user.id = :userId
            ORDER BY pmr.createdAt DESC, pmr.id DESC
            """)
    List<PersonalMeetingRetrospective> findRetrospectivesFirstPage(
            @Param("bookId") Long bookId,
            @Param("userId") Long userId,
            Pageable pageable
    );

    @Query("""
            SELECT pmr
            FROM PersonalMeetingRetrospective pmr
            JOIN FETCH pmr.meeting m
            JOIN FETCH m.gathering g
            JOIN FETCH m.book b
            WHERE b.id = :bookId
            AND pmr.user.id = :userId
            AND (pmr.createdAt < :cursorCreatedAt
                 OR (pmr.createdAt = :cursorCreatedAt AND pmr.id < :cursorRetrospectiveId))
            ORDER BY pmr.createdAt DESC, pmr.id DESC
            """)
    List<PersonalMeetingRetrospective> findRetrospectivesAfterCursor(
            @Param("bookId") Long bookId,
            @Param("userId") Long userId,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorRetrospectiveId") Long cursorRetrospectiveId,
            Pageable pageable
    );

    @Query("""
            SELECT count(pmr)
            FROM PersonalMeetingRetrospective pmr
            JOIN pmr.meeting m
            JOIN m.book b
            WHERE b.id = :bookId
            AND pmr.user.id = :userId
            """)
    int countRetrospectivesByBookAndUser(
            @Param("bookId") Long bookId,
            @Param("userId") Long userId
    );

    boolean existsByIdAndUserId(Long retrospectiveId, Long userId);

    @Query("""
            SELECT pmr
            FROM PersonalMeetingRetrospective pmr
            JOIN FETCH pmr.meeting m
            JOIN FETCH m.gathering g
            WHERE pmr.id IN :retrospectiveIds
            AND pmr.user.id = :userId
            """)
    List<PersonalMeetingRetrospective> findByIdsWithMeeting(
            @Param("retrospectiveIds") List<Long> retrospectiveIds,
            @Param("userId") Long userId
    );
}
