package me.daskabel.dummy2pro.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Überträgt Anmeldedaten aus der HTTP-Sitzung in den Sicherheitskontext.
 *
 * Dadurch erkennt Spring Security auch bei späteren Anfragen den aktuell
 * angemeldeten Benutzer wieder.
 */
@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter
{
    /**
     * Baut den Sicherheitskontext aus den Sitzungsdaten wieder auf.
     *
     * Die Anmeldung wird hier nicht erneut geprüft. Es werden nur die bereits
     * beim Login abgelegten Sitzungsattribute in eine Spring-Security-
     * Authentifizierung überführt.
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException
    {
        // Ohne HTTP-Sitzung gibt es keine persistierten Anmeldedaten.
        HttpSession session = request.getSession(false);

        if (session != null)
        {
            Object userIdObj = session.getAttribute(SecuritySessionKeys.USER_ID);
            Object usernameObj = session.getAttribute(SecuritySessionKeys.USERNAME);

            if (userIdObj instanceof Long userId && usernameObj instanceof String username)
            {
                org.springframework.security.core.Authentication currentAuthentication =
                        SecurityContextHolder.getContext().getAuthentication();

                boolean needsRefresh = true;

                if (currentAuthentication != null
                        && currentAuthentication.getPrincipal() instanceof AuthenticatedUser currentUser)
                {
                    // Eine bereits vorhandene Authentifizierung bleibt erhalten,
                    // solange sie inhaltlich zur Sitzung passt.
                    needsRefresh = !userId.equals(currentUser.userId())
                            || !username.equals(currentUser.username());
                }

                if (needsRefresh)
                {
                    AuthenticatedUser principal = new AuthenticatedUser(userId, username);

                    UsernamePasswordAuthenticationToken authentication =
                            UsernamePasswordAuthenticationToken.authenticated(
                                    principal,
                                    null,
                                    AuthorityUtils.createAuthorityList("ROLE_USER")
                            );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
            else
            {
                // Unvollständige oder inkonsistente Sitzungsdaten dürfen nicht
                // dazu führen, dass ein alter Sicherheitskontext weiterlebt.
                SecurityContextHolder.clearContext();
            }
        }
        else
        {
            // Ohne Sitzung darf auch keine Benutzeridentität im Kontext stehen.
            SecurityContextHolder.clearContext();
        }

        // Die eigentliche Anfrage läuft danach normal weiter durch die Kette.
        filterChain.doFilter(request, response);
    }
}
