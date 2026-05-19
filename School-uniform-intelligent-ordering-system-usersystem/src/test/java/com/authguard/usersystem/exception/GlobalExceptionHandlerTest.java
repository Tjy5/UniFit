package com.authguard.usersystem.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.authguard.usersystem.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldMapCommonErrorResponses() {
        assertResponse(handler.handleBizException(new BizException("bad")), 400, "bad");
        assertResponse(handler.handleUnauthenticatedException(new UnauthenticatedException("login required")), 401, "login required");
        assertResponse(handler.handleForbiddenException(new ForbiddenException("denied")), 403, "denied");
        assertResponse(handler.handleNotFoundException(new NotFoundException("missing")), 404, "missing");
        assertResponse(handler.handleException(new RuntimeException("boom")), 500, "系统内部错误");
    }

    private void assertResponse(ResponseEntity<ApiResponse<Void>> response, int code, String message) {
        assertEquals(code, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(code, response.getBody().getCode());
        assertEquals(message, response.getBody().getMessage());
    }
}
