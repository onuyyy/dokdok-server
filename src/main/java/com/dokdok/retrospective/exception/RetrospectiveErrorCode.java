package com.dokdok.retrospective.exception;

import com.dokdok.global.exception.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RetrospectiveErrorCode implements BaseErrorCode {

    RETROSPECTIVE_ALREADY_EXISTS("R101", "이미 해당 약속에 대한 회고가 존재합니다.", HttpStatus.CONFLICT),
    RETROSPECTIVE_NOT_FOUND("R102", "회고를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    MEETING_RETROSPECTIVE_NOT_FOUND("R103", "공동 회고 내용을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    RETROSPECTIVE_ALREADY_DELETED("R104", "이미 삭제된 개인 회고입니다.", HttpStatus.NOT_FOUND),
    NO_ACCESS_RETROSPECTIVE("R105", "회고에 접근할 권한이 없습니다.", HttpStatus.FORBIDDEN),
    SUMMARY_NOT_FOUND("R106", "AI 요약을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    NOT_AUTHOR_OF_RETROSPECTIVE("R107", "사용자가 작성한 회고가 아닙니다.", HttpStatus.FORBIDDEN);


    private final String code;
    private final String message;
    private final HttpStatus status;
}
