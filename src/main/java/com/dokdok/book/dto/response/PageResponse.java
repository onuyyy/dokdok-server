package com.dokdok.book.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> items;       // 실제 데이터
    private long totalCount;     // 전체 데이터 수
    private int currentPage;     // 현재 페이지 (0-base)
    private int pageSize;        // 페이지 크기
    private int totalPages;      // 전체 페이지 수

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),         // items
                page.getTotalElements(),  // totalCount
                page.getNumber(),         // currentPage
                page.getSize(),           // pageSize
                page.getTotalPages()     // totalPages
        );
    }

}

