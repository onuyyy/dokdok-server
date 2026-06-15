package com.dokdok.user.service;

import com.dokdok.oauth2.CustomOAuth2User;
import com.dokdok.user.dto.request.DevLoginRequest;
import com.dokdok.user.entity.User;
import com.dokdok.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

import java.util.Map;

@Profile({"dev", "local"})
@Service
@RequiredArgsConstructor
public class DevAuthService {

	private static final String DEV_PASSWORD = "dev1234";

	private final UserRepository userRepository;

	public void validatePassword(String password) {
		if (!DEV_PASSWORD.equals(password)) {
			throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
		}
	}

	public void login(DevLoginRequest request, HttpServletRequest httpRequest) {

        validatePassword(request.password());

		User user = userRepository.findByUserEmail(request.loginId())
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일: " + request.loginId()));

		CustomOAuth2User oAuth2User = CustomOAuth2User.builder()
				.user(user)
				.attributes(Map.of("id", user.getId()))
				.build();

		OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
				oAuth2User,
				oAuth2User.getAuthorities(),
				"kakao"
		);

		SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
		securityContext.setAuthentication(authentication);
		SecurityContextHolder.setContext(securityContext);

		HttpSession session = httpRequest.getSession(true);
		session.setAttribute(
				HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
				securityContext
		);
	}
}