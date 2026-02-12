package com.dokdok.meeting.controller;

import com.dokdok.global.response.ApiResponse;
import com.dokdok.global.response.CursorResponse;
import com.dokdok.meeting.api.MyMeetingListApi;
import com.dokdok.meeting.dto.MeetingListCursor;
import com.dokdok.meeting.dto.MeetingListCursorRequest;
import com.dokdok.meeting.dto.MyMeetingListFilter;
import com.dokdok.meeting.dto.MyMeetingListItemResponse;
import com.dokdok.meeting.dto.MyMeetingTabCountsResponse;
import com.dokdok.meeting.service.MeetingService;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meetings")
public class MyMeetingListController implements MyMeetingListApi {

    private final MeetingService meetingService;

    @Override
    @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CursorResponse<MyMeetingListItemResponse, MeetingListCursor>>> getMyMeetingList(
            @RequestParam MyMeetingListFilter filter,
            @ParameterObject MeetingListCursorRequest cursor,
            @RequestParam(defaultValue = "4") int size
    ) {
        MeetingListCursor cursorValue = cursor == null ? null : cursor.toCursorOrNull();
        CursorResponse<MyMeetingListItemResponse, MeetingListCursor> response =
                meetingService.getMyMeetingList(filter, size, cursorValue);
        return ApiResponse.success(response, "내 약속 리스트 조회에 성공했습니다.");
    }

    @Override
    @GetMapping(value = "/me/tab-counts", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<MyMeetingTabCountsResponse>> getMyMeetingTabCounts() {
        MyMeetingTabCountsResponse response = meetingService.getMyMeetingTabCounts();
        return ApiResponse.success(response, "내 약속 탭 카운트 조회에 성공했습니다.");
    }
}
