package com.dokdok.global.exception;

import io.netty.channel.ChannelOption;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class ErrorWebhookService {

    private static final String UNKNOWN = "unknown";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final String[] IP_HEADER_CANDIDATES = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR",
            "X-Real-IP"
    };

    private final ErrorWebhookProperties properties;

    public void sendRuntimeException(RuntimeException exception, HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return;
        }
        if (!StringUtils.hasText(properties.getUrl())) {
            log.warn("Error webhook is enabled but URL is empty. Skip sending runtime error log.");
            return;
        }

        ErrorWebhookPayload payload = new ErrorWebhookPayload(
                buildLogMessage(exception),
                exception.getClass().getSimpleName(),
                request != null ? request.getRequestURI() : UNKNOWN,
                request != null ? request.getMethod() : UNKNOWN,
                resolveClientIp(request),
                LocalDateTime.now().format(TIMESTAMP_FORMATTER)
        );

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getConnectTimeoutMs())
                .responseTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));

        WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build()
                .post()
                .uri(properties.getUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                .onErrorResume(error -> {
                    log.error("Failed to send runtime error webhook: {}", error.getMessage());
                    return Mono.empty();
                })
                .subscribe();
    }

    private String buildLogMessage(RuntimeException exception) {
        String message = StringUtils.hasText(exception.getMessage()) ? exception.getMessage() : "(no message)";
        return exception.getClass().getSimpleName() + " : " + message;
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }

        for (String header : IP_HEADER_CANDIDATES) {
            String headerValue = request.getHeader(header);
            if (!StringUtils.hasText(headerValue) || UNKNOWN.equalsIgnoreCase(headerValue)) {
                continue;
            }

            if ("X-Forwarded-For".equalsIgnoreCase(header)) {
                String[] ips = headerValue.split(",");
                if (ips.length > 0 && StringUtils.hasText(ips[0])) {
                    return ips[0].trim();
                }
            }
            return headerValue.trim();
        }

        String remoteAddr = request.getRemoteAddr();
        return StringUtils.hasText(remoteAddr) ? remoteAddr : UNKNOWN;
    }

    private record ErrorWebhookPayload(
            String log,
            String exception,
            String path,
            String method,
            String ip,
            String timestamp
    ) {
    }
}
