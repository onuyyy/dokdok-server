package com.dokdok.oauth2.handler;

import com.dokdok.global.config.FrontendProperties;
import com.dokdok.global.response.ApiResponse;
import com.dokdok.oauth2.CustomOAuth2User;
import com.dokdok.oauth2.exception.OAuth2ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final FrontendProperties frontendProperties;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        CustomOAuth2User oAuth2User = extractOAuth2User(authentication);

        if (oAuth2User == null) {
            log.error("인증 정보를 추출할 수 없습니다.");
            sendErrorResponse(response,
                    OAuth2ErrorCode.INVALID_USER_PRINCIPAL.getCode(),
                    OAuth2ErrorCode.INVALID_USER_PRINCIPAL.getMessage());
            return;
        }

        String frontendUrl = getFrontendUrl(request);
        String redirectPath = determineRedirectPath(oAuth2User);
        String redirectUrl = frontendUrl + redirectPath;

        log.info("로그인 성공 리다이렉트: userId={}, url={}",
                oAuth2User.getUserId(), redirectUrl);

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    /**
     * Authentication에서 CustomOAuth2User 안전하게 추출
     */
    private CustomOAuth2User extractOAuth2User(Authentication authentication) {

        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomOAuth2User customOAuth2User) {
            return customOAuth2User;
        }

        log.error("Principal이 CustomOAuth2User 타입이 아닙니다: {}", principal.getClass().getName());
        return null;
    }

    /**
     * 프론트엔드 URL 결정 (세션에서 가져오거나 기본값 사용)
     */
    private String getFrontendUrl(HttpServletRequest request) {

        String feOrigin = (String) request.getSession().getAttribute("fe_origin");

        // 세션에서 제거
        if (feOrigin != null) {
            request.getSession().removeAttribute("fe_origin");
        }

        // 프로토콜이 없으면 http:// 추가
        if (feOrigin != null && !feOrigin.startsWith("http://") && !feOrigin.startsWith("https://")) {
            feOrigin = "http://" + feOrigin;
        }

        // 화이트리스트 검증
        if (feOrigin != null
                && frontendProperties.getAllowedOrigins() != null
                && frontendProperties.getAllowedOrigins().contains(feOrigin)) {
            log.info("세션에서 가져온 FE Origin 사용: {}", feOrigin);
            return feOrigin;
        }

        // 기본값 사용
        log.info("기본 FE URL 사용: {}", frontendProperties.getDefaultUrl());
        return frontendProperties.getDefaultUrl();
    }

    /**
     * 리다이렉트 경로 결정 (온보딩 여부)
     */
    private String determineRedirectPath(CustomOAuth2User oAuth2User) {

        if (oAuth2User.getUser().getNickname() == null || oAuth2User.getUser().getNickname().isBlank()) {

            log.info("신규 사용자 - 온보딩 필요: userId={}", oAuth2User.getUserId());
            return "/onboarding";
        }

        log.info("기존 사용자 - 홈으로 이동: userId={}, nickname={}", oAuth2User.getUserId(), oAuth2User.getNickname());
        return "/home";
    }

    /**
     * 에러 응답 전송 (ApiResponse 형식)
     */
    private void sendErrorResponse(HttpServletResponse response, String code, String message) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        ApiResponse<Void> apiResponse = new ApiResponse<>(code, message, null);
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
