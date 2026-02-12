package com.dokdok.retrospective.repository;

import com.dokdok.retrospective.dto.projection.ChangedThoughtProjection;
import com.dokdok.retrospective.entity.RetrospectiveChangedThought;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChangedThoughtRepository extends JpaRepository<RetrospectiveChangedThought, Long> {

    @Query("""
            SELECT ct
            FROM RetrospectiveChangedThought ct
            JOIN FETCH ct.topic t
            WHERE ct.personalMeetingRetrospective.id = :retrospectiveId
            ORDER BY t.confirmOrder
            """)
    List<RetrospectiveChangedThought> findByPersonalMeetingRetrospective(Long retrospectiveId);

    @Query("""
            SELECT new com.dokdok.retrospective.dto.projection.ChangedThoughtProjection(
                        pmr.id,
                        t.id,
                        t.title,
                        t.confirmOrder,
                        ct.keyIssue,
                        ct.postOpinion
            )
            FROM RetrospectiveChangedThought ct
            JOIN ct.topic t
            JOIN ct.personalMeetingRetrospective pmr
            WHERE ct.personalMeetingRetrospective.id IN :retrospectiveIds
            ORDER BY t.confirmOrder
            """)
    List<ChangedThoughtProjection> findByRetrospectiveIds(List<Long> retrospectiveIds);
}
