package com.dokdok.book.repository;

import com.dokdok.book.repository.dto.ReadingTimelineIndexRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class ReadingTimelineRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<ReadingTimelineIndexRow> findTimeline(
            Long personalBookId,
            Long userId,
            Long bookId,
            Long gatheringId,
            String preOpinionTime,
            LocalDateTime cursorEventAt,
            Integer cursorTypeOrder,
            Long cursorSourceId,
            int limit
    ) {
        String sql = """
                WITH timeline AS (
                    SELECT
                        prr.created_at AS event_at,
                        'READING_RECORD' AS type,
                        prr.record_id AS source_id,
                        3 AS type_order
                    FROM personal_reading_record prr
                    WHERE prr.personal_book_id = :personalBookId
                      AND prr.user_id = :userId
                      AND prr.deleted_at IS NULL

                    UNION ALL

                    SELECT
                        pmr.created_at AS event_at,
                        'PERSONAL_RETROSPECTIVE' AS type,
                        pmr.personal_meeting_retrospective_id AS source_id,
                        2 AS type_order
                    FROM personal_meeting_retrospective pmr
                    JOIN meeting m ON m.meeting_id = pmr.meeting_id
                    WHERE pmr.user_id = :userId
                      AND pmr.deleted_at IS NULL
                      AND m.deleted_at IS NULL
                      AND CAST(:gatheringId AS bigint) IS NOT NULL
                      AND m.gathering_id = :gatheringId
                      AND m.book_id = :bookId

                    UNION ALL

                    SELECT
                        pre.event_at AS event_at,
                        'PRE_OPINION' AS type,
                        pre.meeting_id AS source_id,
                        1 AS type_order
                    FROM (
                        SELECT
                            m.meeting_id,
                            CASE
                                WHEN :preOpinionTime = 'ANSWER_CREATED'
                                    THEN MAX(ta.created_at)
                                ELSE m.meeting_start_date
                            END AS event_at
                        FROM meeting m
                        JOIN topic t
                            ON t.meeting_id = m.meeting_id
                            AND t.topic_status = 'CONFIRMED'
                        JOIN topic_answer ta
                            ON ta.topic_id = t.topic_id
                            AND ta.user_id = :userId
                            AND ta.deleted_at IS NULL
                        WHERE m.deleted_at IS NULL
                          AND CAST(:gatheringId AS bigint) IS NOT NULL
                          AND m.gathering_id = :gatheringId
                          AND m.book_id = :bookId
                        GROUP BY m.meeting_id, m.meeting_start_date
                    ) pre
                )
                SELECT
                    event_at AS eventAt,
                    type AS type,
                    source_id AS sourceId,
                    type_order AS typeOrder
                FROM timeline
                WHERE (
                    CAST(:cursorEventAt AS timestamp) IS NULL
                    OR event_at < :cursorEventAt
                    OR (event_at = :cursorEventAt AND (
                        type_order < :cursorTypeOrder
                        OR (type_order = :cursorTypeOrder AND source_id < :cursorSourceId)
                    ))
                )
                ORDER BY event_at DESC, type_order DESC, source_id DESC
                """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("personalBookId", personalBookId);
        query.setParameter("userId", userId);
        query.setParameter("bookId", bookId);
        query.setParameter("gatheringId", gatheringId);
        query.setParameter("preOpinionTime", preOpinionTime);
        query.setParameter("cursorEventAt", cursorEventAt);
        query.setParameter("cursorTypeOrder", cursorTypeOrder);
        query.setParameter("cursorSourceId", cursorSourceId);
        query.setMaxResults(limit);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        return rows.stream()
                .map(row -> new ReadingTimelineIndexRow(
                        toLocalDateTime(row[0]),
                        (String) row[1],
                        row[2] == null ? null : ((Number) row[2]).longValue(),
                        row[3] == null ? null : ((Number) row[3]).intValue()
                ))
                .toList();
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        throw new IllegalArgumentException("Unsupported date type: " + value.getClass());
    }
}
