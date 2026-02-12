package com.dokdok.gathering.service;

import com.dokdok.gathering.entity.Gathering;
import com.dokdok.gathering.entity.GatheringMember;
import com.dokdok.gathering.entity.GatheringRole;
import com.dokdok.gathering.entity.GatheringMemberStatus;
import com.dokdok.gathering.exception.GatheringErrorCode;
import com.dokdok.gathering.exception.GatheringException;
import com.dokdok.gathering.repository.GatheringMemberRepository;
import com.dokdok.gathering.repository.GatheringRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GatheringValidator {

	private final GatheringMemberRepository gatheringMemberRepository;
	private final GatheringRepository gatheringRepository;

	/**
	 * 존재하는 모임인지 검증한다.
	 */
	public void validateGathering(Long gatheringId) {
		boolean isGathering = gatheringRepository.existsById(gatheringId);

		if (!isGathering) {
			throw new GatheringException(GatheringErrorCode.GATHERING_NOT_FOUND);
		}
	}

    /**
     * 모임에 속해있는 사용자인지 검증한다.
     */
    public void validateMembership(Long gatheringId, Long userId) {
        boolean isMember = gatheringMemberRepository
                .existsByGatheringIdAndUserId(gatheringId, userId);

		if (!isMember) {
			throw new GatheringException(GatheringErrorCode.NOT_GATHERING_MEMBER);
		}
	}

    /**
     * 모임의 모임장인지 검증한다.
     */
	public void validateLeader(Long gatheringId, Long userId) {

        validateGathering(gatheringId);

        GatheringMember member = gatheringMemberRepository
				.findByGatheringIdAndUserId(gatheringId, userId)
				.orElseThrow(() -> new GatheringException(GatheringErrorCode.NOT_GATHERING_MEMBER));

		if (member.getRole() != GatheringRole.LEADER) {
			throw new GatheringException(GatheringErrorCode.NOT_GATHERING_LEADER);
		}
	}

	/**
	 * 모임 존재 여부를 검증하고 Gathering을 반환합니다.
	 */
	public Gathering validateAndGetGathering(Long gatheringId) {
		return gatheringRepository.findById(gatheringId)
				.orElseThrow(() -> new GatheringException(GatheringErrorCode.GATHERING_NOT_FOUND));
	}

	/**
	 * 멤버십을 검증하고 GatheringMember를 반환합니다.
	 */
	public GatheringMember validateAndGetMember(Long gatheringId, Long userId) {
		return gatheringMemberRepository
				.findByGatheringIdAndUserId(gatheringId, userId)
				.orElseThrow(() -> new GatheringException(GatheringErrorCode.NOT_GATHERING_MEMBER));
	}

    /**
     * 초대링크와 일치하는 모임이 있는지 검증합니다.
     * 추후 초대링크 갱신기능 추가 시 만료 여부도 검증하도록 추가될 수 있습니다.
     */
    public Gathering validateInvitationLink(String invitationLink) {
        if (invitationLink == null || invitationLink.isBlank()) {
            throw new GatheringException(GatheringErrorCode.INVALID_INVITATION_LINK);
        }
        return gatheringRepository.findGatheringByInvitationLink(invitationLink)
                .orElseThrow(() -> new GatheringException(GatheringErrorCode.GATHERING_NOT_FOUND));
    }

    /**
     * 이미 모임에 가입했거나 가입 신청을 했는지 검증합니다.
     */
	public void validateJoinedGathering(Long gatheringId, Long userId) {
		gatheringMemberRepository
				.findByGatheringIdAndUserId(gatheringId, userId)
				.ifPresent(member -> {
					if (member.getMemberStatus() == GatheringMemberStatus.ACTIVE) {
						throw new GatheringException(GatheringErrorCode.ALREADY_GATHERING_MEMBER);
					} else if (member.getMemberStatus() == GatheringMemberStatus.PENDING) {
						throw new GatheringException(GatheringErrorCode.JOIN_REQUEST_ALREADY_PENDING);
					}
				});
	}

	/**
	 * 즐겨찾기 된 모임이 4개 이상인지 검증합니다.
	 */
	public void validateFavoriteLimit(Long userId) {
		if (gatheringMemberRepository.isFavoriteLimitExceeded(userId)) {
			throw new GatheringException(GatheringErrorCode.FAVORITE_LIMIT_EXCEEDED);
		}
	}
}
