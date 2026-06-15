package com.dokdok.book.controller;

import com.dokdok.book.api.PersonalBookRecordApi;
import com.dokdok.book.dto.request.PersonalReadingRecordCreateRequest;
import com.dokdok.book.dto.request.PersonalReadingRecordUpdateRequest;
import com.dokdok.book.dto.request.PreOpinionTimeType;
import com.dokdok.book.dto.request.TimelineSortType;
import com.dokdok.book.dto.response.*;
import java.util.List;
import com.dokdok.book.entity.RecordType;
import com.dokdok.book.service.PersonalReadingRecordService;
import com.dokdok.book.service.ReadingTimelineService;
import com.dokdok.global.response.CursorResponse;
import com.dokdok.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/book")
public class PersonalBookRecordController implements PersonalBookRecordApi {

    private final PersonalReadingRecordService personalReadingRecordService;
    private final ReadingTimelineService readingTimelineService;

    @Override
    @PostMapping("/{personalBookId}/records")
    public ResponseEntity<ApiResponse<PersonalReadingRecordCreateResponse>> createMyReadingRecord(@PathVariable Long personalBookId, @RequestBody PersonalReadingRecordCreateRequest request) {
        PersonalReadingRecordCreateResponse response = personalReadingRecordService.create(personalBookId, request);
        return ApiResponse.created(response, "기록 등록 성공");
    }

    @Override
    @PatchMapping("/{personalBookId}/records/{recordId}")
    public ResponseEntity<ApiResponse<PersonalReadingRecordCreateResponse>> updateMyReadingRecord(@PathVariable Long personalBookId, @PathVariable Long recordId, @RequestBody PersonalReadingRecordUpdateRequest request) {
        PersonalReadingRecordCreateResponse response = personalReadingRecordService.update(personalBookId, recordId, request);
        return ApiResponse.success(response, "기록 수정 성공");
    }

    @Override
    @DeleteMapping("/{personalBookId}/records/{recordId}")
    public ResponseEntity<ApiResponse<Void>> deleteMyReadingRecord(@PathVariable Long personalBookId, @PathVariable Long recordId) {
        personalReadingRecordService.delete(personalBookId, recordId);
        return ApiResponse.deleted("기록 삭제 성공");
    }

    @Override
    @GetMapping("/{personalBookId}/records")
    public ResponseEntity<ApiResponse<CursorPageResponse<PersonalReadingRecordListResponse, ReadingRecordCursor>>> getMyReadingRecords(
            @PathVariable Long personalBookId,
            @RequestParam(required = false) Long gatheringId,
            @RequestParam(required = false) RecordType recordType,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime cursorCreatedAt,
            @RequestParam(required = false) Long cursorRecordId,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false, defaultValue = "DESC") Sort.Direction sort
    ) {
        CursorPageResponse<PersonalReadingRecordListResponse, ReadingRecordCursor> response =
                personalReadingRecordService.getRecords(personalBookId, gatheringId, recordType, cursorCreatedAt, cursorRecordId, size, sort);
        return ApiResponse.success(response, "기록 조회 성공");

    }

    @Override
    @GetMapping("/{personalBookId}/records/topic-answer")
    public ResponseEntity<ApiResponse<PersonalReadingTopicAnswerResponse>> getMyTopicAnswer(
            @PathVariable Long personalBookId
    ) {
        PersonalReadingTopicAnswerResponse response = personalReadingRecordService.getTopicAnswers(personalBookId);
        return ApiResponse.success(response, "사전 의견 조회 성공");
    }

    @Override
    @GetMapping("/{personalBookId}/gatherings")
    public ResponseEntity<ApiResponse<List<PersonalBookGatheringResponse>>> getGatheringsForBook(
            @PathVariable Long personalBookId) {
        return ApiResponse.success(
                personalReadingRecordService.getGatheringsForBook(personalBookId),
                "책에 연결된 모임 목록 조회 성공"
        );
    }

    @Override
    @GetMapping("/{personalBookId}/records/timeline")
    public ResponseEntity<ApiResponse<CursorResponse<ReadingTimelineItem, ReadingTimelineCursor>>> getMyReadingTimeline(
            @PathVariable Long personalBookId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime cursorEventAt,
            @RequestParam(required = false) ReadingTimelineType cursorType,
            @RequestParam(required = false) Long cursorSourceId,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false, defaultValue = "ANSWER_CREATED") PreOpinionTimeType preOpinionTime,
            @RequestParam(required = false) Long gatheringId,
            @RequestParam(required = false) RecordType recordType,
            @RequestParam(required = false, defaultValue = "DESC") TimelineSortType sort
    ) {
        CursorResponse<ReadingTimelineItem, ReadingTimelineCursor> response =
                readingTimelineService.getTimeline(
                        personalBookId,
                        cursorEventAt,
                        cursorType,
                        cursorSourceId,
                        size,
                        preOpinionTime,
                        gatheringId,
                        recordType,
                        sort
                );
        return ApiResponse.success(response, "독서 타임라인 조회 성공");
    }
}
