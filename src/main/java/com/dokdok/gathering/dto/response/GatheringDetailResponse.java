package com.dokdok.gathering.dto.response;

import com.dokdok.gathering.entity.Gathering;
import com.dokdok.gathering.entity.GatheringMember;
import com.dokdok.gathering.entity.GatheringRole;
import com.dokdok.gathering.entity.GatheringStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Schema(description = "모임 상세 응답")
@Builder
public record GatheringDetailResponse(
        @Schema(description = "모임 ID", example = "1")
        Long gatheringId,
        @Schema(description = "모임 이름", example = "독서 모임")
        String gatheringName,
        @Schema(description = "모임 설명", example = "매주 함께 책을 읽는 모임입니다.")
        String description,
        @Schema(description = "모임 상태", example = "ACTIVE")
        GatheringStatus gatheringStatus,
        @Schema(description = "즐겨찾기 여부", example = "true")
        Boolean isFavorite,
        @Schema(description = "초대 링크", example = "https://example.com/invite/abc123")
        String invitationLink,
        @Schema(description = "모임 생성 후 경과 일수", example = "30")
        Integer daysFromCreation,
        @Schema(description = "현재 사용자 역할", example = "LEADER")
        GatheringRole currentUserRole,
        @Schema(description = "모임 멤버 목록")
        List<MemberInfo> members,
        @Schema(description = "전체 멤버 수", example = "10")
        Integer totalMembers,
        @Schema(description = "전체 약속 수", example = "5")
        Integer totalMeetings
) {
    public static GatheringDetailResponse from(
            GatheringMember currentMember,
            List<GatheringMember> allMember,
            int totalMeetings,
            Map<Long, String> profileImageUrlMap
    ){
        List<MemberInfo> memberInfoList = allMember.stream()
                .map(member -> MemberInfo.from(member, profileImageUrlMap.get(member.getUser().getId())))
                .toList();

        Gathering gathering = currentMember.getGathering();

        return GatheringDetailResponse.builder()
                .gatheringId(gathering.getId())
                .gatheringName(gathering.getGatheringName())
                .description(gathering.getDescription())
                .gatheringStatus(gathering.getGatheringStatus())
                .isFavorite(currentMember.getIsFavorite())
                .invitationLink(gathering.getInvitationLink())
                .daysFromCreation(gathering.getDaysFromCreation())
                .currentUserRole(currentMember.getRole())
                .members(memberInfoList)
                .totalMembers(allMember.size())
                .totalMeetings(totalMeetings)
                .build();
    }

    @Schema(description = "모임 멤버 정보")
    @Builder
    public record MemberInfo(
            @Schema(description = "모임 멤버 ID", example = "10")
            Long gatheringMemberId,
            @Schema(description = "사용자 ID", example = "1")
            Long userId,
            @Schema(description = "닉네임", example = "독서왕")
            String nickname,
            @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
            String profileImageUrl,
            @Schema(description = "모임 역할", example = "LEADER")
            GatheringRole role
    ){
        public static MemberInfo from(GatheringMember member, String presignedProfileImageUrl){
            if(member == null){
                return null;
            }
            return MemberInfo.builder()
                    .gatheringMemberId(member.getId())
                    .userId(member.getUser().getId())
                    .nickname(member.getUser().getNickname())
                    .profileImageUrl(presignedProfileImageUrl)
                    .role(member.getRole())
                    .build();
        }
    }
}
