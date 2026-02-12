package com.dokdok.keyword.repository;

import com.dokdok.book.entity.KeywordType;
import com.dokdok.keyword.entity.Keyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface KeywordRepository extends JpaRepository<Keyword, Long> {
    @Query("""
            SELECT k
            FROM Keyword k
            LEFT JOIN FETCH k.parent p
            ORDER BY k.keywordType, k.level, k.sortOrder, k.id
            """)
    List<Keyword> findAllWithParent();

    @Query("""
            SELECT k
            FROM Keyword k
            LEFT JOIN FETCH k.parent p
            WHERE k.keywordType IN :keywordTypes
            ORDER BY k.keywordType, k.level, k.sortOrder, k.id
            """)
    List<Keyword> findByKeywordTypeInWithParent(List<KeywordType> keywordTypes);
}
