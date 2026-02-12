package com.dokdok.book.exception;

import com.dokdok.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum BookErrorCode implements BaseErrorCode {

    BOOK_NOT_FOUND("B001", "책을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    BOOK_ALREADY_EXISTS("B002", "이미 등록된 책입니다.", HttpStatus.CONFLICT),
    BOOK_NOT_IN_SHELF("B003", "책장에 해당 책이 존재하지 않습니다.", HttpStatus.NOT_FOUND),
    BOOK_REVIEW_NOT_FOUND("B004", "책 리뷰를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    BOOK_REVIEW_ALREADY_EXISTS("B005", "이미 책 리뷰가 존재합니다.", HttpStatus.CONFLICT),
    KEYWORD_NOT_FOUND("B006", "키워드를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    KEYWORD_NOT_SELECTABLE("B007", "선택할 수 없는 키워드입니다.", HttpStatus.BAD_REQUEST),
    BOOK_REVIEW_INVALID_RATING("B008", "별점은 0.5 단위의 5점 만점 값이어야 합니다.", HttpStatus.BAD_REQUEST),
    BOOK_REVIEW_DELETED("B009", "삭제된 책 리뷰입니다.", HttpStatus.BAD_REQUEST),
    BOOK_REVIEW_ACCESS_DENIED_NOT_WRITTEN("B010", "평가를 작성한 사용자만 조회할 수 있습니다.", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
