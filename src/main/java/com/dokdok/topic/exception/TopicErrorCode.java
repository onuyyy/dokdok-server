package com.dokdok.topic.exception;

import com.dokdok.global.exception.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TopicErrorCode implements BaseErrorCode {
    // 리소스 에러
    TOPIC_NOT_FOUND("E101", "주제를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    TOPIC_NOT_IN_MEETING("E102", "해당 주제는 지정한 약속에 속하지 않습니다.", HttpStatus.NOT_FOUND),
    TOPIC_ANSWER_NOT_FOUND("E103", "답변을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    TOPIC_ANSWER_ALREADY_SUBMITTED("E104", "이미 제출된 답변입니다.", HttpStatus.CONFLICT),
    TOPIC_USER_CANNOT_DELETE("E105", "사용자에게 주제 삭제 권한이 없습니다.", HttpStatus.FORBIDDEN),
    TOPIC_ALREADY_DELETED("E106", "이미 삭제된 주제입니다.", HttpStatus.CONFLICT),
    TOPIC_ANSWER_ALREADY_DELETED("E107", "이미 삭제된 답변입니다.", HttpStatus.CONFLICT),
    TOPIC_ANSWER_ALREADY_EXISTS("E108", "이미 답변이 존재합니다.", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
