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

@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter
{
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException
    {
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
                SecurityContextHolder.clearContext();
            }
        }
        else
        {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}