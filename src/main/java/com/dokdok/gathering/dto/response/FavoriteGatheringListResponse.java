package com.dokdok.gathering.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Schema(description = "즐겨찾기 모임 목록 응답")
@Builder
public record FavoriteGatheringListResponse(
        @Schema(description = "즐겨찾기 모임 목록")
        List<GatheringListItemResponse> gatherings
) {
    public static FavoriteGatheringListResponse from(
            List<GatheringListItemResponse> favoriteGatherings
    ){
        return FavoriteGatheringListResponse.builder()
                .gatherings(favoriteGatherings)
                .build();
    }
}
