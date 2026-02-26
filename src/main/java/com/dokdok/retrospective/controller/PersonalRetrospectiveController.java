package com.dokdok.retrospective.controller;

import com.dokdok.global.response.ApiResponse;
import com.dokdok.retrospective.api.PersonalRetrospectiveApi;
import com.dokdok.retrospective.dto.request.PersonalRetrospectiveRequest;
import com.dokdok.retrospective.dto.response.PersonalRetrospectiveDetailResponse;
import com.dokdok.retrospective.dto.response.PersonalRetrospectiveEditResponse;
import com.dokdok.retrospective.dto.response.PersonalRetrospectiveFormResponse;
import com.dokdok.retrospective.dto.response.PersonalRetrospectiveResponse;
import com.dokdok.retrospective.service.PersonalRetrospectiveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meetings/{meetingId}/retrospectives/personal")
public class PersonalRetrospectiveController implements PersonalRetrospectiveApi {

    private final PersonalRetrospectiveService personalRetrospectiveService;

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<PersonalRetrospectiveResponse>> writePersonalRetrospective(
            @PathVariable Long meetingId,
            @Valid @RequestBody PersonalRetrospectiveRequest request
    ) {
        PersonalRetrospectiveResponse response
                = personalRetrospectiveService.createPersonalRetrospective(meetingId, request);

        return ApiResponse.created(response, "개인 회고 저장 완료했습니다.");
    }

    @Override
    @GetMapping("/form")
    public ResponseEntity<ApiResponse<PersonalRetrospectiveFormResponse>> getPersonalRetrospectiveForm(
            @PathVariable Long meetingId
    ) {
        PersonalRetrospectiveFormResponse response
                = personalRetrospectiveService.getPersonalRetrospectiveForm(meetingId);

        return ApiResponse.success(response, "개인 회고 입력 폼 조회를 성공했습니다.");
    }


    @Override
    @GetMapping("/form/edit")
    public ResponseEntity<ApiResponse<PersonalRetrospectiveEditResponse>> getPersonalRetrospectiveEditForm(
            @PathVariable Long meetingId
    ) {
        PersonalRetrospectiveEditResponse response
                = personalRetrospectiveService.getPersonalRetrospectiveEditForm(meetingId);

        return ApiResponse.success(response, "개인 회고 수정 폼 조회를 성공했습니다.");
    }

    @Override
    @PutMapping
    public ResponseEntity<ApiResponse<PersonalRetrospectiveResponse>> editPersonalRetrospective(
            @PathVariable Long meetingId,
            @Valid @RequestBody PersonalRetrospectiveRequest request
    ) {
        PersonalRetrospectiveResponse response =
                personalRetrospectiveService.editPersonalRetrospective(meetingId, request);

        return ApiResponse.success(response, "개인 회고 수정을 성공했습니다.");
    }

    @Override
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deletePersonalRetrospective(
            @PathVariable Long meetingId
    ) {
        personalRetrospectiveService.deletePersonalRetrospective(meetingId);

        return ApiResponse.deleted("개인 회고 삭제를 성공했습니다.");
    }

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<PersonalRetrospectiveDetailResponse>> getPersonalRetrospective(
            @PathVariable Long meetingId
    ) {
        PersonalRetrospectiveDetailResponse response
                = personalRetrospectiveService.getPersonalRetrospective(meetingId);

        return ApiResponse.success(response, "개인 회고 조회를 성공했습니다.");
    }
}