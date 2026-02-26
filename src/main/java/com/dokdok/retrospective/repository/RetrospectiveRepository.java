package com.dokdok.retrospective.repository;

import com.dokdok.retrospective.entity.MeetingRetrospective;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RetrospectiveRepository extends JpaRepository<MeetingRetrospective, Long> {

    Optional<MeetingRetrospective> findByIdAndMeetingId(Long meetingRetrospectiveId, Long meetingId);

    @Query("SELECT mr FROM MeetingRetrospective mr " +
            "JOIN FETCH mr.createdBy " +
            "WHERE mr.meeting.id = :meetingId " +
            "ORDER BY mr.createdAt DESC, mr.id DESC")
    List<MeetingRetrospective> findByMeetingIdFirstPage(
            @Param("meetingId") Long meetingId,
            Pageable pageable
    );

    @Query("SELECT mr FROM MeetingRetrospective mr " +
            "JOIN FETCH mr.createdBy " +
            "WHERE mr.meeting.id = :meetingId " +
            "AND (mr.createdAt < :cursorCreatedAt " +
            "     OR (mr.createdAt = :cursorCreatedAt AND mr.id < :cursorCommentId)) " +
            "ORDER BY mr.createdAt DESC, mr.id DESC")
    List<MeetingRetrospective> findByMeetingIdAfterCursor(
            @Param("meetingId") Long meetingId,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorCommentId") Long cursorCommentId,
            Pageable pageable
    );

    @Query("SELECT COUNT(mr) FROM MeetingRetrospective mr WHERE mr.meeting.id = :meetingId")
    int countByMeetingId(@Param("meetingId") Long meetingId);
}
