package com.dokdok.book.api;

import com.dokdok.book.dto.response.KakaoBookResponse;
import com.dokdok.book.webClient.KakaoBookClient;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("카카오 도서 검색 API 연동 테스트")
class KakaoBookAPI_GET {

    private static final String DEFAULT_BASE_URL = "https://dapi.kakao.com";
    private static final String PRIMARY_ENV_KEY = "KAKAO_REST_API_KEY";
    private static final String FALLBACK_ENV_KEY = "KAKAO_API_KEY";
    private static final String BASE_URL_KEY = "KAKAO_API_BASE_URL";
    private static final Map<String, String> DOTENV = loadDotEnv();

    @Test
    @DisplayName("환경변수 또는 .env의 API 키로 책 검색 시 결과를 반환한다")
    void searchBooks_withValidApiKey_returnsResults() {
        String apiKey = resolveApiKey();
        Assumptions.assumeFalse(apiKey.isBlank(), "Kakao API key is missing; skipping external API call.");

        String baseUrl = resolveBaseUrl();
        KakaoBookClient kakaoBookClient = new KakaoBookClient(createWebClient(baseUrl, apiKey));
        KakaoBookResponse response = kakaoBookClient.searchBooks("harry potter", 1, 10);

        assertThat(response).isNotNull();
        assertThat(response.documents()).isNotNull();
        assertThat(response.documents()).isNotEmpty();
        assertThat(response.meta()).isNotNull();
        assertThat(response.meta().totalCount()).isGreaterThan(0);
    }

    private WebClient createWebClient(String baseUrl, String apiKey) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + apiKey)
                .build();
    }

    private static String resolveApiKey() {
        return resolveFromEnvOrDotEnv(PRIMARY_ENV_KEY, FALLBACK_ENV_KEY);
    }

    private static String resolveBaseUrl() {
        String value = resolveFromEnvOrDotEnv(BASE_URL_KEY, null);
        return value.isBlank() ? DEFAULT_BASE_URL : value;
    }

    private static String resolveFromEnvOrDotEnv(String key, String secondaryKey) {
        String value = readKey(key);
        if (value.isBlank() && secondaryKey != null) {
            value = readKey(secondaryKey);
        }
        return value;
    }

    private static String readKey(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            value = DOTENV.getOrDefault(key, "");
        }
        return value.trim();
    }

    private static Map<String, String> loadDotEnv() {
        Path path = Path.of(".env");
        if (!Files.exists(path)) {
            return Collections.emptyMap();
        }

        Map<String, String> values = new HashMap<>();
        try {
            for (String line : Files.readAllLines(path)) {
                String trimmed = line.trim();
                if (trimmed.startsWith("#") || !trimmed.contains("=")) {
                    continue;
                }
                int idx = trimmed.indexOf('=');
                String key = trimmed.substring(0, idx).trim();
                String val = trimmed.substring(idx + 1).trim();
                values.put(key, val);
            }
        } catch (IOException ignored) {
            return Collections.emptyMap();
        }
        return values;
    }
}
