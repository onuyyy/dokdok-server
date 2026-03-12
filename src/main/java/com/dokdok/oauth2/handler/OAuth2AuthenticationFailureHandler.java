package com.dokdok.oauth2.handler;

import com.dokdok.global.response.ApiResponse;
import com.dokdok.oauth2.exception.OAuth2ErrorCode;
import com.dokdok.oauth2.exception.OAuth2Exception;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {

        log.error("OAuth2 인증 실패: {}", exception.getMessage(), exception);

        String errorCode;
        String errorMessage;

        // OAuth2AuthenticationException 내부의 OAuth2Exception 확인
        if (exception instanceof OAuth2AuthenticationException oauth2Exception) {

            // cause가 OAuth2Exception인 경우
            if (oauth2Exception.getCause() instanceof OAuth2Exception customException) {

                errorCode = customException.getErrorCode().getCode();
                errorMessage = customException.getErrorCode().getMessage();
            } else {

                // OAuth2AuthenticationException의 error 정보 사용
                errorCode = oauth2Exception.getError().getErrorCode();
                errorMessage = oauth2Exception.getError().getDescription();

                // description이 null이면 기본 메시지 사용
                if (errorMessage == null || errorMessage.isBlank()) {
                    errorMessage = OAuth2ErrorCode.OAUTH_AUTHENTICATION_FAILED.getMessage();
                }
            }

            log.error("OAuth2 에러 코드: {}, 메시지: {}", errorCode, errorMessage);
        } else {

            // 기타 AuthenticationException
            errorCode = OAuth2ErrorCode.OAUTH_AUTHENTICATION_FAILED.getCode();
            errorMessage = OAuth2ErrorCode.OAUTH_AUTHENTICATION_FAILED.getMessage();
        }

        // ApiResponse 형식으로 JSON 응답
        sendErrorResponse(response, errorCode, errorMessage);
    }

    private void sendErrorResponse(HttpServletResponse response, String code, String message) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        ApiResponse<Void> apiResponse = new ApiResponse<>(code, message, null);
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
