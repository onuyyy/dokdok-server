package com.dokdok.gathering.entity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface GatheringBookRepository extends JpaRepository<GatheringBook, Long> {

    @Query(value = """
            SELECT gb
            FROM GatheringBook gb
            JOIN FETCH gb.book b
            WHERE gb.gathering.id = :gatheringId
            """,
            countQuery = """
            SELECT COUNT(gb)
            FROM GatheringBook gb
            WHERE gb.gathering.id = :gatheringId
            """)
    Page<GatheringBook> findGatheringBooks(Long gatheringId, Pageable pageable);
}
