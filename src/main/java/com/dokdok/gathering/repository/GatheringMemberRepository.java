package com.dokdok.gathering.repository;

import com.dokdok.gathering.entity.GatheringMember;
import com.dokdok.gathering.entity.GatheringMemberStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GatheringMemberRepository extends JpaRepository<GatheringMember, Long> {

    /**
     * 사용자가 특정 모임의 활성 멤버인지 확인
     */
    boolean existsByGatheringIdAndUserId(Long gatheringId, Long userId);

    /**
     * 특정 모임의 활성 멤버 수 조회
     */
    int countByGatheringIdAndRemovedAtIsNull(Long gatheringId);

    /**
     * 특정 유저가 특정 모임의 멤버인지 확인 (Gathering fetch join)
     */
    @Query("SELECT gm FROM GatheringMember gm " +
            "JOIN FETCH gm.gathering g " +
            "WHERE gm.gathering.id = :gatheringId " +
            "AND gm.user.id = :userId " +
            "AND gm.removedAt IS NULL")
    Optional<GatheringMember> findByGatheringIdAndUserId(
            @Param("gatheringId") Long gatheringId,
            @Param("userId") Long userId
    );

    /**
     * 특정 모임의 모든 활성 멤버 조회 (User 정보 포함)
     */
    @Query("SELECT gm FROM GatheringMember gm " +
            "JOIN FETCH gm.user u " +
            "JOIN FETCH gm.gathering g " +
            "WHERE gm.gathering.id = :gatheringId " +
            "AND gm.removedAt IS NULL")
    List<GatheringMember> findAllMembersByGatheringId(@Param("gatheringId") Long gatheringId);

    /**
     * 특정 모임의 ACTIVE 상태 멤버 수 조회
     */
    @Query("SELECT count(gm) FROM GatheringMember gm " +
            "WHERE gm.gathering.id = :gatheringId " +
            "AND gm.memberStatus = 'ACTIVE' " +
            "AND gm.removedAt IS NULL")
    int countActiveMembersByStatus(@Param("gatheringId") Long gatheringId);

    // 사용자의 즐겨찾기 모임 목록 조회
    @Query("SELECT gm FROM GatheringMember gm " +
            "JOIN FETCH gm.gathering g " +
            "WHERE gm.user.id = :userId " +
            "AND gm.isFavorite = true " +
            "AND gm.memberStatus = 'ACTIVE' " +
            "AND gm.removedAt IS NULL " +
            "ORDER BY gm.joinedAt DESC")
    List<GatheringMember> findFavoriteGatheringsByUserId(@Param("userId") Long userId);

    // 사용자 즐겨찾기 개수 조회
    @Query(value =
            "SELECT COUNT(*) >= 4 FROM (" +
                    "SELECT 1 FROM gathering_member " +
                    "WHERE user_id = :userId " +
                    "AND is_favorite = true " +
                    "AND member_status = 'ACTIVE' " +
                    "AND removed_at IS NULL " +
                    "LIMIT 4" +
                    ") sub", nativeQuery = true)
    boolean isFavoriteLimitExceeded(@Param("userId") Long userId);

    /**
     * 커서 기반 내 모임 목록 조회 (첫 페이지)
     */
    @Query("SELECT gm FROM GatheringMember gm " +
            "JOIN FETCH gm.gathering g " +
            "WHERE gm.user.id = :userId " +
            "AND gm.memberStatus = 'ACTIVE' " +
            "AND g.gatheringStatus = 'ACTIVE' " +
            "AND gm.removedAt IS NULL " +
            "ORDER BY gm.joinedAt DESC, gm.id DESC")
    List<GatheringMember> findMyGatheringsFirstPage(
            @Param("userId") Long userId,
            Pageable pageable
    );

    /**
     * 커서 기반 내 모임 목록 조회 (다음 페이지)
     */
    @Query("SELECT gm FROM GatheringMember gm " +
            "JOIN FETCH gm.gathering g " +
            "WHERE gm.user.id = :userId " +
            "AND gm.memberStatus = 'ACTIVE' " +
            "AND g.gatheringStatus = 'ACTIVE' " +
            "AND gm.removedAt IS NULL " +
            "AND (gm.joinedAt < :cursorJoinedAt " +
            "     OR (gm.joinedAt = :cursorJoinedAt AND gm.id < :cursorId)) " +
            "ORDER BY gm.joinedAt DESC, gm.id DESC")
    List<GatheringMember> findMyGatheringsAfterCursor(
            @Param("userId") Long userId,
            @Param("cursorJoinedAt") LocalDateTime cursorJoinedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    /**
     * 커서 기반 내 모임 목록 총 개수 조회 (첫 페이지용)
     */
    @Query("SELECT count(gm) FROM GatheringMember gm " +
            "JOIN gm.gathering g " +
            "WHERE gm.user.id = :userId " +
            "AND gm.memberStatus = 'ACTIVE' " +
            "AND g.gatheringStatus = 'ACTIVE' " +
            "AND gm.removedAt IS NULL")
    int countMyGatherings(@Param("userId") Long userId);

    /**
     * 모임 멤버 상태별 조회 (첫 페이지)
     */
    @Query("SELECT gm FROM GatheringMember gm " +
            "JOIN FETCH gm.user u " +
            "WHERE gm.gathering.id = :gatheringId " +
            "AND gm.memberStatus = :status " +
            "AND gm.removedAt IS NULL " +
            "ORDER BY gm.id DESC")
    List<GatheringMember> findMembersByStatusFirstPage(
            @Param("gatheringId") Long gatheringId,
            @Param("status") GatheringMemberStatus status,
            Pageable pageable
    );

    /**
     * 모임 멤버 상태별 조회 (다음 페이지)
     */
    @Query("SELECT gm FROM GatheringMember gm " +
            "JOIN FETCH gm.user u " +
            "WHERE gm.gathering.id = :gatheringId " +
            "AND gm.memberStatus = :status " +
            "AND gm.removedAt IS NULL " +
            "AND gm.id < :cursorId " +
            "ORDER BY gm.id DESC")
    List<GatheringMember> findMembersByStatusAfterCursor(
            @Param("gatheringId") Long gatheringId,
            @Param("status") GatheringMemberStatus status,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    /**
     * 모임 멤버 상태별 총 개수 조회
     */
    @Query("SELECT count(gm) FROM GatheringMember gm " +
            "WHERE gm.gathering.id = :gatheringId " +
            "AND gm.memberStatus = :status " +
            "AND gm.removedAt IS NULL")
    int countMembersByStatus(
            @Param("gatheringId") Long gatheringId,
            @Param("status") GatheringMemberStatus status
    );

    /**
     * 여러 모임의 ACTIVE 멤버 수를 한번에 조회
     */
    @Query("SELECT gm.gathering.id AS gatheringId, COUNT(gm) AS count " +
            "FROM GatheringMember gm " +
            "WHERE gm.gathering.id IN :gatheringIds " +
            "AND gm.memberStatus = 'ACTIVE' " +
            "AND gm.removedAt IS NULL " +
            "GROUP BY gm.gathering.id")
    List<GatheringCountProjection> countActiveMembersByGatherings(
            @Param("gatheringIds") List<Long> gatheringIds
    );
}
