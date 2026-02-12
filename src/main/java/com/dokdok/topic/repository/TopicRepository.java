package com.dokdok.topic.repository;

import com.dokdok.topic.entity.Topic;
import com.dokdok.topic.entity.TopicStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {

    List<Topic> findAllByIdInAndMeetingId(List<Long> topicIds, Long meetingId);

    List<Topic> findByMeetingIdAndTopicStatusOrderByConfirmOrderAsc(
            Long meetingId,
            TopicStatus topicStatus
    );

    @Query("SELECT t " +
            "FROM Topic t " +
            "JOIN FETCH t.proposedBy p " +
            "JOIN FETCH t.meeting m " +
            "JOIN FETCH m.gathering g " +
            "WHERE t.meeting.id = :meetingId " +
            "AND t.topicStatus = com.dokdok.topic.entity.TopicStatus.CONFIRMED " +
            "AND t.deletedAt IS NULL " +
            "ORDER BY t.confirmOrder ASC, t.id ASC")
    List<Topic> findConfirmedTopicsFirstPage(
            @Param("meetingId") Long meetingId,
            Pageable pageable
    );

    @Query("SELECT t " +
            "FROM Topic t " +
            "JOIN FETCH t.proposedBy p " +
            "JOIN FETCH t.meeting m " +
            "JOIN FETCH m.gathering g " +
            "WHERE t.meeting.id = :meetingId " +
            "AND t.topicStatus = com.dokdok.topic.entity.TopicStatus.CONFIRMED " +
            "AND t.deletedAt IS NULL " +
            "AND (t.confirmOrder > :cursorConfirmOrder " +
            "     OR (t.confirmOrder = :cursorConfirmOrder AND t.id > :cursorTopicId)) " +
            "ORDER BY t.confirmOrder ASC, t.id ASC")
    List<Topic> findConfirmedTopicsAfterCursor(
            @Param("meetingId") Long meetingId,
            @Param("cursorConfirmOrder") Integer cursorConfirmOrder,
            @Param("cursorTopicId") Long cursorTopicId,
            Pageable pageable
    );

    @Query("""
                    SELECT t
                    FROM Topic t
                    WHERE t.meeting.id = :meetingId
                    AND t.topicStatus = com.dokdok.topic.entity.TopicStatus.CONFIRMED
                    ORDER BY t.confirmOrder
            """)
    List<Topic> findConfirmedTopics(Long meetingId);

    @Query("SELECT t " +
            "FROM Topic t " +
            "LEFT JOIN FETCH t.meeting m " +
            "LEFT JOIN FETCH t.proposedBy u " +
            "WHERE t.id = :topicId")
    Optional<Topic> findDetailById(Long topicId);

    @Modifying
    @Query("""
            UPDATE Topic t
            SET t.deletedAt = CURRENT_TIMESTAMP
            WHERE t.meeting.id = :meetingId
            AND t.proposedBy.id = :userId
            AND t.deletedAt IS NULL
            """)
    void softDeleteByMeetingIdAndProposedById(
            @Param("meetingId") Long meetingId,
            @Param("userId") Long userId
    );

    @Modifying
    @Query("""
            UPDATE Topic t
            SET t.deletedAt = CURRENT_TIMESTAMP
            WHERE t.meeting.id = :meetingId
            AND t.deletedAt IS NULL
            """)
    void softDeleteByMeetingId(@Param("meetingId") Long meetingId);

    @Query("SELECT t " +
            "FROM Topic t " +
            "JOIN FETCH t.proposedBy p " +
            "JOIN FETCH t.meeting m " +
            "JOIN FETCH m.gathering g " +
            "WHERE t.meeting.id = :meetingId " +
            "AND t.deletedAt IS NULL " +
            "ORDER BY t.likeCount DESC, t.id ASC")
    Page<Topic> findTopicsByMeetingId(
            @Param("meetingId") Long meetingId,
            Pageable pageable
    );

    @Query("SELECT t " +
            "FROM Topic t " +
            "JOIN FETCH t.proposedBy p " +
            "JOIN FETCH t.meeting m " +
            "JOIN FETCH m.gathering g " +
            "WHERE t.meeting.id = :meetingId " +
            "AND t.deletedAt IS NULL " +
            "ORDER BY t.likeCount DESC, t.id ASC")
    List<Topic> findTopicsFirstPage(
            @Param("meetingId") Long meetingId,
            Pageable pageable
    );

    @Query("SELECT t " +
            "FROM Topic t " +
            "JOIN FETCH t.proposedBy p " +
            "JOIN FETCH t.meeting m " +
            "JOIN FETCH m.gathering g " +
            "WHERE t.meeting.id = :meetingId " +
            "AND t.deletedAt IS NULL " +
            "AND (t.likeCount < :cursorLikeCount " +
            "     OR (t.likeCount = :cursorLikeCount AND t.id > :cursorTopicId)) " +
            "ORDER BY t.likeCount DESC, t.id ASC")
    List<Topic> findTopicsAfterCursor(
            @Param("meetingId") Long meetingId,
            @Param("cursorLikeCount") Integer cursorLikeCount,
            @Param("cursorTopicId") Long cursorTopicId,
            Pageable pageable
    );

    @Query("SELECT t " +
            "FROM Topic t " +
            "LEFT JOIN FETCH t.meeting m " +
            "LEFT JOIN FETCH m.gathering g " +
            "WHERE t.id = :topicId " +
            "AND (t.proposedBy.id = :userId " +
            "OR g.gatheringLeader.id = :userId " +
            "OR m.meetingLeader.id = :userId)")
    Optional<Topic> findTopicWithDeletePermission(
            @Param("topicId") Long topicId,
            @Param("userId") Long userId
    );

    @Modifying
    @Query("""
                UPDATE Topic t
                SET t.likeCount = t.likeCount + 1
                WHERE t.id = :topicId
            """)
    void increaseLikeCount(@Param("topicId") Long topicId);

    @Modifying
    @Query("""
                UPDATE Topic t
                SET t.likeCount = t.likeCount - 1
                WHERE t.id = :topicId
                  AND t.likeCount > 0
            """)
    void decreaseLikeCount(@Param("topicId") Long topicId);

    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END " +
            "FROM Topic t " +
            "JOIN t.meeting m " +
            "JOIN m.gathering g " +
            "WHERE t.id = :topicId " +
            "AND (t.proposedBy.id = :userId " +
            "OR g.gatheringLeader.id = :userId " +
            "OR m.meetingLeader.id = :userId)")
    boolean existsByTopicIdAndUserId(
            @Param("topicId") Long topicId,
            @Param("userId") Long userId
    );

    @Query("SELECT t.id " +
            "FROM Topic t " +
            "JOIN t.meeting m " +
            "JOIN m.gathering g " +
            "WHERE t.id IN :topicIds " +
            "AND (t.proposedBy.id = :userId " +
            "OR g.gatheringLeader.id = :userId " +
            "OR m.meetingLeader.id = :userId)")
    Set<Long> findDeletableTopicIds(
            @Param("topicIds") List<Long> topicIds,
            @Param("userId") Long userId
    );

    @Query("""
            SELECT t.meeting.id, t.topicType
            FROM Topic t
            WHERE t.meeting.id IN :meetingIds
            AND t.deletedAt IS NULL
            """)
    List<Object[]> findTopicTypesByMeetingIds(
            @Param("meetingIds") List<Long> meetingIds
    );

    @Query("""
            SELECT t
            FROM Topic t
            WHERE t.meeting.id = :meetingId
            AND t.topicStatus = com.dokdok.topic.entity.TopicStatus.CONFIRMED
            ORDER BY t.confirmOrder
            """)
    List<Topic> findTopicsInfoByMeetingId(
            @Param("meetingId") Long meetingId
    );

    long countByMeetingIdAndTopicStatusAndDeletedAtIsNull(Long meetingId, TopicStatus topicStatus);

    @Query("""
            SELECT t
            FROM Topic t
            WHERE t.meeting.id IN :meetingIds
            AND t.topicStatus = com.dokdok.topic.entity.TopicStatus.CONFIRMED
            ORDER BY t.meeting.id, t.confirmOrder, t.id
            """)
    List<Topic> findTopicsInfoByMeetingIds(
            @Param("meetingIds") List<Long> meetingIds
    );

    @Query("""
            SELECT MAX(t.updatedAt)
            FROM Topic t
            WHERE t.meeting.id = :meetingId
            AND t.topicStatus = :status
            AND t.deletedAt IS NULL
            """)
    LocalDateTime findConfirmedTopicDateByMeetingId(
            @Param("meetingId") Long meetingId,
            @Param("status") TopicStatus status
    );

    /**
     * 확정된 주제가 없고, 약속장 혹은 모임장일 경우 true
     */
    @Query("""
                SELECT CASE WHEN
                    EXISTS (
                        SELECT 1 FROM Meeting m
                        JOIN m.gathering g
                        WHERE m.id = :meetingId
                          AND (
                                m.meetingLeader.id = :userId
                             OR g.gatheringLeader.id = :userId
                          )
                    )
                    AND NOT EXISTS (
                        SELECT 1 FROM Topic t
                        WHERE t.meeting.id = :meetingId
                          AND t.topicStatus = com.dokdok.topic.entity.TopicStatus.CONFIRMED
                    )
                THEN true ELSE false END
            """)
    boolean canConfirmTopic(
            @Param("meetingId") Long meetingId,
            @Param("userId") Long userId
    );


    /**
     * 확정된 약속이며, 확정된 주제가 없고, 약속 멤버일 경우
     */
    @Query("""
                SELECT CASE WHEN
                    EXISTS (
                        SELECT 1 FROM Meeting m
                        WHERE m.id = :meetingId
                          AND m.meetingStatus = com.dokdok.meeting.entity.MeetingStatus.CONFIRMED
                    )
                    AND NOT EXISTS (
                        SELECT 1 FROM Topic t
                        WHERE t.meeting.id = :meetingId
                          AND t.topicStatus = com.dokdok.topic.entity.TopicStatus.CONFIRMED
                    )
                    AND EXISTS (
                        SELECT 1 FROM MeetingMember mm
                        WHERE mm.meeting.id = :meetingId
                          AND mm.user.id = :userId
                    )
                THEN true ELSE false END
            """)
    boolean canSuggestTopic(Long meetingId, Long userId);

    long countByMeetingIdAndDeletedAtIsNull(Long meetingId);
}
