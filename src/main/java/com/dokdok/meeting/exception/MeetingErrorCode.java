package com.dokdok.meeting.exception;

import com.dokdok.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum MeetingErrorCode implements BaseErrorCode {

    MEETING_NOT_FOUND("M001", "약속을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    GATHERING_NOT_FOUND("M002", "모임을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    NOT_GATHERING_MEETING("M003", "모임에 속한 약속이 아닙니다.", HttpStatus.FORBIDDEN),
    NOT_MEETING_MEMBER("M004", "약속의 멤버가 아닙니다.", HttpStatus.FORBIDDEN),
    MEETING_MEMBER_NOT_FOUND("M005", "해당 약속의 멤버들을 찾을 수 없습니다.", HttpStatus.FORBIDDEN),
    NOT_MEETING_LEADER("M006", "약속장만 수정할 수 있습니다.", HttpStatus.FORBIDDEN),

    MEETING_ALREADY_CONFIRMED("M007", "약속이 확정된 경우에는 주제를 제안할 수 없습니다.", HttpStatus.BAD_REQUEST),
    MEETING_FULL("M008", "약속 정원이 마감되었습니다.", HttpStatus.BAD_REQUEST),
    INVALID_MEETING_STATUS_CHANGE("M009", "약속 상태를 변경할 수 없습니다.", HttpStatus.BAD_REQUEST),
    MEETING_ALREADY_JOINED("M010", "이미 참가한 약속입니다.", HttpStatus.BAD_REQUEST),
    MEETING_ALREADY_CANCELED("M011", "이미 취소된 약속입니다.", HttpStatus.BAD_REQUEST),
    MEETING_CANCEL_NOT_ALLOWED("M012", "약속 시작 24시간 이내에는 취소할 수 없습니다.", HttpStatus.BAD_REQUEST),
    MEETING_DELETE_NOT_ALLOWED("M015", "약속 시작 24시간 이내에는 삭제할 수 없습니다.", HttpStatus.BAD_REQUEST),
    MEETING_JOIN_NOT_ALLOWED("M016", "약속 시작 24시간 이내에는 참가 신청할 수 없습니다.", HttpStatus.BAD_REQUEST),
    MEETING_UPDATE_NOT_ALLOWED("M017", "약속 시작 24시간 이내에는 수정할 수 없습니다.", HttpStatus.BAD_REQUEST),
    MEETING_NOT_CONFIRMED("M018", "약속이 확정된 경우에만 주제를 제안할 수 있습니다.", HttpStatus.BAD_REQUEST),
    MEETING_DATE_REQUIRED("M019", "약속 시작/종료 일시는 필수입니다.", HttpStatus.BAD_REQUEST),

    INVALID_MAX_PARTICIPANTS("M013", "최대 참가 인원은 1명 이상이어야 하며, 모임 전체 인원을 초과할 수 없습니다.", HttpStatus.BAD_REQUEST),
    MAX_PARTICIPANTS_LESS_THAN_CURRENT("M014", "현재 참가 확정된 인원 수보다 적게 수정할 수 없습니다.", HttpStatus.BAD_REQUEST);


    private final String code;
    private final String message;
    private final HttpStatus status;
}
