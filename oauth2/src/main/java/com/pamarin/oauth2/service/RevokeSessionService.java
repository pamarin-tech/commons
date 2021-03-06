/*
 * Copyright 2017-2019 Pamarin.com
 */
package com.pamarin.oauth2.service;

import java.util.List;

/**
 *
 * @author jitta
 */
public interface RevokeSessionService {

    void revokeBySessionId(String sessionId);

    void revokeBySessionIdWithoutToken(String sessionId);

    void revokeBySessionIds(List<String> sessionIds);
    
    void revokeBySessionIdsWithoutToken(List<String> sessionIds);

    void revokeAllOnSameUserAgentBySessionId(String sessionId);

    void revokeOthersOnSameUserAgentBySessionId(String sessionId);

    void revokeByUserId(String userId);

    void revokeExpiredSessions();
}
