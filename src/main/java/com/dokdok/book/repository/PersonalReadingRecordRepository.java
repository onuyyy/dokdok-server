package com.dokdok.book.repository;

import com.dokdok.book.entity.PersonalReadingRecord;
import com.dokdok.book.entity.RecordType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PersonalReadingRecordRepository extends JpaRepository<PersonalReadingRecord, Long> {
    Optional<PersonalReadingRecord> findByIdAndPersonalBook_IdAndUserId(Long id, Long personalBookId, Long userId);
    List<PersonalReadingRecord> findByIdInAndPersonalBook_IdAndUserId(List<Long> ids, Long personalBookId, Long userId);

    @Query("""
            select record
            from PersonalReadingRecord record
            join record.personalBook pb
            left join pb.gathering g
            where pb.id = :personalBookId
                and record.user.id = :userId
                and (:gatheringId is null or g.id = :gatheringId)
                and (:recordType is null or record.recordType = :recordType)
            """)
    Page<PersonalReadingRecord> findRecords(
            @Param("personalBookId") Long personalBookId,
            @Param("userId") Long userId,
            @Param("gatheringId") Long gatheringId,
            @Param("recordType") RecordType recordType,
            Pageable pageable
    );

    @Query("""
            select count(record)
            from PersonalReadingRecord record
            join record.personalBook pb
            left join pb.gathering g
            where pb.id = :personalBookId
                and record.user.id = :userId
                and (:gatheringId is null or g.id = :gatheringId)
                and (:recordType is null or record.recordType = :recordType)
            """)
    long countRecords(
            @Param("personalBookId") Long personalBookId,
            @Param("userId") Long userId,
            @Param("gatheringId") Long gatheringId,
            @Param("recordType") RecordType recordType
    );

    @Query("""
            select record
            from PersonalReadingRecord record
            join record.personalBook pb
            left join pb.gathering g
            where pb.id = :personalBookId
                and record.user.id = :userId
                and (:gatheringId is null or g.id = :gatheringId)
                and (:recordType is null or record.recordType = :recordType)
                and (
                    :cursorCreatedAt is null
                    or record.createdAt < :cursorCreatedAt
                    or (record.createdAt = :cursorCreatedAt and record.id < :cursorRecordId)
                )
            order by record.createdAt desc, record.id desc
            """)
    List<PersonalReadingRecord> findRecordsByCursor(
            @Param("personalBookId") Long personalBookId,
            @Param("userId") Long userId,
            @Param("gatheringId") Long gatheringId,
            @Param("recordType") RecordType recordType,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorRecordId") Long cursorRecordId,
            Pageable pageable
    );

    @Query("""
            select record
            from PersonalReadingRecord record
            join record.personalBook pb
            left join pb.gathering g
            where pb.id = :personalBookId
                and record.user.id = :userId
                and (:gatheringId is null or g.id = :gatheringId)
                and (:recordType is null or record.recordType = :recordType)
                and (
                    :cursorCreatedAt is null
                    or record.createdAt > :cursorCreatedAt
                    or (record.createdAt = :cursorCreatedAt and record.id > :cursorRecordId)
                )
            order by record.createdAt asc, record.id asc
            """)
    List<PersonalReadingRecord> findRecordsByCursorAsc(
            @Param("personalBookId") Long personalBookId,
            @Param("userId") Long userId,
            @Param("gatheringId") Long gatheringId,
            @Param("recordType") RecordType recordType,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorRecordId") Long cursorRecordId,
            Pageable pageable
    );
}
