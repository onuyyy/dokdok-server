package com.dokdok.topic.controller;

import com.dokdok.global.response.ApiResponse;
import com.dokdok.topic.api.PreOpinionApi;
import com.dokdok.topic.dto.response.PreOpinionResponse;
import com.dokdok.topic.service.PreOpinionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/gatherings/{gatheringId}/meetings/{meetingId}/answers")
public class PreOpinionController implements PreOpinionApi {

    private final PreOpinionService preOpinionService;

    @Override
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PreOpinionResponse>> findAnswers(
            @PathVariable("gatheringId") Long gatheringId,
            @PathVariable("meetingId") Long meetingId
    ) {
        PreOpinionResponse response = preOpinionService.findPreOpinions(gatheringId, meetingId);
        return ApiResponse.success(response, "약속의 사전 의견 목록 조회를 성공했습니다.");
    }

    @Override
    @DeleteMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> deleteMyAnswer(
            @PathVariable("gatheringId") Long gatheringId,
            @PathVariable("meetingId") Long meetingId
    ) {

        preOpinionService.deleteMyAnswer(gatheringId, meetingId);

        return ApiResponse.deleted("내 사전의견이 삭제되었습니다.");
    }
}
