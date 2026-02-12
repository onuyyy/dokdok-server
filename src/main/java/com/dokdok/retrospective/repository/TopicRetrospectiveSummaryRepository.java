package com.dokdok.retrospective.repository;

import com.dokdok.retrospective.entity.TopicRetrospectiveSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TopicRetrospectiveSummaryRepository extends JpaRepository<TopicRetrospectiveSummary, Long> {

    List<TopicRetrospectiveSummary> findAllByTopicIdIn(Collection<Long> topicIds);

    Optional<TopicRetrospectiveSummary> findByTopicId(Long topicId);
}
