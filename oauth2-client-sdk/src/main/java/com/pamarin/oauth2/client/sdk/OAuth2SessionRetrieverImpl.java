/*
 * Copyright 2017-2019 Pamarin.com
 */
package com.pamarin.oauth2.client.sdk;

import com.pamarin.commons.provider.HostUrlProvider;
import com.pamarin.commons.security.DefaultUserDetails;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import static org.springframework.util.StringUtils.hasText;
import org.springframework.web.client.HttpClientErrorException;
import java.util.Objects;

/**
 *
 * @author jitta
 */
@Component
public class OAuth2SessionRetrieverImpl implements OAuth2SessionRetriever {

    private static final Logger LOG = LoggerFactory.getLogger(OAuth2SessionRetrieverImpl.class);

    private static final String SPRING_SECURITY_CONTEXT = "SPRING_SECURITY_CONTEXT";

    private static final int ONE_DAY = 60 * 60 * 24;

    private final OAuth2ClientOperations oauth2ClientOperations;

    private final HostUrlProvider hostUrlProvider;

    private final OAuth2AccessTokenResolver oauth2AccessTokenResolver;

    private final OAuth2RefreshTokenResolver oauth2RefreshTokenResolver;

    @Autowired
    public OAuth2SessionRetrieverImpl(
            OAuth2ClientOperations oauth2ClientOperations,
            HostUrlProvider hostUrlProvider,
            OAuth2AccessTokenResolver oauth2AccessTokenResolver,
            OAuth2RefreshTokenResolver oauth2RefreshTokenResolver
    ) {
        this.oauth2ClientOperations = oauth2ClientOperations;
        this.hostUrlProvider = hostUrlProvider;
        this.oauth2AccessTokenResolver = oauth2AccessTokenResolver;
        this.oauth2RefreshTokenResolver = oauth2RefreshTokenResolver;
    }

    @Override
    public void retrieve(HttpServletRequest httpReq, HttpServletResponse httpResp) {
        String code = httpReq.getParameter("code");
        String state = httpReq.getParameter("state");
        if (hasText(code) && hasText(state)) {
            verifyAuthorizationState(state, httpReq);
            getAccessTokenByAuthorizationCode(code, httpReq, httpResp);
        }

        getSession(httpReq, httpResp);
    }

    private void verifyAuthorizationState(String state, HttpServletRequest httpReq) {
        HttpSession session = httpReq.getSession(false);
        if (session != null) {
            String sessionState = (String) session.getAttribute(OAuth2SdkConstant.OAUTH2_AUTHORIZATION_STATE);
            if (!Objects.equals(state, sessionState)) {
                clearSecurityContext(httpReq);
                throw new InvalidAuthorizationStateException("Invalid Authorization state " + state);
            }
        }
    }

    private void throwIfNotUnauthorized(HttpClientErrorException ex) {
        if (ex.getStatusCode() != HttpStatus.UNAUTHORIZED) {
            throw ex;
        }
    }

    private void getAccessTokenByAuthorizationCode(String authorizationCode, HttpServletRequest httpReq, HttpServletResponse httpResp) {
        try {
            LOG.debug("authorizationCode => {}", authorizationCode);
            OAuth2AccessToken accessToken = oauth2ClientOperations.getAccessTokenByAuthorizationCode(authorizationCode);
            saveToken(accessToken, httpReq, httpResp);
        } catch (HttpClientErrorException ex) {
            LOG.debug("getAccessToken error => {}", ex);
            clearSecurityContext(httpReq);
            throwIfNotUnauthorized(ex);
        }
    }

    private void getSession(HttpServletRequest httpReq, HttpServletResponse httpResp) {
        String accessToken = oauth2AccessTokenResolver.resolve(httpReq);
        if (!retrieveSession(accessToken, httpReq, httpResp)) {
            accessToken = refreshToken(httpReq, httpResp);
            retrieveSession(accessToken, httpReq, httpResp);
        }
    }

    private SecurityContext buildSecurityContext(OAuth2Session.User user) {
        DefaultUserDetails userDetails = DefaultUserDetails.builder()
                .username(user.getId())
                .password(user.getId())
                .authorities(user.getAuthorities())
                .build();

        SecurityContext context = new SecurityContextImpl();
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        context.setAuthentication(token);
        return context;
    }

    private boolean retrieveSession(String accessToken, HttpServletRequest httpReq, HttpServletResponse httpResp) {
        if (!hasText(accessToken)) {
            clearSecurityContext(httpReq);
            return false;
        }

        try {
            LOG.debug("accessToken => {}", accessToken);
            OAuth2Session session = oauth2ClientOperations.getSession(accessToken);
            LOG.debug("loggedIn sessionId => {}", session.getId());
            SecurityContext context = buildSecurityContext(session.getUser());
            HttpSession httpSession = httpReq.getSession(true);
            httpSession.setAttribute(SPRING_SECURITY_CONTEXT, context);
            OAuth2SessionContext.setSession(session);
            return true;
        } catch (HttpClientErrorException ex) {
            LOG.debug("retrieveSession error => {}", ex);
            clearSecurityContext(httpReq);
            throwIfNotUnauthorized(ex);
            return false;
        }
    }

    private void clearSecurityContext(HttpServletRequest httpReq) {
        HttpSession session = httpReq.getSession(false);
        if (session != null) {
            session.setAttribute(SPRING_SECURITY_CONTEXT, null);
        }
    }

    private String refreshToken(HttpServletRequest httpReq, HttpServletResponse httpResp) {
        String refreshToken = oauth2RefreshTokenResolver.resolve(httpReq);
        if (!hasText(refreshToken)) {
            return null;
        }

        try {
            LOG.debug("refreshToken => {}", refreshToken);
            OAuth2AccessToken accessToken = oauth2ClientOperations.getAccessTokenByRefreshToken(refreshToken);
            saveToken(accessToken, httpReq, httpResp);
            return accessToken.getAccessToken();
        } catch (HttpClientErrorException ex) {
            LOG.debug("refreshToken error => {}", ex);
            throwIfNotUnauthorized(ex);
            return null;
        }
    }

    private void saveToken(OAuth2AccessToken accessToken, HttpServletRequest httpReq, HttpServletResponse httpResp) {
        //cookie
        httpResp.addCookie(buildCookie(
                oauth2AccessTokenResolver.getTokenName(),
                accessToken.getAccessToken(),
                ONE_DAY
        ));

        httpResp.addCookie(buildCookie(
                oauth2RefreshTokenResolver.getTokenName(),
                accessToken.getRefreshToken(),
                ONE_DAY * 14
        ));

        //request attribute
        httpReq.setAttribute(
                oauth2RefreshTokenResolver.getTokenName(),
                accessToken.getAccessToken()
        );

        httpReq.setAttribute(
                oauth2RefreshTokenResolver.getTokenName(),
                accessToken.getRefreshToken()
        );
    }

    private Cookie buildCookie(String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setMaxAge(maxAge);
        cookie.setSecure(hostUrlProvider.provide().startsWith("https://"));
        return cookie;
    }

}
