package com.dokdok.retrospective.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "멤버별 수집된 주제 답변")
public record MemberAnswerResponse(
        @Schema(description = "사용자 ID",example = "1")
        Long userId,

        @Schema(description = "사용자 닉네임", example = "독서왕")
        String nickname,

        @Schema(description = "프로필 이미지 URL")
        String profileImageUrl,

        @Schema(description = "주제별 사전 답변목록")
        List<TopicAnswerItem> topics
) {
    @Schema(description = "주제별 사전 답변")
    public record TopicAnswerItem(
            @Schema(description = "주제 ID",example = "1")
            Long topicId,

            @Schema(description = "주제 제목", example = "가짜욕망 유사욕망")
            String title,

            @Schema(description = "확정 순서", example = "1")
            Integer confirmOrder,

            @Schema(description = "답변 ID",example = "101")
            Long answerId,

            @Schema(description = "답변 내용",example = "어쩌구 저쩌구 ...")
            String content
    ){
    }
}
