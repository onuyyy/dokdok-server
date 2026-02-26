package com.dokdok.retrospective.controller;

import com.dokdok.global.response.ApiResponse;
import com.dokdok.retrospective.api.RetrospectiveSummaryApi;
import com.dokdok.retrospective.dto.request.RetrospectiveSummaryUpdateRequest;
import com.dokdok.retrospective.dto.response.RetrospectiveSummaryResponse;
import com.dokdok.retrospective.service.RetrospectiveSummaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meetings/{meetingId}/retrospectives/summary")
public class RetrospectiveSummaryController implements RetrospectiveSummaryApi {

    private final RetrospectiveSummaryService retrospectiveSummaryService;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<RetrospectiveSummaryResponse>> getRetrospectiveSummary(
            @PathVariable Long meetingId
    ) {
        RetrospectiveSummaryResponse response = retrospectiveSummaryService.getRetrospectiveSummary(meetingId);

        return ApiResponse.success(response, "AI 요약 조회 성공");
    }

    @Override
    @PatchMapping
    public ResponseEntity<ApiResponse<RetrospectiveSummaryResponse>> updateRetrospectiveSummary(
            @PathVariable Long meetingId,
            @Valid @RequestBody RetrospectiveSummaryUpdateRequest request
    ) {
        RetrospectiveSummaryResponse response = retrospectiveSummaryService.updateRetrospectiveSummary(meetingId, request);

        return ApiResponse.success(response, "AI 요약 수정 성공");
    }

    @Override
    @PostMapping("/publish")
    public ResponseEntity<ApiResponse<RetrospectiveSummaryResponse>> publishRetrospective(
            @PathVariable Long meetingId
    ) {
        RetrospectiveSummaryResponse response = retrospectiveSummaryService.publishRetrospective(meetingId);

        return ApiResponse.created(response, "약속 회고 생성 성공");
    }
}
