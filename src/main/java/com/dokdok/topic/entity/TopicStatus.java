package com.dokdok.topic.entity;

import lombok.Getter;

@Getter
public enum TopicStatus {
    PROPOSED("제안됨"),
    CONFIRMED("확정됨");

    private final String displayName;

    TopicStatus(String displayName) {
        this.displayName = displayName;
    }

}
