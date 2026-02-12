package com.dokdok.meeting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MeetingLocation {

    @Column(name = "location_name", length = 255)
    private String name;

    @Column(name = "location_address", length = 255)
    private String address;

    @Column(name = "location_latitude")
    private Double latitude;

    @Column(name = "location_longitude")
    private Double longitude;
}
