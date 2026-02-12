package com.dokdok.meeting.dto;

import com.dokdok.meeting.entity.MeetingLocation;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "장소 정보")
public record MeetingLocationDto(
        @Schema(description = "장소명", example = "강남 스터디룸 A")
        String name,

        @Schema(description = "주소", example = "서울 강남구 ...")
        String address,

        @Schema(description = "위도", example = "37.4979")
        Double latitude,

        @Schema(description = "경도", example = "127.0276")
        Double longitude
) {
    public static MeetingLocationDto from(MeetingLocation location) {
        if (location == null) {
            return null;
        }
        return new MeetingLocationDto(
                location.getName(),
                location.getAddress(),
                location.getLatitude(),
                location.getLongitude()
        );
    }

    public MeetingLocation toEntity() {
        return MeetingLocation.builder()
                .name(name)
                .address(address)
                .latitude(latitude)
                .longitude(longitude)
                .build();
    }
}
