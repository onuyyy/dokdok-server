package com.dokdok.retrospective.controller;

import com.dokdok.global.response.ApiResponse;
import com.dokdok.global.response.CursorResponse;
import com.dokdok.retrospective.api.MeetingRetrospectiveApi;
import com.dokdok.retrospective.dto.request.MeetingRetrospectiveRequest;
import com.dokdok.retrospective.dto.response.CollectedAnswersCursor;
import com.dokdok.retrospective.dto.response.CommentCursor;
import com.dokdok.retrospective.dto.response.MeetingRetrospectiveResponse;
import com.dokdok.retrospective.dto.response.MemberAnswerResponse;
import com.dokdok.retrospective.service.MeetingRetrospectiveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meetings/{meetingId}/retrospectives")
public class MeetingRetrospectiveController implements MeetingRetrospectiveApi {

    private final MeetingRetrospectiveService meetingRetrospectiveService;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<MeetingRetrospectiveResponse>> getMeetingRetrospective(@PathVariable Long meetingId) {
        MeetingRetrospectiveResponse response = meetingRetrospectiveService.getMeetingRetrospective(meetingId);

        return ApiResponse.success(response,"공동 회고 조회 성공");
    }

    @Override
    @GetMapping("/comments")
    public ResponseEntity<ApiResponse<CursorResponse<MeetingRetrospectiveResponse.CommentResponse, CommentCursor>>> getTopicComments(
            @PathVariable Long meetingId,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cursorCreatedAt,
            @RequestParam(required = false) Long cursorCommentId
    ) {
        CursorResponse<MeetingRetrospectiveResponse.CommentResponse, CommentCursor> response =
                meetingRetrospectiveService.getComments(
                        meetingId, pageSize, cursorCreatedAt, cursorCommentId
                );
        return ApiResponse.success(response, "코멘트 조회 성공");
    }

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<MeetingRetrospectiveResponse.CommentResponse>> createMeetingRetrospective(
            @PathVariable Long meetingId,
            @Valid @RequestBody MeetingRetrospectiveRequest request
            ) {
        MeetingRetrospectiveResponse.CommentResponse response = meetingRetrospectiveService.createMeetingRetrospective(meetingId, request);

        return ApiResponse.created(response, "공동 회고 작성 완료");
    }

    @Override
    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteMeetingRetrospective(
            @PathVariable Long meetingId,
            @PathVariable Long commentId
    ) {
        meetingRetrospectiveService.deleteMeetingRetrospective(meetingId, commentId);

        return ApiResponse.deleted("공동 회고 코멘트 삭제 완료");
    }

    @Override
    @GetMapping("/collected-answers")
    public ResponseEntity<ApiResponse<CursorResponse<MemberAnswerResponse, CollectedAnswersCursor>>> getCollectedAnswers(
            @PathVariable Long meetingId,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long cursorUserId
    ) {
        CursorResponse<MemberAnswerResponse, CollectedAnswersCursor> response =
                meetingRetrospectiveService.getCollectedAnswers(meetingId, pageSize, cursorUserId);
        return ApiResponse.success(response, "수집된 사전 의견 조회 성공");
    }
}
