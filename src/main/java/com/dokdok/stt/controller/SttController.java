package com.dokdok.stt.controller;

import com.dokdok.global.response.ApiResponse;
import com.dokdok.stt.api.SttApi;
import com.dokdok.stt.dto.SttJobResponse;
import com.dokdok.stt.service.SttJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class SttController implements SttApi {

    private final SttJobService sttJobService;

    @Override
    public ResponseEntity<ApiResponse<SttJobResponse>> createJob(
            Long gatheringId,
            Long meetingId,
            MultipartFile file
    ) {
        SttJobResponse response = sttJobService.createJob(gatheringId, meetingId, file);
        return ApiResponse.created(response, "STT 작업이 생성되었습니다.");
    }

    @Override
    public ResponseEntity<ApiResponse<SttJobResponse>> getJob(Long gatheringId, Long meetingId, Long jobId) {
        SttJobResponse response = sttJobService.getJob(gatheringId, meetingId, jobId);
        return ApiResponse.success(response, "STT 작업 조회를 완료했습니다.");
    }
}
