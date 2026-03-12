package com.dokdok.global.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.error-webhook")
public class ErrorWebhookProperties {
    private boolean enabled = false;
    private String url;
    private int connectTimeoutMs = 1000;
    private int readTimeoutMs = 1500;
}
