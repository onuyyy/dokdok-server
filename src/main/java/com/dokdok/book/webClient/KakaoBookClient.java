package com.dokdok.book.webClient;

import com.dokdok.book.dto.response.KakaoBookResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class KakaoBookClient {

    private final WebClient kakaoWebClient;

    public KakaoBookResponse searchBooks(String query, int page, int size) {
        return kakaoWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v3/search/book")
                        .queryParam("query", query)
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build())
                .retrieve()
                .bodyToMono(KakaoBookResponse.class)
                .block();
    }

}
