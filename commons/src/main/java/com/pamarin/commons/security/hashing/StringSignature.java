/*
 * Copyright 2017-2019 Pamarin.com
 */
package com.pamarin.commons.security.hashing;

/**
 *
 * @author jitta
 */
public interface StringSignature {

    String sign(String string);

    void verify(String string, String signature);
}
