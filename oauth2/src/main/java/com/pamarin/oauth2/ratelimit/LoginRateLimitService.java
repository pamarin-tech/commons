/*
 * Copyright 2017-2019 Pamarin.com
 */
package com.pamarin.oauth2.ratelimit;

/**
 *
 * @author jitta
 */
public interface LoginRateLimitService {

    void limit(String username);

}
