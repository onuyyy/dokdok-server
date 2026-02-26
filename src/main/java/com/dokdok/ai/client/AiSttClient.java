package com.dokdok.ai.client;

import com.dokdok.global.response.ApiResponse;
import com.dokdok.retrospective.dto.response.RetrospectiveSummaryResponse;
import com.dokdok.ai.dto.SttRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class AiSttClient {

    private final WebClient aiWebClient;

    @Value("${ai.api.stt-path}")
    private String sttPath;

    public ApiResponse<RetrospectiveSummaryResponse> requestStt(SttRequest request) {
        return aiWebClient.post()
                .uri(sttPath)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<RetrospectiveSummaryResponse>>() {})
                .block();
    }
}
