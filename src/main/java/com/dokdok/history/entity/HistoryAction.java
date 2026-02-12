package com.dokdok.history.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HistoryAction {

    INSERT("INSERT", "새로작성"),
    UPDATE("UPDATE", "수정"),
    DELETE("DELETE", "삭제");

    private final String action;
    private final String description;
}
