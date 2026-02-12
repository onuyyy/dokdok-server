package com.dokdok.meeting.controller;

import com.dokdok.global.response.ApiResponse;
import com.dokdok.meeting.api.MeetingApi;
import com.dokdok.meeting.dto.MeetingCreateRequest;
import com.dokdok.meeting.dto.MeetingDetailResponse;
import com.dokdok.meeting.dto.MeetingResponse;
import com.dokdok.meeting.dto.MeetingStatusResponse;
import com.dokdok.meeting.dto.MeetingTabCountsResponse;
import com.dokdok.meeting.dto.MeetingUpdateRequest;
import com.dokdok.meeting.dto.MeetingUpdateResponse;
import com.dokdok.meeting.service.MeetingService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meetings")
public class MeetingController implements MeetingApi {

    private final MeetingService meetingService;

    @Override
    @GetMapping(value = "/{meetingId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<MeetingDetailResponse>> findMeeting(
            @PathVariable Long meetingId
    ) {
        return ApiResponse.success(meetingService.findMeeting(meetingId), "약속 상세 조회에 성공했습니다.");
    }

    @Override
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<MeetingResponse>> createMeeting(
            @Valid @RequestBody MeetingCreateRequest request
    ) {
        return ApiResponse.created(meetingService.createMeeting(request), "약속 생성 요청에 성공했습니다.");
    }

    @Override
    @PostMapping(value = "/{meetingId}/confirm", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<MeetingStatusResponse>> confirmMeeting(
            @PathVariable Long meetingId
    ) {
        MeetingStatusResponse response = meetingService.confirmMeeting(meetingId);
        return ApiResponse.updated(response, "약속 확정에 성공했습니다.");
    }

    @Override
    @PostMapping(value = "/{meetingId}/reject", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<MeetingStatusResponse>> rejectMeeting(
            @PathVariable Long meetingId
    ) {
        MeetingStatusResponse response = meetingService.rejectMeeting(meetingId);
        return ApiResponse.updated(response, "약속 거절에 성공했습니다.");
    }

    @Override
    @PostMapping(value = "/{meetingId}/join", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Long>> joinMeeting(
            @PathVariable Long meetingId
    ) {
        Long response = meetingService.joinMeeting(meetingId);
        return ApiResponse.success(response, "약속 참가 신청에 성공했습니다.");
    }

    @Override
    @DeleteMapping(value = "/{meetingId}/cancel", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Long>> cancelMeeting(
            @PathVariable Long meetingId
    ) {
        Long response = meetingService.cancelMeeting(meetingId);
        return ApiResponse.success(response, "약속 참가 취소에 성공했습니다.");
    }

    @Override
    @PatchMapping(value = "/{meetingId}", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<MeetingUpdateResponse>> updateMeeting(
            @PathVariable Long meetingId,
            @RequestBody @Valid MeetingUpdateRequest request
    ) {
        MeetingUpdateResponse response = meetingService.updateMeeting(meetingId, request);
        return ApiResponse.updated(response, "약속 수정에 성공했습니다.");
    }

    @Override
    @DeleteMapping(value = "/{meetingId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> deleteMeeting(
            @PathVariable Long meetingId
    ) {
        meetingService.deleteMeeting(meetingId);
        return ApiResponse.deleted("약속 삭제에 성공했습니다.");
    }

    @Override
    @GetMapping(value = "/tab-counts", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<MeetingTabCountsResponse>> getMeetingTabCounts(
            @RequestParam Long gatheringId
    ) {
        MeetingTabCountsResponse response = meetingService.getMeetingTabCounts(gatheringId);
        return ApiResponse.success(response, "약속 탭 카운트 조회에 성공했습니다.");
    }
}
