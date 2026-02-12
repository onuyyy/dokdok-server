package com.dokdok.gathering.controller;

import com.dokdok.gathering.api.GatheringApi;
import com.dokdok.gathering.dto.request.GatheringCreateRequest;
import com.dokdok.gathering.dto.request.JoinGatheringMemberRequest;
import com.dokdok.gathering.dto.response.*;
import com.dokdok.gathering.dto.request.GatheringUpdateRequest;
import com.dokdok.gathering.entity.GatheringMemberStatus;
import com.dokdok.gathering.service.GatheringService;
import com.dokdok.global.response.ApiResponse;
import com.dokdok.global.response.CursorResponse;
import com.dokdok.global.response.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/gatherings")
@RequiredArgsConstructor
@Validated
public class GatheringController implements GatheringApi {

    private final GatheringService gatheringService;

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<GatheringCreateResponse>> createGathering(
            @Valid @RequestBody GatheringCreateRequest request
    ) {
        GatheringCreateResponse response = gatheringService.createGathering(request);
        return ApiResponse.created(response, "모임 생성에 성공하였습니다.");
    }

    @Override
    @GetMapping("/join-request/{invitationLink}")
    public ResponseEntity<ApiResponse<GatheringCreateResponse>> joinGatheringInfo(
            @PathVariable("invitationLink") @NotBlank(message = "초대링크는 필수입니다") String invitationLink
    ) {
        GatheringCreateResponse response = gatheringService.getJoinGatheringInfo(invitationLink);
        return ApiResponse.success(response);
    }

    @Override
    @PostMapping("/join-request/{invitationLink}")
    public ResponseEntity<ApiResponse<GatheringJoinResponse>> joinGathering(
            @PathVariable("invitationLink") @NotBlank(message = "초대링크는 필수입니다") String invitationLink
    ) {
        GatheringJoinResponse response = gatheringService.joinGathering(invitationLink);
        return ApiResponse.success(response);
    }

    @Override
    @PatchMapping("/{gatheringId}/join-requests/{memberId}")
    public ResponseEntity<ApiResponse<Void>> handleJoinRequest(
            @PathVariable("gatheringId") Long gatheringId,
            @PathVariable("memberId") Long memberId,
            @Valid @RequestBody JoinGatheringMemberRequest request) {

        gatheringService.handleJoinRequest(gatheringId, memberId, request);
        return ApiResponse.success("해당 멤버가 " + request.approve_type().getDescription() + " 되었습니다.");
    }

    @Override
    @GetMapping("/favorites")
    public ResponseEntity<ApiResponse<FavoriteGatheringListResponse>> getFavoriteGatherings() {
        FavoriteGatheringListResponse response = gatheringService.getFavoriteGatherings();

        return ApiResponse.success(response, "즐겨찾기 모임 리스트 조회 성공");
    }

    @Override
    @GetMapping("/{gatheringId}")
    public ResponseEntity<ApiResponse<GatheringDetailResponse>> getGatheringDetail(@PathVariable Long gatheringId){
        GatheringDetailResponse response = gatheringService.getGatheringDetail(gatheringId);

        return ApiResponse.success(response,"모임 상세정보 조회 성공");
    }

    @Override
    @PatchMapping("/{gatheringId}")
    public ResponseEntity<ApiResponse<GatheringUpdateResponse>> updateGathering(
            @PathVariable Long gatheringId,
            @Valid @RequestBody GatheringUpdateRequest request
    ) {
        GatheringUpdateResponse response = gatheringService.updateGathering(gatheringId, request);
        return ApiResponse.updated(response, "모임 정보 수정 성공");
    }

    @Override
    @DeleteMapping("/{gatheringId}")
    public ResponseEntity<ApiResponse<Void>> deleteGathering(@PathVariable Long gatheringId) {
        gatheringService.deleteGathering(gatheringId);
        return ApiResponse.deleted("모임 삭제 성공");
    }

    @DeleteMapping("/{gatheringId}/members/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable Long gatheringId,
            @PathVariable Long userId
    ){
        gatheringService.removeMember(gatheringId,userId);
        return ApiResponse.deleted("모임원 강퇴 성공");
    }

    @Override
    @PatchMapping("/{gatheringId}/favorites")
    public ResponseEntity<ApiResponse<Void>> updateFavorite(@PathVariable Long gatheringId){
        gatheringService.updateFavorite(gatheringId);
        return ApiResponse.success("모임의 즐겨찾기 상태변경 성공");
    }

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<CursorResponse<GatheringListItemResponse, MyGatheringCursor>>> getMyGatherings(
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cursorJoinedAt,
            @RequestParam(required = false) Long cursorId
    ) {
        CursorResponse<GatheringListItemResponse, MyGatheringCursor> response = gatheringService.getMyGatherings(pageSize, cursorJoinedAt, cursorId);
        return ApiResponse.success(response, "내 모임 전체 목록 조회 성공");
    }

    @Override
    @GetMapping("/{gatheringId}/books")
    public ResponseEntity<ApiResponse<PageResponse<GatheringBookListResponse>>> getGatheringBooks(
            @PathVariable Long gatheringId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageResponse<GatheringBookListResponse> response = gatheringService.getGatheringBooks(gatheringId, page, size);
        return ApiResponse.success(response, "모임 책장 조회를 성공했습니다.");
    }

    @Override
    @GetMapping("/{gatheringId}/members")
    public ResponseEntity<ApiResponse<CursorResponse<GatheringMemberResponse, GatheringMemberCursor>>> getGatheringMembers(
            @PathVariable Long gatheringId,
            @RequestParam GatheringMemberStatus status,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long cursorId
    ){
        CursorResponse<GatheringMemberResponse, GatheringMemberCursor> response = gatheringService.getGatheringMembers(gatheringId, status, pageSize, cursorId);
        return ApiResponse.success(response,"모임 멤버 관리 조회 성공");
    }
}
