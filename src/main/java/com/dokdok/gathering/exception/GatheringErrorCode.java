package com.dokdok.gathering.exception;

import com.dokdok.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum GatheringErrorCode implements BaseErrorCode {

    GATHERING_NOT_FOUND("GA001", "모임을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    NOT_GATHERING_MEMBER("GA002", "모임의 멤버가 아닙니다.", HttpStatus.FORBIDDEN),
    NOT_GATHERING_LEADER("GA003","리더만 가능한 작업입니다.", HttpStatus.FORBIDDEN),
    ALREADY_INACTIVE("GA004","이미 비활성 상태인 모임은 삭제할 수 없습니다.",HttpStatus.CONFLICT),
    CANNOT_REMOVE_LEADER("GA005", "유일한 리더는 강퇴할 수 없습니다.", HttpStatus.FORBIDDEN),
    ALREADY_REMOVED_MEMBER("GA006", "이미 제거된 멤버입니다.", HttpStatus.CONFLICT),
    INVITATION_CODE_GENERATION_FAILED("GA007", "초대 코드 생성에 실패했습니다. 다시 시도해주세요.", HttpStatus.INTERNAL_SERVER_ERROR),
    ALREADY_GATHERING_MEMBER("GA008", "이미 가입된 모임입니다.", HttpStatus.CONFLICT),
    JOIN_REQUEST_ALREADY_PENDING("GA009", "이미 가입 요청이 진행 중입니다.", HttpStatus.CONFLICT),
    INVALID_INVITATION_LINK("GA010", "초대링크는 필수입니다.", HttpStatus.BAD_REQUEST),
    NOT_PENDING_STATUS("GA011", "대기 중인 가입 요청만 처리할 수 있습니다.", HttpStatus.BAD_REQUEST),
    INVALID_APPROVE_TYPE("GA012", "승인 상태는 ACTIVE 또는 REJECTED만 가능합니다.", HttpStatus.BAD_REQUEST),
    FAVORITE_LIMIT_EXCEEDED("GA013", "즐겨찾기는 최대 4개까지만 등록할 수 있습니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
