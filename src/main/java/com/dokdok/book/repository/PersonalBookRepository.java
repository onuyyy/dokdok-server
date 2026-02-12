package com.dokdok.book.repository;

import com.dokdok.book.entity.BookReadingStatus;
import com.dokdok.book.entity.PersonalBook;
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
public interface PersonalBookRepository extends JpaRepository<PersonalBook, Long> {
    Optional<PersonalBook> findTopByUserIdAndBookIdAndGatheringIsNullOrderByAddedAtDesc(Long userId, Long bookId);
    Optional<PersonalBook> findByUserIdAndBookIdAndGatheringId(Long userId, Long bookId, Long gatheringId);
    Optional<PersonalBook> findByIdAndUserId(Long personalBookId, Long userId);
    @Query(
            value = """
                select
                    b.book_id as bookId,
                    b.book_name as title,
                    b.publisher as publisher,
                    b.author as authors,
                    (array_agg(pb.reading_status order by pb.added_at desc))[1] as bookReadingStatus,
                    b.book_image_url as thumbnail,
                    string_agg(distinct g.gathering_name, ', ') as gatheringName,
                    max(pb.added_at) as addedAt
                from personal_book pb
                join book b on pb.book_id = b.book_id
                left join gathering g
                    on pb.gathering_id = g.gathering_id
                    and g.deleted_at is null
                where pb.user_id = :userId
                    and pb.deleted_at is null
                    and (:gatheringId is null or g.gathering_id = :gatheringId)
                    and (:readingStatus is null or pb.reading_status = :readingStatus)
                group by b.book_id, b.book_name, b.publisher, b.author, b.book_image_url
                """,
            countQuery = """
                select count(distinct pb.book_id)
                from personal_book pb
                left join gathering g
                    on pb.gathering_id = g.gathering_id
                    and g.deleted_at is null
                where pb.user_id = :userId
                    and pb.deleted_at is null
                    and (:gatheringId is null or g.gathering_id = :gatheringId)
                    and (:readingStatus is null or pb.reading_status = :readingStatus)
                """,
            nativeQuery = true
    )
    Page<PersonalBookListProjection> findPersonalBooksByUserIdReadingStatusAndGatheringId(
            @Param("userId") Long userId,
            @Param("gatheringId") Long gatheringId,
            @Param("readingStatus") String readingStatus,
            Pageable pageable
    );

    @Query(
            value = """
                select
                    b.book_id as bookId,
                    b.book_name as title,
                    b.publisher as publisher,
                    b.author as authors,
                    (array_agg(pb.reading_status order by pb.added_at desc))[1] as bookReadingStatus,
                    b.book_image_url as thumbnail,
                    string_agg(distinct g.gathering_name, ', ') as gatheringName,
                    max(pb.added_at) as addedAt
                from personal_book pb
                join book b on pb.book_id = b.book_id
                left join gathering g
                    on pb.gathering_id = g.gathering_id
                    and g.deleted_at is null
                where pb.user_id = :userId
                    and pb.deleted_at is null
                    and (:gatheringId is null or g.gathering_id = :gatheringId)
                    and (:readingStatus is null or pb.reading_status = :readingStatus)
                group by b.book_id, b.book_name, b.publisher, b.author, b.book_image_url
                having (cast(:cursorAddedAt as timestamp) is null
                        or max(pb.added_at) < :cursorAddedAt
                        or (max(pb.added_at) = :cursorAddedAt and b.book_id < :cursorBookId))
                order by max(pb.added_at) desc, b.book_id desc
                """,
            nativeQuery = true
    )
    List<PersonalBookListProjection> findPersonalBooksByUserIdReadingStatusAndGatheringIdCursor(
            @Param("userId") Long userId,
            @Param("gatheringId") Long gatheringId,
            @Param("readingStatus") String readingStatus,
            @Param("cursorAddedAt") LocalDateTime cursorAddedAt,
            @Param("cursorBookId") Long cursorBookId,
            Pageable pageable
    );

    @Query(
            value = """
                select count(distinct pb.book_id)
                from personal_book pb
                left join gathering g
                    on pb.gathering_id = g.gathering_id
                    and g.deleted_at is null
                where pb.user_id = :userId
                    and pb.deleted_at is null
                    and (:gatheringId is null or g.gathering_id = :gatheringId)
                    and (:readingStatus is null or pb.reading_status = :readingStatus)
                """,
            nativeQuery = true
    )
    long countPersonalBooksByUserIdReadingStatusAndGatheringId(
            @Param("userId") Long userId,
            @Param("gatheringId") Long gatheringId,
            @Param("readingStatus") String readingStatus
    );

}
