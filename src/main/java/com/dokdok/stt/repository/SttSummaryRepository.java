package com.dokdok.stt.repository;

import com.dokdok.stt.entity.SttSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SttSummaryRepository extends JpaRepository<SttSummary, Long> {
    Optional<SttSummary> findBySttJobId(Long sttJobId);
}
