package com.dokdok.book.repository;

import com.dokdok.book.entity.PersonalBook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonalBookRepository extends JpaRepository<PersonalBook, Long> {
    Optional<PersonalBook> findTopByUserIdAndBookIdAndGatheringIsNullOrderByAddedAtDesc(Long userId, Long bookId);
    Optional<PersonalBook> findTopByUserIdAndBookIdOrderByAddedAtDesc(Long userId, Long bookId);
    Optional<PersonalBook> findByUserIdAndBookIdAndGatheringId(Long userId, Long bookId, Long gatheringId);
    Optional<PersonalBook> findByIdAndUserId(Long personalBookId, Long userId);
    @Query(
            value = """
                select
                    (array_agg(pb.personal_book_id order by pb.added_at desc, pb.personal_book_id desc))[1] as personalBookId,
                    b.book_id as bookId,
                    b.book_name as title,
                    b.publisher as publisher,
                    b.author as authors,
                    (array_agg(pb.reading_status order by pb.added_at desc, pb.personal_book_id desc))[1] as bookReadingStatus,
                    b.thumbnail as thumbnail,
                    max(br.rating) as rating,
                    coalesce(
                        json_agg(distinct jsonb_build_object('gatheringId', g.gathering_id, 'gatheringName', g.gathering_name))
                            filter (where g.gathering_id is not null),
                        '[]'::json
                    )::text as gatherings,
                    max(pb.added_at) as addedAt,
                    (
                        select
                            case
                                when bool_or(m.meeting_status = 'CONFIRMED') then 'BEFORE'
                                when count(m.meeting_id) > 0 and bool_and(m.meeting_status = 'DONE') then 'AFTER'
                                else null
                            end
                        from meeting m
                        where m.book_id = b.book_id
                            and m.deleted_at is null
                            and (:gatheringId is null or m.gathering_id = :gatheringId)
                            and m.gathering_id in (
                                select pb2.gathering_id from personal_book pb2
                                where pb2.book_id = b.book_id
                                    and pb2.user_id = :userId
                                    and pb2.deleted_at is null
                                    and pb2.gathering_id is not null
                            )
                    ) as meetingProgressStatus
                from personal_book pb
                join book b on pb.book_id = b.book_id
                left join gathering g
                    on pb.gathering_id = g.gathering_id
                    and g.deleted_at is null
                left join book_review br
                    on br.book_id = b.book_id
                    and br.user_id = :userId
                    and br.deleted_at is null
                where pb.user_id = :userId
                    and pb.deleted_at is null
                    and (:gatheringId is null or g.gathering_id = :gatheringId)
                group by b.book_id, b.book_name, b.publisher, b.author, b.thumbnail
                having (:readingStatus is null or
                    (array_agg(pb.reading_status order by pb.added_at desc, pb.personal_book_id desc))[1] = :readingStatus)
                """,
            countQuery = """
                select count(*)
                from (
                    select pb.book_id
                    from personal_book pb
                    left join gathering g
                        on pb.gathering_id = g.gathering_id
                        and g.deleted_at is null
                    where pb.user_id = :userId
                        and pb.deleted_at is null
                        and (:gatheringId is null or g.gathering_id = :gatheringId)
                    group by pb.book_id
                    having (:readingStatus is null or
                        (array_agg(pb.reading_status order by pb.added_at desc, pb.personal_book_id desc))[1] = :readingStatus)
                ) sub
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
                    (array_agg(pb.personal_book_id order by pb.added_at desc, pb.personal_book_id desc))[1] as personalBookId,
                    b.book_id as bookId,
                    b.book_name as title,
                    b.publisher as publisher,
                    b.author as authors,
                    (array_agg(pb.reading_status order by pb.added_at desc, pb.personal_book_id desc))[1] as bookReadingStatus,
                    b.thumbnail as thumbnail,
                    max(br.rating) as rating,
                    coalesce(
                        json_agg(distinct jsonb_build_object('gatheringId', g.gathering_id, 'gatheringName', g.gathering_name))
                            filter (where g.gathering_id is not null),
                        '[]'::json
                    )::text as gatherings,
                    max(pb.added_at) as addedAt,
                    (
                        select
                            case
                                when bool_or(m.meeting_status = 'CONFIRMED') then 'BEFORE'
                                when count(m.meeting_id) > 0 and bool_and(m.meeting_status = 'DONE') then 'AFTER'
                                else null
                            end
                        from meeting m
                        where m.book_id = b.book_id
                            and m.deleted_at is null
                            and (:gatheringId is null or m.gathering_id = :gatheringId)
                            and m.gathering_id in (
                                select pb2.gathering_id from personal_book pb2
                                where pb2.book_id = b.book_id
                                    and pb2.user_id = :userId
                                    and pb2.deleted_at is null
                                    and pb2.gathering_id is not null
                            )
                    ) as meetingProgressStatus
                from personal_book pb
                join book b on pb.book_id = b.book_id
                left join gathering g
                    on pb.gathering_id = g.gathering_id
                    and g.deleted_at is null
                left join book_review br
                    on br.book_id = b.book_id
                    and br.user_id = :userId
                    and br.deleted_at is null
                where pb.user_id = :userId
                    and pb.deleted_at is null
                    and (:gatheringId is null or g.gathering_id = :gatheringId)
                group by b.book_id, b.book_name, b.publisher, b.author, b.thumbnail
                having (:readingStatus is null or
                    (array_agg(pb.reading_status order by pb.added_at desc, pb.personal_book_id desc))[1] = :readingStatus)
                """,
            nativeQuery = true
    )
    List<PersonalBookListProjection> findPersonalBookAggregatesByUserIdAndGatheringIdAndReadingStatus(
            @Param("userId") Long userId,
            @Param("gatheringId") Long gatheringId,
            @Param("readingStatus") String readingStatus
    );

    @Query(
            value = """
                with latest_status as (
                    select
                        pb.book_id,
                        ((array_agg(pb.reading_status order by pb.added_at desc, pb.personal_book_id desc))[1])::text as readingStatus
                    from personal_book pb
                    left join gathering g
                        on pb.gathering_id = g.gathering_id
                        and g.deleted_at is null
                    where pb.user_id = :userId
                        and pb.deleted_at is null
                        and (:gatheringId is null or g.gathering_id = :gatheringId)
                    group by pb.book_id
                )
                select
                    ls.readingStatus as readingStatus,
                    count(*) as count
                from latest_status ls
                group by ls.readingStatus
                """,
            nativeQuery = true
    )
    List<PersonalBookStatusCountProjection> countPersonalBookStatusByUserIdAndGatheringId(
            @Param("userId") Long userId,
            @Param("gatheringId") Long gatheringId
    );

    @Query(value = """
            SELECT DISTINCT g.gathering_id AS gatheringId, g.gathering_name AS gatheringName
            FROM personal_book pb
            JOIN gathering g ON pb.gathering_id = g.gathering_id
            WHERE pb.user_id = :userId
              AND pb.deleted_at IS NULL
              AND g.deleted_at IS NULL
              AND pb.personal_book_id = :personalBookId
            """, nativeQuery = true)
    List<PersonalBookGatheringProjection> findActiveGatheringsWithMeetingsByUserAndBook(
            @Param("userId") Long userId,
            @Param("personalBookId") Long personalBookId
    );

}
