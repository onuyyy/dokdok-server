package com.dokdok.meeting.repository;

import com.dokdok.meeting.entity.MeetingMember;
import com.dokdok.meeting.entity.MeetingStatus;
import com.dokdok.meeting.entity.Meeting;
import com.dokdok.retrospective.entity.PersonalMeetingRetrospective;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingMemberRepository extends JpaRepository<MeetingMember, Long> {

    List<MeetingMember> findAllByMeetingId(Long meetingId);

    @Query("""                                                                                                                                                           
            SELECT mm FROM MeetingMember mm
            JOIN FETCH mm.user u
            JOIN FETCH mm.meeting
            WHERE mm.meeting.id = :meetingId
            AND mm.user.id = :userId
            AND mm.canceledAt IS NULL
            """)
    Optional<MeetingMember> findByMeetingIdAndUserId(
            @Param("meetingId") Long meetingId,
            @Param("userId") Long userId
    );

    @Query("""
            SELECT mm FROM MeetingMember mm
            JOIN FETCH mm.user u
            JOIN FETCH mm.meeting
            WHERE mm.meeting.id = :meetingId
            AND mm.user.id = :userId
            """)
    Optional<MeetingMember> findAnyByMeetingIdAndUserId(
            @Param("meetingId") Long meetingId,
            @Param("userId") Long userId
    );

    @Query("""
            SELECT count(mm) FROM MeetingMember mm
            WHERE mm.meeting.id = :meetingId
            AND mm.canceledAt IS NULL
            """)
    int countActiveMembers(@Param("meetingId") Long meetingId);

    boolean existsByMeetingIdAndUserId(
            Long meetingId,
            Long userId
    );

    @Query("""
            SELECT mm.meeting.id FROM MeetingMember mm
            WHERE mm.user.id = :userId
            AND mm.canceledAt IS NULL
            AND mm.meeting.gathering.id = :gatheringId
            """)
    List<Long> findActiveMeetingIdsByUserIdAndGatheringId(
            @Param("userId") Long userId,
            @Param("gatheringId") Long gatheringId
    );

    @Query("""
            SELECT count(mm) FROM MeetingMember mm
            JOIN mm.meeting m
            WHERE mm.user.id = :userId
            AND mm.canceledAt IS NULL
            AND m.gathering.id = :gatheringId
            AND m.meetingStatus = :meetingStatus
            """)
    int countMeetingsByUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("gatheringId") Long gatheringId,
            @Param("meetingStatus") MeetingStatus meetingStatus
    );

    @Query("""
            SELECT count(mm) FROM MeetingMember mm
            JOIN mm.meeting m
            WHERE mm.user.id = :userId
            AND mm.canceledAt IS NULL
            AND m.meetingStatus IN :meetingStatuses
            """)
    int countMyMeetingsByStatuses(
            @Param("userId") Long userId,
            @Param("meetingStatuses") List<MeetingStatus> meetingStatuses
    );

    @Query("""
            SELECT count(mm) FROM MeetingMember mm
            JOIN mm.meeting m
            WHERE mm.user.id = :userId
            AND mm.canceledAt IS NULL
            AND m.meetingStatus = :meetingStatus
            """)
    int countMyMeetingsByStatus(
            @Param("userId") Long userId,
            @Param("meetingStatus") MeetingStatus meetingStatus
    );

    @Query("""
            SELECT count(mm) FROM MeetingMember mm
            JOIN mm.meeting m
            WHERE mm.user.id = :userId
            AND mm.canceledAt IS NULL
            AND m.meetingStatus = :meetingStatus
            AND NOT EXISTS (
                SELECT 1 FROM PersonalMeetingRetrospective pmr
                WHERE pmr.meeting = m
                AND pmr.user.id = :userId
            )
            """)
    int countMyMeetingsByStatusWithoutPersonalRetrospective(
            @Param("userId") Long userId,
            @Param("meetingStatus") MeetingStatus meetingStatus
    );

    @Query("""
            SELECT count(mm) FROM MeetingMember mm
            JOIN mm.meeting m
            WHERE mm.user.id = :userId
            AND mm.canceledAt IS NULL
            AND m.meetingStatus = :meetingStatus
            AND m.meetingStartDate BETWEEN :startDate AND :endDate
            """)
    int countMyUpcomingMeetings(
            @Param("userId") Long userId,
            @Param("meetingStatus") MeetingStatus meetingStatus,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query(
            value = """
                    SELECT m FROM MeetingMember mm
                    JOIN mm.meeting m
                    JOIN FETCH m.book
                    WHERE mm.user.id = :userId
                    AND mm.canceledAt IS NULL
                    AND m.gathering.id = :gatheringId
                    AND m.meetingStatus = :meetingStatus
                    """,
            countQuery = """
                    SELECT count(mm) FROM MeetingMember mm
                    JOIN mm.meeting m
                    WHERE mm.user.id = :userId
                    AND mm.canceledAt IS NULL
                    AND m.gathering.id = :gatheringId
                    AND m.meetingStatus = :meetingStatus
                    """
    )
    Page<Meeting> findMeetingsByUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("gatheringId") Long gatheringId,
            @Param("meetingStatus") MeetingStatus meetingStatus,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT m FROM MeetingMember mm
                    JOIN mm.meeting m
                    JOIN FETCH m.book
                    WHERE mm.user.id = :userId
                    AND mm.canceledAt IS NULL
                    AND m.gathering.id = :gatheringId
                    AND m.meetingStatus = :meetingStatus
                    AND (CAST(:cursorStartDateTime AS timestamp) IS NULL
                        OR m.meetingStartDate > :cursorStartDateTime
                        OR (m.meetingStartDate = :cursorStartDateTime AND m.id > :cursorMeetingId))
                    ORDER BY m.meetingStartDate ASC, m.id ASC
                    """
    )
    List<Meeting> findMeetingsByUserIdAndStatusAfterCursor(
            @Param("userId") Long userId,
            @Param("gatheringId") Long gatheringId,
            @Param("meetingStatus") MeetingStatus meetingStatus,
            @Param("cursorStartDateTime") LocalDateTime cursorStartDateTime,
            @Param("cursorMeetingId") Long cursorMeetingId,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT m FROM MeetingMember mm
                    JOIN mm.meeting m
                    JOIN FETCH m.book
                    JOIN FETCH m.gathering
                    WHERE mm.user.id = :userId
                    AND mm.canceledAt IS NULL
                    AND m.meetingStatus IN :meetingStatuses
                    AND (CAST(:cursorStartDateTime AS timestamp) IS NULL
                        OR m.meetingStartDate > :cursorStartDateTime
                        OR (m.meetingStartDate = :cursorStartDateTime AND m.id > :cursorMeetingId))
                    ORDER BY m.meetingStartDate ASC, m.id ASC
                    """
    )
    List<Meeting> findMyMeetingsByStatusesAfterCursor(
            @Param("userId") Long userId,
            @Param("meetingStatuses") List<MeetingStatus> meetingStatuses,
            @Param("cursorStartDateTime") LocalDateTime cursorStartDateTime,
            @Param("cursorMeetingId") Long cursorMeetingId,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT m FROM MeetingMember mm
                    JOIN mm.meeting m
                    JOIN FETCH m.book
                    JOIN FETCH m.gathering
                    WHERE mm.user.id = :userId
                    AND mm.canceledAt IS NULL
                    AND m.meetingStatus = :meetingStatus
                    AND (CAST(:cursorStartDateTime AS timestamp) IS NULL
                        OR m.meetingStartDate > :cursorStartDateTime
                        OR (m.meetingStartDate = :cursorStartDateTime AND m.id > :cursorMeetingId))
                    ORDER BY m.meetingStartDate ASC, m.id ASC
                    """
    )
    List<Meeting> findMyMeetingsByStatusAfterCursor(
            @Param("userId") Long userId,
            @Param("meetingStatus") MeetingStatus meetingStatus,
            @Param("cursorStartDateTime") LocalDateTime cursorStartDateTime,
            @Param("cursorMeetingId") Long cursorMeetingId,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT m FROM MeetingMember mm
                    JOIN mm.meeting m
                    JOIN FETCH m.book
                    JOIN FETCH m.gathering
                    WHERE mm.user.id = :userId
                    AND mm.canceledAt IS NULL
                    AND m.meetingStatus = :meetingStatus
                    AND NOT EXISTS (
                        SELECT 1 FROM PersonalMeetingRetrospective pmr
                        WHERE pmr.meeting = m
                        AND pmr.user.id = :userId
                    )
                    AND (CAST(:cursorStartDateTime AS timestamp) IS NULL
                        OR m.meetingStartDate > :cursorStartDateTime
                        OR (m.meetingStartDate = :cursorStartDateTime AND m.id > :cursorMeetingId))
                    ORDER BY m.meetingStartDate ASC, m.id ASC
                    """
    )
    List<Meeting> findMyDoneMeetingsWithoutPersonalRetrospectiveAfterCursor(
            @Param("userId") Long userId,
            @Param("meetingStatus") MeetingStatus meetingStatus,
            @Param("cursorStartDateTime") LocalDateTime cursorStartDateTime,
            @Param("cursorMeetingId") Long cursorMeetingId,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT m FROM MeetingMember mm
                    JOIN mm.meeting m
                    JOIN FETCH m.book
                    JOIN FETCH m.gathering
                    WHERE mm.user.id = :userId
                    AND mm.canceledAt IS NULL
                    AND m.meetingStatus = :meetingStatus
                    AND m.meetingStartDate BETWEEN :startDate AND :endDate
                    AND (CAST(:cursorStartDateTime AS timestamp) IS NULL
                        OR m.meetingStartDate > :cursorStartDateTime
                        OR (m.meetingStartDate = :cursorStartDateTime AND m.id > :cursorMeetingId))
                    ORDER BY m.meetingStartDate ASC, m.id ASC
                    """
    )
    List<Meeting> findMyUpcomingMeetingsAfterCursor(
            @Param("userId") Long userId,
            @Param("meetingStatus") MeetingStatus meetingStatus,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("cursorStartDateTime") LocalDateTime cursorStartDateTime,
            @Param("cursorMeetingId") Long cursorMeetingId,
            Pageable pageable
    );

    @Query("""
                SELECT mm
                FROM MeetingMember mm
                JOIN FETCH mm.user u
                WHERE mm.meeting.id = :meetingId
                AND mm.user.id <> :userId
            """)
    List<MeetingMember> findOtherMembersByMeetingId(
            @Param("meetingId") Long meetingId,
            @Param("userId") Long userId
    );

    @Query("""
                SELECT u.id
                FROM MeetingMember mm
                JOIN mm.user u
                JOIN mm.meeting m
                WHERE m.gathering.id = :gatheringId
                AND mm.canceledAt IS NULL
            """)
    List<Long> findByGatheringId(Long gatheringId);
}
