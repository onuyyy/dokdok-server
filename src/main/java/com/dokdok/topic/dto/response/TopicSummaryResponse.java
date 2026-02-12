package com.dokdok.topic.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "토픽 요약 응답")
public record TopicSummaryResponse(
        @Schema(description = "AI 요약 결과 원문(JSON 문자열)", example = "{\"summary\":\"...\",\"highlights\":[\"...\"],\"keywords\":[\"...\"]}")
        String result
) {
    public static TopicSummaryResponse from(String result) {
        return new TopicSummaryResponse(result);
    }
}
