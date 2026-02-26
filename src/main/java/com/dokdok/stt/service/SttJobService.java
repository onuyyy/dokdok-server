package com.dokdok.stt.service;

import com.dokdok.ai.client.AiSttClient;
import com.dokdok.ai.dto.SttRequest;
import com.dokdok.global.exception.GlobalErrorCode;
import com.dokdok.global.exception.GlobalException;
import com.dokdok.gathering.service.GatheringValidator;
import com.dokdok.global.response.ApiResponse;
import com.dokdok.global.util.SecurityUtil;
import com.dokdok.meeting.entity.Meeting;
import com.dokdok.meeting.service.MeetingValidator;
import com.dokdok.retrospective.dto.response.RetrospectiveSummaryResponse;
import com.dokdok.retrospective.entity.TopicRetrospectiveSummary;
import com.dokdok.retrospective.repository.TopicRetrospectiveSummaryRepository;
import com.dokdok.stt.dto.SttJobResponse;
import com.dokdok.stt.entity.SttJob;
import com.dokdok.stt.entity.SttJobStatus;
import com.dokdok.stt.entity.SttSummary;
import com.dokdok.stt.repository.SttJobRepository;
import com.dokdok.stt.repository.SttSummaryRepository;
import com.dokdok.topic.entity.Topic;
import com.dokdok.topic.entity.TopicAnswer;
import com.dokdok.topic.repository.TopicRepository;
import com.dokdok.topic.repository.TopicAnswerRepository;
import com.dokdok.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SttJobService {

    private static final long MAX_FILE_SIZE = 50L * 1024L * 1024L;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "mp3", "aac", "ac3", "ogg", "flac", "wav", "m4a"
    );

    private final MeetingValidator meetingValidator;
    private final GatheringValidator gatheringValidator;
    private final SttJobRepository sttJobRepository;
    private final SttSummaryRepository sttSummaryRepository;
    private final TopicRepository topicRepository;
    private final TopicRetrospectiveSummaryRepository topicRetrospectiveSummaryRepository;
    private final TopicAnswerRepository topicAnswerRepository;
    private final AiSttClient aiSttClient;

    @Value("${stt.temp-dir:}")
    private String tempDirProperty;

    @Transactional
    public SttJobResponse createJob(Long gatheringId, Long meetingId, MultipartFile file) {
        Long userId = SecurityUtil.getCurrentUserId();
        gatheringValidator.validateMembership(gatheringId, userId);
        meetingValidator.validateMeetingInGathering(meetingId, gatheringId);
        meetingValidator.validateMeeting(meetingId);
        meetingValidator.validateMeetingMember(meetingId, userId);

        Meeting meeting = meetingValidator.findMeetingOrThrow(meetingId);
        User user = SecurityUtil.getCurrentUserEntity();

        if (file != null && file.isEmpty()) {
            throw new GlobalException(GlobalErrorCode.INVALID_INPUT_VALUE);
        }

        List<SttRequest.PreAnswer> preAnswers = buildPreAnswers(meetingId);
        if (file == null && preAnswers.isEmpty()) {
            throw new GlobalException(GlobalErrorCode.INVALID_INPUT_VALUE);
        }

        Path tempFilePath = null;
        if (file != null) {
            validateFile(file);
            tempFilePath = saveToTemp(file);
        }
        SttJob job = SttJob.builder()
                .meeting(meeting)
                .user(user)
                .originalFilename(file != null ? file.getOriginalFilename() : null)
                .contentType(file != null ? file.getContentType() : null)
                .fileSize(file != null ? file.getSize() : null)
                .tempFilePath(tempFilePath != null ? tempFilePath.toString() : null)
                .status(SttJobStatus.PROCESSING)
                .build();
        sttJobRepository.save(job);

        SttSummary summary = null;
        try {
            log.info("STT pre-answers count: {}", preAnswers.size());
            ApiResponse<RetrospectiveSummaryResponse> apiResponse = aiSttClient.requestStt(
                    new SttRequest(
                            job.getId(),
                            meetingId,
                            tempFilePath != null ? tempFilePath.toString() : null,
                            "ko-KR",
                            preAnswers
                    )
            );
            RetrospectiveSummaryResponse response = apiResponse != null ? apiResponse.data() : null;
            if (apiResponse == null) {
                job.markFailed("STT response is empty");
            } else if (!"SUCCESS".equalsIgnoreCase(apiResponse.code())) {
                String message = apiResponse.message() == null ? "STT failed" : apiResponse.message();
                job.markFailed(message);
            } else if (response == null) {
                job.markFailed("STT response is empty");
            } else {
                int topicCount = response.topics() == null ? 0 : response.topics().size();
                List<Long> topicIds = response.topics() == null
                        ? List.of()
                        : response.topics().stream().map(RetrospectiveSummaryResponse.TopicSummaryResponse::topicId).toList();
                log.info("STT summary topics count: {}, topicIds={}", topicCount, topicIds);
                job.markDone();
                summary = saveSummary(job, response);
                saveRetrospectiveSummaries(meetingId, response);
            }
        } catch (WebClientResponseException e) {
            job.markFailed("AI STT error: " + e.getStatusCode());
            log.error(
                    "AI STT request failed: status={}, body={}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString(),
                    e
            );
        } catch (Exception e) {
            job.markFailed("AI STT error");
            log.error("AI STT request failed", e);
        } finally {
            deleteTempFile(tempFilePath);
        }

        return SttJobResponse.from(job, summary);
    }

    @Transactional(readOnly = true)
    public SttJobResponse getJob(Long gatheringId, Long meetingId, Long jobId) {
        Long userId = SecurityUtil.getCurrentUserId();
        gatheringValidator.validateMembership(gatheringId, userId);
        meetingValidator.validateMeetingInGathering(meetingId, gatheringId);
        meetingValidator.validateMeeting(meetingId);
        meetingValidator.validateMeetingMember(meetingId, userId);

        SttJob job = sttJobRepository.findByIdAndMeetingId(jobId, meetingId)
                .orElseThrow(() -> new GlobalException(GlobalErrorCode.INVALID_INPUT_VALUE));

        SttSummary summary = sttSummaryRepository.findBySttJobId(jobId).orElse(null);
        return SttJobResponse.from(job, summary);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new GlobalException(GlobalErrorCode.INVALID_INPUT_VALUE);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new GlobalException(GlobalErrorCode.FILE_SIZE_EXCEEDED);
        }
        String originalFilename = file.getOriginalFilename();
        String extension = extractExtension(originalFilename);
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new GlobalException(GlobalErrorCode.INVALID_FILE_TYPE);
        }
    }

    private String extractExtension(String filename) {
        if (filename == null) {
            return null;
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0 || lastDot == filename.length() - 1) {
            return null;
        }
        return filename.substring(lastDot + 1).toLowerCase();
    }

    private Path saveToTemp(MultipartFile file) {
        String tempRoot = tempDirProperty == null || tempDirProperty.isBlank()
                ? System.getProperty("java.io.tmpdir")
                : tempDirProperty;
        Path tempDir = Paths.get(tempRoot, "dokdok-stt");
        try {
            Files.createDirectories(tempDir);
            String safeName = UUID.randomUUID() + "_" + sanitizeFilename(file.getOriginalFilename());
            Path tempFilePath = tempDir.resolve(safeName);
            file.transferTo(tempFilePath.toFile());
            return tempFilePath;
        } catch (IOException e) {
            throw new GlobalException(GlobalErrorCode.FILE_UPLOAD_FAILED, e);
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "audio";
        }
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private void deleteTempFile(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Failed to delete temp STT file: {}", path, e);
        }
    }

    private SttSummary saveSummary(SttJob job, RetrospectiveSummaryResponse response) {
        RetrospectiveSummaryResponse.TopicSummaryResponse topicSummary = extractTopicSummary(response);
        if (topicSummary == null) {
            return null;
        }
        String summaryText = topicSummary.summary();
        List<String> highlights = topicSummary.keyPoints() == null
                ? null
                : topicSummary.keyPoints().stream()
                .map(RetrospectiveSummaryResponse.KeyPointResponse::title)
                .toList();

        if ((summaryText == null || summaryText.isBlank())
                && (highlights == null || highlights.isEmpty())) {
            return null;
        }

        SttSummary summary = SttSummary.builder()
                .sttJob(job)
                .summary(summaryText)
                .highlights(highlights)
                .build();
        return sttSummaryRepository.save(summary);
    }

    private RetrospectiveSummaryResponse.TopicSummaryResponse extractTopicSummary(
            RetrospectiveSummaryResponse response
    ) {
        if (response.topics() == null || response.topics().isEmpty()) {
            return null;
        }
        return response.topics().get(0);
    }

    private void saveRetrospectiveSummaries(
            Long meetingId,
            RetrospectiveSummaryResponse response
    ) {
        if (response.topics() == null || response.topics().isEmpty()) {
            return;
        }

        RetrospectiveSummaryResponse.TopicSummaryResponse baseSummary = response.topics().stream()
                .filter(topic -> topic.topicId() == null)
                .findFirst()
                .orElse(null);
        if (baseSummary != null) {
            List<Topic> topics = topicRepository.findConfirmedTopics(meetingId);
            for (Topic topic : topics) {
                upsertTopicSummary(topic, baseSummary);
            }
            return;
        }

        for (RetrospectiveSummaryResponse.TopicSummaryResponse topicResponse : response.topics()) {
            Long topicId = topicResponse.topicId();
            if (topicId == null) {
                continue;
            }
            Topic topic = topicRepository.findById(topicId).orElse(null);
            if (topic == null || !meetingId.equals(topic.getMeeting().getId())) {
                continue;
            }
            upsertTopicSummary(topic, topicResponse);
        }
    }

    private void upsertTopicSummary(
            Topic topic,
            RetrospectiveSummaryResponse.TopicSummaryResponse topicResponse
    ) {
        List<TopicRetrospectiveSummary.KeyPoint> keyPoints = topicResponse.keyPoints() == null
                ? List.of()
                : topicResponse.keyPoints().stream()
                .map(kp -> new TopicRetrospectiveSummary.KeyPoint(kp.title(), kp.details()))
                .toList();
        TopicRetrospectiveSummary summary = topicRetrospectiveSummaryRepository
                .findByTopicId(topic.getId())
                .orElseGet(() -> TopicRetrospectiveSummary.builder()
                        .topic(topic)
                        .build());
        summary.update(topicResponse.summary(), keyPoints);
        topicRetrospectiveSummaryRepository.save(summary);
    }

    private List<SttRequest.PreAnswer> buildPreAnswers(Long meetingId) {
        List<TopicAnswer> answers = topicAnswerRepository.findByMeetingId(meetingId);
        return answers.stream()
                .filter(answer -> answer.getContent() != null && !answer.getContent().isBlank())
                .map(answer -> new SttRequest.PreAnswer(
                        answer.getTopic() != null ? answer.getTopic().getId() : null,
                        answer.getTopic() != null ? answer.getTopic().getTitle() : null,
                        answer.getUser() != null ? answer.getUser().getId() : null,
                        answer.getContent()
                ))
                .toList();
    }
}
