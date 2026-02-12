package com.dokdok.book.controller;

import com.dokdok.book.api.BookRecordApi;
import com.dokdok.global.response.ApiResponse;
import com.dokdok.global.response.CursorResponse;
import com.dokdok.retrospective.dto.response.RetrospectiveRecordResponse;
import com.dokdok.retrospective.dto.response.RetrospectiveRecordsCursor;
import com.dokdok.retrospective.service.PersonalRetrospectiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/book/{personalBookId}/records")
public class BookRecordController implements BookRecordApi {

    private final PersonalRetrospectiveService retrospectiveService;

    @Override
    @GetMapping(value = "/retrospectives", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CursorResponse<RetrospectiveRecordResponse, RetrospectiveRecordsCursor>>> getRetrospectiveRecords(
            @PathVariable Long personalBookId,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cursorCreatedAt,
            @RequestParam(required = false) Long cursorRetrospectiveId
    ) {
        CursorResponse<RetrospectiveRecordResponse, RetrospectiveRecordsCursor> response =
                retrospectiveService.getRetrospectiveRecords(personalBookId, pageSize, cursorCreatedAt, cursorRetrospectiveId);

        return ApiResponse.success(response, "해당 책의 개인 회고 목록 조회를 성공했습니다.");
    }
}
