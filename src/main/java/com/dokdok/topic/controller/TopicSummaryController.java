package com.dokdok.topic.controller;

import com.dokdok.global.response.ApiResponse;
import com.dokdok.topic.api.TopicSummaryApi;
import com.dokdok.topic.dto.response.TopicSummaryResponse;
import com.dokdok.topic.service.TopicSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TopicSummaryController implements TopicSummaryApi {

    private final TopicSummaryService topicSummaryService;

    @Override
    public ResponseEntity<ApiResponse<TopicSummaryResponse>> requestTopicSummary(
            Long gatheringId,
            Long meetingId,
            Long topicId
    ) {
        String response = topicSummaryService.requestTopicSummary(gatheringId, meetingId, topicId);

        return ApiResponse.success(TopicSummaryResponse.from(response), "AI 요약 요청이 전달되었습니다.");
    }
}
