package com.dokdok.topic.controller;

import com.dokdok.global.response.ApiResponse;
import com.dokdok.topic.api.TopicApi;
import com.dokdok.topic.dto.request.ConfirmTopicsRequest;
import com.dokdok.topic.dto.request.SuggestTopicRequest;
import com.dokdok.topic.dto.response.ConfirmTopicsResponse;
import com.dokdok.topic.dto.response.SuggestTopicResponse;
import com.dokdok.topic.dto.response.TopicLikeResponse;
import com.dokdok.topic.dto.response.TopicsWithActionsResponse;
import com.dokdok.topic.entity.TopicMessage;
import com.dokdok.topic.service.TopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/gatherings/{gatheringId}/meetings/{meetingId}/topics")
public class TopicController implements TopicApi {

    private final TopicService topicService;

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<SuggestTopicResponse>> createTopic(
            Long gatheringId,
            Long meetingId,
            SuggestTopicRequest request
    ) {

        SuggestTopicResponse response = topicService.createTopic(gatheringId, meetingId, request);

        return ApiResponse.created(response, "주제 제안이 완료되었습니다.");
    }

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<TopicsWithActionsResponse>> getTopics(
            Long gatheringId,
            Long meetingId,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer cursorLikeCount,
            @RequestParam(required = false) Long cursorTopicId
    ) {

        TopicsWithActionsResponse response = topicService.getTopics(
                gatheringId, meetingId, pageSize, cursorLikeCount, cursorTopicId
        );

        return ApiResponse.success(response, "제안된 주제 조회를 성공했습니다.");
    }

    @Override
    @PatchMapping(value = "/confirm")
    public ResponseEntity<ApiResponse<ConfirmTopicsResponse>> confirmTopics(
            Long gatheringId,
            Long meetingId,
            ConfirmTopicsRequest request
    ) {
        ConfirmTopicsResponse response = topicService.confirmTopics(
                gatheringId, meetingId, request
        );

        return ApiResponse.success(response, "주제가 확정되었습니다.");
    }

    @Override
    @DeleteMapping(value = "/{topicId}")
    public ResponseEntity<ApiResponse<Void>> deleteTopic(
            Long gatheringId,
            Long meetingId,
            Long topicId
    ) {

        topicService.deleteTopic(gatheringId, meetingId, topicId);

        return ApiResponse.deleted("주제가 삭제되었습니다.");
    }

    @Override
    @PostMapping("/{topicId}/likes")
    public ResponseEntity<ApiResponse<TopicLikeResponse>> toggleLike(
            Long gatheringId,
            Long meetingId,
            Long topicId
    ) {

        TopicLikeResponse response = topicService.toggleTopicLike(gatheringId, meetingId, topicId);

        TopicMessage message = response.liked()
                ? TopicMessage.LIKE_SUCCESS
                : TopicMessage.LIKE_CANCEL;

        return ApiResponse.success(response, message.getMessage());
    }

}