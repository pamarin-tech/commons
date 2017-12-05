/*
 * Copyright 2017 Pamarin.com
 */
package com.pamarin.oauth2;

import com.auth0.jwt.exceptions.TokenExpiredException;
import com.pamarin.commons.security.Base64RSAEncryption;
import com.pamarin.oauth2.exception.UnauthorizedClientException;
import com.pamarin.oauth2.model.AccessTokenResponse;
import com.pamarin.oauth2.model.AuthorizationRequest;
import com.pamarin.oauth2.model.CodeAccessTokenRequest;
import com.pamarin.oauth2.model.RefreshAccessTokenRequest;
import com.pamarin.oauth2.model.TokenBase;
import com.pamarin.oauth2.service.AccessTokenGenerator;
import com.pamarin.oauth2.service.ClientVerification;
import com.pamarin.commons.security.LoginSession;
import com.pamarin.oauth2.service.TokenVerification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import com.pamarin.commons.security.RSAKeyPairs;
import com.pamarin.oauth2.domain.OAuth2AccessToken;
import com.pamarin.oauth2.repository.OAuth2AccessTokenRepo;
import com.pamarin.oauth2.service.RefreshTokenGenerator;
import com.pamarin.oauth2.service.RefreshTokenVerification;

/**
 * @author jittagornp &lt;http://jittagornp.me&gt; create : 2017/11/12
 */
@Service
@Transactional
class AccessTokenGeneratorImpl implements AccessTokenGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(AccessTokenGeneratorImpl.class);

    @Autowired
    private LoginSession loginSession;

    @Autowired
    @Qualifier("authorizationCodeVerification")
    private TokenVerification authorizationCodeVerification;

    @Autowired
    private ClientVerification clientVerification;

    @Autowired
    @Qualifier("accessTokenKeyPairs")
    private RSAKeyPairs keyPairs;

    @Autowired
    private OAuth2AccessTokenRepo accessTokenRepo;

    @Autowired
    private Base64RSAEncryption base64RSAEncryption;

    @Autowired
    private RefreshTokenGenerator refreshTokenGenerator;

    @Autowired
    private RefreshTokenVerification refreshTokenVerification;

    private AccessTokenResponse buildAccessTokenResponse(TokenBase base) {
        OAuth2AccessToken accessToken = accessTokenRepo.save(OAuth2AccessToken.builder()
                .userId(base.getUserId())
                .clientId(base.getClientId())
                .build()
        );
        String encryptedToken = base64RSAEncryption.encrypt(accessToken.getId(), keyPairs.getPrivateKey());
        return AccessTokenResponse.builder()
                .accessToken(encryptedToken)
                .expiresIn(accessToken.getExpireMinutes() * 60L)
                .refreshToken(refreshTokenGenerator.generate(base))
                .tokenType("bearer")
                .build();
    }

    @Override
    public AccessTokenResponse generate(AuthorizationRequest req) {
        UserDetails userDetails = loginSession.getUserDetails();
        return buildAccessTokenResponse(TokenBase.builder()
                .clientId(req.getClientId())
                .userId(userDetails.getUsername())
                .build());
    }

    @Override
    public AccessTokenResponse generate(CodeAccessTokenRequest req) {
        clientVerification.verifyClientIdAndClientSecret(req.getClientId(), req.getClientSecret());
        try {
            TokenBase authCode = authorizationCodeVerification.verify(req.getCode());
            return buildAccessTokenResponse(authCode);
        } catch (TokenExpiredException ex) {
            LOG.warn(null, ex);
            throw new UnauthorizedClientException(ex);
        }
    }

    @Override
    public AccessTokenResponse generate(RefreshAccessTokenRequest req) {
        clientVerification.verifyClientIdAndClientSecret(req.getClientId(), req.getClientSecret());
        TokenBase base = refreshTokenVerification.verify(req.getRefreshToken());
        base.setClientId(req.getClientId());
        return buildAccessTokenResponse(base);
    }
}
