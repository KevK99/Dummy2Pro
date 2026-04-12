package me.daskabel.dummy2pro.security;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Unittests für den {@link SessionAuthenticationFilter}.
 *
 * Geprüft wird, wie der Filter Sitzungsdaten in den Security-Kontext
 * überführt und wie er sich bei fehlender oder unvollständiger Sitzung
 * verhält. Der Test läuft bewusst ohne Spring-Kontext und arbeitet direkt
 * mit Mock-Servletobjekten.
 */
class SessionAuthenticationFilterUnitTest
{
    private final SessionAuthenticationFilter filter = new SessionAuthenticationFilter();

    @AfterEach
    void clearContext()
    {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validSession_populatesAuthenticatedUserIntoSecurityContext() throws ServletException, IOException
    {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SecuritySessionKeys.USER_ID, 42L);
        session.setAttribute(SecuritySessionKeys.USERNAME, "jan");
        request.setSession(session);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertInstanceOf(AuthenticatedUser.class, authentication.getPrincipal());

        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        assertEquals(42L, principal.userId());
        assertEquals("jan", principal.username());
        assertFalse(authentication.getAuthorities().isEmpty());
    }

    @Test
    void missingSession_clearsExistingSecurityContext() throws ServletException, IOException
    {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        new AuthenticatedUser(1L, "alt"),
                        null,
                        AuthorityUtils.createAuthorityList("ROLE_USER")
                )
        );

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void sessionWithoutRequiredAttributes_clearsExistingSecurityContext() throws ServletException, IOException
    {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        new AuthenticatedUser(1L, "alt"),
                        null,
                        AuthorityUtils.createAuthorityList("ROLE_USER")
                )
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SecuritySessionKeys.USER_ID, 7L);
        request.setSession(session);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void sameAuthenticatedUser_keepsCurrentAuthenticationInstance() throws ServletException, IOException
    {
        var existingAuthentication = UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedUser(9L, "mira"),
                null,
                AuthorityUtils.createAuthorityList("ROLE_USER")
        );
        SecurityContextHolder.getContext().setAuthentication(existingAuthentication);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SecuritySessionKeys.USER_ID, 9L);
        session.setAttribute(SecuritySessionKeys.USERNAME, "mira");
        request.setSession(session);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertSame(existingAuthentication, SecurityContextHolder.getContext().getAuthentication());
    }
}
