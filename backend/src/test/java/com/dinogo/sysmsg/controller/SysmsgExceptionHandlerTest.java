package com.dinogo.sysmsg.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

import com.dinogo.sysmsg.dto.response.SysmsgApiErrorResponse;
import com.dinogo.sysmsg.exception.SysmsgConflictException;

class SysmsgExceptionHandlerTest {
    private SysmsgExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new SysmsgExceptionHandler();
        request = new MockHttpServletRequest("GET", "/api/sysmsg/test");
    }

    @Test
    void illegalArgumentReturns400() {
        assertResponse(HttpStatus.BAD_REQUEST,
                handler.badRequest(new IllegalArgumentException("bad"), request).getBody());
    }

    @Test
    void missingAuthenticationReturns401() {
        assertResponse(HttpStatus.UNAUTHORIZED,
                handler.unauthorized(
                        new AuthenticationCredentialsNotFoundException("login"), request).getBody());
    }

    @Test
    void securityExceptionReturns403() {
        assertResponse(HttpStatus.FORBIDDEN,
                handler.forbidden(new SecurityException("denied"), request).getBody());
    }

    @Test
    void missingResourceReturns404() {
        assertResponse(HttpStatus.NOT_FOUND,
                handler.notFound(new NoSuchElementException("missing"), request).getBody());
    }

    @Test
    void duplicateResourceReturns409() {
        assertResponse(HttpStatus.CONFLICT,
                handler.conflict(new SysmsgConflictException("duplicate"), request).getBody());
    }

    @Test
    void illegalStateReturns422() {
        assertResponse(HttpStatus.UNPROCESSABLE_ENTITY,
                handler.unprocessable(new IllegalStateException("state"), request).getBody());
    }

    private void assertResponse(HttpStatus expected, SysmsgApiErrorResponse body) {
        assertEquals(expected.value(), body.status());
        assertEquals(expected.getReasonPhrase(), body.error());
        assertEquals("/api/sysmsg/test", body.path());
    }
}
