/*
 * Copyright 2017-2019 Pamarin.com
 */
package com.pamarin.oauth2.exception.handler;

import com.pamarin.commons.exception.InvalidCsrfTokenException;
import com.pamarin.oauth2.model.ErrorResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

/**
 *
 * @author jitta
 */
@Component
public class ErrorResponseInvalidCsrfTokenExceptionHandler extends ErrorResponseExceptionHandlerAdapter<InvalidCsrfTokenException> {

    @Override
    public Class<InvalidCsrfTokenException> getTypeClass() {
        return InvalidCsrfTokenException.class;
    }

    @Override
    protected ErrorResponse buildError(InvalidCsrfTokenException ex, HttpServletRequest httpReq, HttpServletResponse httpResp) {
        return ErrorResponse.unauthorizedClient();
    }

}
