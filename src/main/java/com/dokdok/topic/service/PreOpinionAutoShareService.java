package com.dokdok.topic.service;

import com.dokdok.meeting.entity.Meeting;
import com.dokdok.topic.entity.TopicAnswer;
import com.dokdok.topic.repository.TopicAnswerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PreOpinionAutoShareService {

    private final TopicAnswerRepository topicAnswerRepository;
    private final PreOpinionBookReviewService preOpinionBookReviewService;

    @Transactional
    public AutoShareResult autoShareDrafts(Meeting meeting) {
        List<TopicAnswer> draftAnswers = topicAnswerRepository.findDraftsByMeetingId(meeting.getId());
        if (draftAnswers.isEmpty()) {
            return AutoShareResult.empty(meeting.getId());
        }

        draftAnswers.forEach(TopicAnswer::submit);

        Set<Long> userIds = draftAnswers.stream()
                .map(answer -> answer.getUser().getId())
                .collect(Collectors.toSet());
        int appliedReviewCount = preOpinionBookReviewService.applyToPersonalBookReviews(meeting.getId(), userIds);

        return new AutoShareResult(meeting.getId(), draftAnswers.size(), userIds.size(), appliedReviewCount);
    }

    public record AutoShareResult(
            Long meetingId,
            int submittedAnswerCount,
            int submittedUserCount,
            int appliedReviewCount
    ) {
        private static AutoShareResult empty(Long meetingId) {
            return new AutoShareResult(meetingId, 0, 0, 0);
        }
    }
}
