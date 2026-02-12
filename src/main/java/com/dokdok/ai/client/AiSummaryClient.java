package com.dokdok.ai.client;

import com.dokdok.ai.dto.TopicSummaryRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class AiSummaryClient {

    private final WebClient aiWebClient;

    @Value("${ai.api.summary-path}")
    private String summaryPath;

    public String requestTopicSummary(TopicSummaryRequest request) {
        return aiWebClient.post()
                .uri(summaryPath)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
