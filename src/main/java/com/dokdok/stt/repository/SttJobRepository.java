package com.dokdok.stt.repository;

import com.dokdok.stt.entity.SttJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SttJobRepository extends JpaRepository<SttJob, Long> {
    Optional<SttJob> findByIdAndMeetingId(Long id, Long meetingId);
}
