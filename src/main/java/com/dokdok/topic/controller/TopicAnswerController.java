package com.dokdok.topic.controller;

import com.dokdok.global.response.ApiResponse;
import com.dokdok.topic.api.TopicAnswerApi;
import com.dokdok.topic.dto.request.TopicAnswerBulkSaveRequest;
import com.dokdok.topic.dto.request.TopicAnswerBulkSubmitRequest;
import com.dokdok.topic.dto.response.PreOpinionSaveResponse;
import com.dokdok.topic.dto.response.PreOpinionSubmitResponse;
import com.dokdok.topic.dto.response.TopicAnswerDetailResponse;
import com.dokdok.topic.service.TopicAnswerService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/gatherings/{gatheringId}/meetings/{meetingId}/answers")
public class TopicAnswerController implements TopicAnswerApi {

    private final TopicAnswerService topicAnswerService;

    @Override
    public ResponseEntity<ApiResponse<PreOpinionSaveResponse>> createAnswer(
            Long gatheringId,
            Long meetingId,
            @Valid @RequestBody TopicAnswerBulkSaveRequest request
    ) {
        PreOpinionSaveResponse response = topicAnswerService.createAnswer(
                gatheringId, meetingId, request
        );

        return ApiResponse.created(response, "사전 의견이 저장되었습니다.");
    }

    @Override
    public ResponseEntity<ApiResponse<TopicAnswerDetailResponse>> findMyAnswer(
            Long gatheringId,
            Long meetingId
    ) {
        TopicAnswerDetailResponse response = topicAnswerService.getMyAnswer(
                gatheringId, meetingId
        );

        return ApiResponse.success(response, "사전 의견 작성 화면 조회를 성공했습니다.");
    }

    @Override
    public ResponseEntity<ApiResponse<PreOpinionSaveResponse>> updateMyAnswer(
            Long gatheringId,
            Long meetingId,
            @Valid @RequestBody TopicAnswerBulkSaveRequest request
    ) {
        PreOpinionSaveResponse response = topicAnswerService.updateMyAnswer(
                gatheringId, meetingId, request
        );

        return ApiResponse.updated(response, "사전 의견이 저장되었습니다.");
    }

    @Override
    public ResponseEntity<ApiResponse<PreOpinionSubmitResponse>> submitMyAnswer(
            Long gatheringId,
            Long meetingId,
            @Valid @RequestBody TopicAnswerBulkSubmitRequest request
    ) {
        PreOpinionSubmitResponse response = topicAnswerService.submitMyAnswer(
                gatheringId, meetingId, request
        );

        return ApiResponse.success(response, "사전 의견이 제출되었습니다.");
    }
}
