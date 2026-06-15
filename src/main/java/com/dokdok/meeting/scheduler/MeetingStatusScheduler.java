package com.dokdok.meeting.scheduler;

import com.dokdok.meeting.entity.Meeting;
import com.dokdok.meeting.entity.MeetingStatus;
import com.dokdok.meeting.repository.MeetingRepository;
import com.dokdok.topic.service.PreOpinionAutoShareService;
import com.dokdok.topic.service.TopicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Meeting 상태 자동 업데이트 Scheduler
 * - 매 10분마다 실행
 * - 약속 시작 24시간 이내에 확정 주제가 없으면 주제를 자동 확정
 * - 약속 당일에 임시저장 사전의견을 자동 공유
 * - meetingEndDate가 지난 CONFIRMED 상태의 Meeting을 DONE으로 변경
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MeetingStatusScheduler {

    private final MeetingRepository meetingRepository;
    private final PreOpinionAutoShareService preOpinionAutoShareService;
    private final TopicService topicService;

    /**
     * 종료 시간이 지난 모임의 상태를 자동으로 DONE으로 변경
     * - 매 10분마다 실행 (0분, 10분, 20분, ...)
     */
    @Scheduled(cron = "0 */10 * * * *")
    @Transactional
    public void updateExpiredMeetings() {
        autoConfirmTopicsDueWithin24Hours();
        autoShareTodayMeetings();

        long startTime = System.currentTimeMillis();
        
        LocalDateTime now = LocalDateTime.now();
        
        // 1. 종료 시간이 지난 CONFIRMED 상태의 Meeting 조회
        List<Meeting> expiredMeetings = meetingRepository
                .findByMeetingEndDateBeforeAndMeetingStatus(now, MeetingStatus.CONFIRMED);
        
        if (expiredMeetings.isEmpty()) {
            log.info("[Scheduler] No expired meetings found.");
            return;
        }
        
        // 2. 상태를 DONE으로 변경
        int count = 0;
        for (Meeting meeting : expiredMeetings) {
            try {
                meeting.changeStatus(MeetingStatus.DONE);
                count++;
            } catch (Exception e) {
                log.error("[Scheduler] Failed to update meeting {}: {}", meeting.getId(), e.getMessage());
            }
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // 3. 성능 지표 로깅
        log.info("[Scheduler] ========================================");
        log.info("[Scheduler] Updated {} / {} meetings to DONE", count, expiredMeetings.size());
        log.info("[Scheduler] Execution time: {}ms", duration);
        log.info("[Scheduler] Throughput: {} meetings/sec", 
                 duration > 0 ? (count * 1000.0 / duration) : count);
        log.info("[Scheduler] ========================================");
    }

    private void autoConfirmTopicsDueWithin24Hours() {
        LocalDateTime deadline = LocalDateTime.now().plusHours(24);
        List<Meeting> meetings = meetingRepository
                .findMeetingsDueForTopicAutoConfirm(deadline, MeetingStatus.CONFIRMED);

        if (meetings.isEmpty()) {
            log.info("[Scheduler] No meetings for topic auto-confirm.");
            return;
        }

        int confirmedTopicCount = 0;
        int confirmedMeetingCount = 0;

        for (Meeting meeting : meetings) {
            try {
                TopicService.AutoConfirmResult result = topicService.autoConfirmTopics(meeting);
                confirmedTopicCount += result.confirmedTopicCount();
                if (result.confirmedTopicCount() > 0) {
                    confirmedMeetingCount++;
                }
            } catch (Exception e) {
                log.error("[Scheduler] Failed to auto-confirm topics for meeting {}: {}",
                        meeting.getId(), e.getMessage());
            }
        }

        log.info("[Scheduler] Auto-confirmed topics for {} / {} meetings: topics={}",
                confirmedMeetingCount, meetings.size(), confirmedTopicCount);
    }

    private void autoShareTodayMeetings() {
        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime nextDayStart = today.plusDays(1).atStartOfDay();
        List<Meeting> todayMeetings = meetingRepository
                .findMeetingsOnDateWithDraftPreOpinions(dayStart, nextDayStart, MeetingStatus.CONFIRMED);

        if (todayMeetings.isEmpty()) {
            log.info("[Scheduler] No today's meetings for pre-opinion auto share.");
            return;
        }

        int submittedAnswerCount = 0;
        int submittedUserCount = 0;
        int appliedReviewCount = 0;

        for (Meeting meeting : todayMeetings) {
            try {
                PreOpinionAutoShareService.AutoShareResult result =
                        preOpinionAutoShareService.autoShareDrafts(meeting);
                submittedAnswerCount += result.submittedAnswerCount();
                submittedUserCount += result.submittedUserCount();
                appliedReviewCount += result.appliedReviewCount();
            } catch (Exception e) {
                log.error("[Scheduler] Failed to auto-share pre-opinions for meeting {}: {}",
                        meeting.getId(), e.getMessage());
            }
        }

        log.info("[Scheduler] Auto-shared pre-opinions for {} meetings: answers={}, users={}, reviews={}",
                todayMeetings.size(), submittedAnswerCount, submittedUserCount, appliedReviewCount);
    }
}
