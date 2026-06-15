package com.dokdok.meeting.repository;

import com.dokdok.gathering.repository.GatheringCountProjection;
import com.dokdok.meeting.entity.Meeting;
import com.dokdok.meeting.entity.MeetingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    boolean existsByIdAndGatheringId(Long meetingId, Long gatheringId);
  
    boolean existsByGatheringIdAndMeetingStatus(Long gatheringId, MeetingStatus meetingStatus);

    @Query("""
            SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END
            FROM Meeting m
            WHERE m.gathering.id = :gatheringId
            AND m.meetingStatus = :meetingStatus
            AND m.id <> :meetingId
            AND m.meetingStartDate < :endDate
            AND m.meetingEndDate > :startDate
            """)
    boolean existsOverlappingMeeting(
            @Param("gatheringId") Long gatheringId,
            @Param("meetingStatus") MeetingStatus meetingStatus,
            @Param("meetingId") Long meetingId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    int countByGatheringIdAndMeetingStatus(Long gatheringId, MeetingStatus meetingStatus);

    int countByGatheringIdAndMeetingStatusIn(Long gatheringId, List<MeetingStatus> meetingStatuses);

    @EntityGraph(attributePaths = {"book"})
    Page<Meeting> findByGatheringIdAndMeetingStatus(
            Long gatheringId,
            MeetingStatus meetingStatus,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"book"})
    Page<Meeting> findByGatheringIdAndMeetingStatusIn(
            Long gatheringId,
            List<MeetingStatus> meetingStatuses,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"book"})
    Page<Meeting> findByGatheringIdAndMeetingStatusAndMeetingStartDateBetween(
            Long gatheringId,
            MeetingStatus meetingStatus,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"book"})
    @Query("""
            SELECT m
            FROM Meeting m
            WHERE m.gathering.id = :gatheringId
            AND m.meetingStatus = :meetingStatus
            AND (CAST(:cursorStartDateTime AS timestamp) IS NULL
                OR m.meetingStartDate > :cursorStartDateTime
                OR (m.meetingStartDate = :cursorStartDateTime AND m.id > :cursorMeetingId))
            ORDER BY m.meetingStartDate ASC, m.id ASC
            """)
    List<Meeting> findByGatheringIdAndMeetingStatusAfterCursor(
            @Param("gatheringId") Long gatheringId,
            @Param("meetingStatus") MeetingStatus meetingStatus,
            @Param("cursorStartDateTime") LocalDateTime cursorStartDateTime,
            @Param("cursorMeetingId") Long cursorMeetingId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"book"})
    @Query("""
            SELECT m
            FROM Meeting m
            WHERE m.gathering.id = :gatheringId
            AND m.meetingStatus = :meetingStatus
            AND m.meetingStartDate BETWEEN :startDate AND :endDate
            AND (CAST(:cursorStartDateTime AS timestamp) IS NULL
                OR m.meetingStartDate > :cursorStartDateTime
                OR (m.meetingStartDate = :cursorStartDateTime AND m.id > :cursorMeetingId))
            ORDER BY m.meetingStartDate ASC, m.id ASC
            """)
    List<Meeting> findByGatheringIdAndMeetingStatusAndMeetingStartDateBetweenAfterCursor(
            @Param("gatheringId") Long gatheringId,
            @Param("meetingStatus") MeetingStatus meetingStatus,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("cursorStartDateTime") LocalDateTime cursorStartDateTime,
            @Param("cursorMeetingId") Long cursorMeetingId,
            Pageable pageable
    );

    @Query("""
            SELECT count(m)
            FROM Meeting m
            WHERE m.gathering.id = :gatheringId
            AND m.meetingStatus = :meetingStatus
            AND m.meetingStartDate BETWEEN :startDate AND :endDate
            """)
    int countUpcomingMeetings(
            @Param("gatheringId") Long gatheringId,
            @Param("meetingStatus") MeetingStatus meetingStatus,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // Scheduler용: 종료 시간이 지난 CONFIRMED 상태의 Meeting 조회
    List<Meeting> findByMeetingEndDateBeforeAndMeetingStatus(
            LocalDateTime endDate,
            MeetingStatus meetingStatus
    );

    // Scheduler용: 약속 당일이고 임시저장 사전의견이 있는 CONFIRMED 상태의 Meeting 조회
    @Query("""
            SELECT DISTINCT m
            FROM Meeting m
            JOIN FETCH m.book b
            JOIN Topic t ON t.meeting = m
            JOIN TopicAnswer ta ON ta.topic = t
            WHERE m.meetingStartDate >= :dayStart
            AND m.meetingStartDate < :nextDayStart
            AND m.meetingStatus = :meetingStatus
            AND ta.isSubmitted = false
            """)
    List<Meeting> findMeetingsOnDateWithDraftPreOpinions(
            @Param("dayStart") LocalDateTime dayStart,
            @Param("nextDayStart") LocalDateTime nextDayStart,
            @Param("meetingStatus") MeetingStatus meetingStatus
    );

    // Scheduler용: 시작 24시간 이내이고 확정 주제가 없는 CONFIRMED 상태의 Meeting 조회
    @Query("""
            SELECT DISTINCT m
            FROM Meeting m
            WHERE m.meetingStartDate <= :deadline
            AND m.meetingStatus = :meetingStatus
            AND NOT EXISTS (
                SELECT 1
                FROM Topic t
                WHERE t.meeting = m
                AND t.topicStatus = com.dokdok.topic.entity.TopicStatus.CONFIRMED
                AND t.deletedAt IS NULL
            )
            AND EXISTS (
                SELECT 1
                FROM Topic t
                WHERE t.meeting = m
                AND t.deletedAt IS NULL
            )
            """)
    List<Meeting> findMeetingsDueForTopicAutoConfirm(
            @Param("deadline") LocalDateTime deadline,
            @Param("meetingStatus") MeetingStatus meetingStatus
    );

    Optional<Meeting> findTopByGatheringIdAndBookIdAndMeetingStatusOrderByMeetingStartDateDescIdDesc(
            Long gatheringId,
            Long bookId,
            MeetingStatus meetingStatus
    );

    @EntityGraph(attributePaths = {"gathering"})
    @Query("""
            SELECT m
            FROM Meeting m
            WHERE m.id IN :meetingIds
            """)
    List<Meeting> findByIdInWithGathering(@Param("meetingIds") List<Long> meetingIds);

    @Query("""
            SELECT DISTINCT b
            FROM Meeting m
            JOIN m.book b
            WHERE m.gathering.id = :gatheringId
            AND m.meetingStatus IN :meetingStatuses
            ORDER BY b.id ASC
            """)
    Page<com.dokdok.book.entity.Book> findDistinctBooksByGatheringIdAndStatuses(
            @Param("gatheringId") Long gatheringId,
            @Param("meetingStatuses") List<MeetingStatus> meetingStatuses,
            Pageable pageable
    );

    /**
     * 여러 모임의 완료된 미팅 수를 한번에 조회
     */
    @Query("SELECT m.gathering.id AS gatheringId, COUNT (m) AS count " +
            "FROM Meeting m " +
            "WHERE m.gathering.id IN :gatheringIds " +
            "AND m.meetingStatus = :status " +
            "GROUP BY m.gathering.id")
    List<GatheringCountProjection> countByGatheringIdsAndStatus(@Param("gatheringIds") List<Long> gatheringIds, @Param("status") MeetingStatus status);

}
