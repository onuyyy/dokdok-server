package com.dokdok.topic.repository;

import com.dokdok.topic.entity.TopicLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface TopicLikeRepository extends JpaRepository<TopicLike, Long> {

    boolean existsByTopicId(Long topicId);

    void deleteByTopicIdAndUserId(Long topicId, Long id);

    @Query("""
            SELECT tl.topic.id
            FROM TopicLike tl
            WHERE tl.topic.id IN :topicIds
            AND tl.user.id = :userId
            """)
    Set<Long> findLikedTopicIds(
            @Param("topicIds") List<Long> topicIds,
            @Param("userId") Long userId
    );
}
