package com.dokdok.meeting.controller;

import com.dokdok.global.response.ApiResponse;
import com.dokdok.global.response.PageResponse;
import com.dokdok.meeting.api.MeetingListApi;
import com.dokdok.meeting.dto.MeetingListFilter;
import com.dokdok.meeting.dto.MeetingListItemResponse;
import com.dokdok.meeting.entity.MeetingStatus;
import com.dokdok.meeting.service.MeetingService;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/gatherings/{gatheringId}/meetings")
public class MeetingListController implements MeetingListApi {

    private final MeetingService meetingService;

    @Override
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PageResponse<MeetingListItemResponse>>> getMeetingList(
            @PathVariable Long gatheringId,
            @RequestParam MeetingListFilter filter,
            @ParameterObject
            @PageableDefault(size = 5, sort = {"meetingStartDate", "id"}) Pageable pageable
    ) {
        PageResponse<MeetingListItemResponse> response =
                meetingService.meetingList(gatheringId, filter, pageable);
        return ApiResponse.success(response, "약속 리스트 조회에 성공했습니다.");
    }

    @Override
    @GetMapping(value = "/approvals", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PageResponse<MeetingListItemResponse>>> getApprovalMeetingList(
            @PathVariable Long gatheringId,
            @RequestParam MeetingStatus status,
            @ParameterObject
            @PageableDefault(size = 10) Pageable pageable
    ) {
        PageResponse<MeetingListItemResponse> response =
                meetingService.getApprovalMeetingList(gatheringId, status, pageable);
        return ApiResponse.success(response, "약속 승인 리스트 조회에 성공했습니다.");
    }
}
