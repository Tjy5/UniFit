package com.authguard.usersystem.util;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.authguard.usersystem.exception.UnauthenticatedException;
import com.authguard.usersystem.security.UserPrincipal;
import com.authguard.usersystem.service.UserAccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

    private JwtUtils jwtUtils;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        jwtUtils = mock(JwtUtils.class);
        filter = new JwtAuthenticationFilter(jwtUtils, mock(UserAccountService.class), new ObjectMapper());
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldContinueWithoutAuthenticationWhenTokenIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/cart");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertSame(request, chain.getRequest());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldRejectInvalidBearerToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/cart");
        request.addHeader("Authorization", "Bearer bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        when(jwtUtils.validateToken("bad-token")).thenThrow(new UnauthenticatedException("Token 无效"));

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().startsWith(MediaType.APPLICATION_JSON_VALUE));
        assertNull(chain.getRequest());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldSetSecurityContextForValidToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/cart");
        request.addHeader("Authorization", "Bearer good-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        Claims claims = mock(Claims.class);
        UserPrincipal principal = new UserPrincipal(42L, "student");
        when(jwtUtils.validateToken("good-token")).thenReturn(claims);
        when(jwtUtils.toPrincipal(claims)).thenReturn(principal);

        filter.doFilter(request, response, chain);

        assertSame(request, chain.getRequest());
        UsernamePasswordAuthenticationToken authentication = assertInstanceOf(
                UsernamePasswordAuthenticationToken.class,
                SecurityContextHolder.getContext().getAuthentication()
        );
        assertSame(principal, authentication.getPrincipal());
        assertEquals("good-token", authentication.getCredentials());
    }
}
