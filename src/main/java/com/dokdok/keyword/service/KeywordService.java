package com.dokdok.keyword.service;

import com.dokdok.book.entity.KeywordType;
import com.dokdok.keyword.dto.response.KeywordListResponse;
import com.dokdok.keyword.entity.Keyword;
import com.dokdok.keyword.repository.KeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KeywordService {

    private final KeywordRepository keywordRepository;

    @Transactional(readOnly = true)
    public KeywordListResponse getKeywords(List<KeywordType> types) {
        List<Keyword> keywords;
        if (types == null || types.isEmpty()) {
            keywords = keywordRepository.findAllWithParent();
        } else {
            keywords = keywordRepository.findByKeywordTypeInWithParent(types);
        }
        return KeywordListResponse.from(keywords);
    }
}
