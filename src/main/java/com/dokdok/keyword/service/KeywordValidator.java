package com.dokdok.keyword.service;

import com.dokdok.book.exception.BookErrorCode;
import com.dokdok.book.exception.BookException;
import com.dokdok.keyword.entity.Keyword;
import com.dokdok.keyword.repository.KeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class KeywordValidator {

    private final KeywordRepository keywordRepository;

    public Map<Long, Keyword> getKeywordMapByIds(List<Long> keywordIds) {
        return keywordRepository.findAllById(keywordIds).stream()
                .collect(Collectors.toMap(Keyword::getId, Function.identity()));
    }

    // 선택 가능한 키워드인지 확인하고 반환합니다.
    public Keyword validateAndGetSelectableKeyword(Long keywordId) {
        Keyword keyword = keywordRepository.findById(keywordId)
                .orElseThrow(() -> new BookException(BookErrorCode.KEYWORD_NOT_FOUND));
        if (Boolean.FALSE.equals(keyword.getIsSelectable())) {
            throw new BookException(BookErrorCode.KEYWORD_NOT_SELECTABLE);
        }
        return keyword;
    }
}
