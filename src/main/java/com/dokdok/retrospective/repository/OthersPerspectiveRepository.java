package com.dokdok.retrospective.repository;

import com.dokdok.retrospective.dto.projection.OtherPerspectiveProjection;
import com.dokdok.retrospective.entity.RetrospectiveOthersPerspective;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OthersPerspectiveRepository extends JpaRepository<RetrospectiveOthersPerspective, Long> {

    @Query("""
            SELECT op
            FROM RetrospectiveOthersPerspective op
            JOIN FETCH op.topic t
            JOIN FETCH op.meetingMember mm
            JOIN FETCH mm.user u
            WHERE op.personalMeetingRetrospective.id = :retrospectiveId
            ORDER BY t.confirmOrder
            """)
    List<RetrospectiveOthersPerspective> findByPersonalMeetingRetrospective(Long retrospectiveId);

    @Query("""
            SELECT new com.dokdok.retrospective.dto.projection.OtherPerspectiveProjection(
                        op.personalMeetingRetrospective.id,
                        t.id,
                        t.title,
                        t.confirmOrder,
                        mm.id,
                        u.nickname,
                        op.opinionContent,
                        op.impressiveReason
            )
            FROM RetrospectiveOthersPerspective op
            LEFT JOIN op.topic t
            JOIN op.meetingMember mm
            JOIN mm.user u
            WHERE op.personalMeetingRetrospective.id IN :retrospectiveIds
            ORDER BY t.confirmOrder NULLS LAST, op.id
            """)
    List<OtherPerspectiveProjection> findByRetrospectiveIds(List<Long> retrospectiveIds);
}