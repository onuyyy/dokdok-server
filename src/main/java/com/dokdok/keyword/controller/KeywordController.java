package com.dokdok.keyword.controller;

import com.dokdok.book.entity.KeywordType;
import com.dokdok.global.response.ApiResponse;
import com.dokdok.keyword.api.KeywordApi;
import com.dokdok.keyword.dto.response.KeywordListResponse;
import com.dokdok.keyword.service.KeywordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class KeywordController implements KeywordApi {

    private final KeywordService keywordService;

    @Override
    public ResponseEntity<ApiResponse<KeywordListResponse>> getKeywords(List<KeywordType> types) {
        KeywordListResponse response = keywordService.getKeywords(types);
        return ApiResponse.success(response, "키워드 목록 조회 성공");
    }
}
