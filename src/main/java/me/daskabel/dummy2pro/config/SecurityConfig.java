package me.daskabel.dummy2pro.config;

import me.daskabel.dummy2pro.security.SessionAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import java.time.Clock;

/**
 * Sicherheitskonfiguration der Anwendung.
 *
 * Legt fest, welche Pfade frei erreichbar sind, wann eine Anmeldung
 * erforderlich ist und wie Sitzungs- und CSRF-Prüfung behandelt werden.
 */
@Configuration
public class SecurityConfig
{
    /**
     * Zentrale Uhr für zeitabhängige Prüfungen.
     */
    @Bean
    public Clock applicationClock()
    {
        return Clock.systemUTC();
    }

    /**
     * Definiert die Sicherheitsregeln der Anwendung.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SessionAuthenticationFilter sessionAuthenticationFilter) throws Exception
    {
        return http
                // Eigene Sitzungsprüfung vor den Standardfilter setzen.
                .addFilterBefore(sessionAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        // Login und Registrierung müssen auch ohne vorhandenes CSRF-Token möglich sein.
                        .ignoringRequestMatchers(
                                "/api/login",
                                "/api/register"
                        )
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/login.html",
                                "/register.html",
                                "/api/login",
                                "/api/register",
                                "/csrf",
                                "/images/**",
                                "/js/**",
                                "/css/**",
                                "/favicon.ico",
                                "/actuator/health",
                                "/actuator/info"
                        ).permitAll()
                        .requestMatchers(
                                "/schema/**",
                                "/actuator/beans/**",
                                "/actuator/env/**",
                                "/actuator/configprops/**"
                        ).denyAll()
                        .requestMatchers(HttpMethod.GET, "/error").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            // API-Aufrufe bekommen einen Statuscode, Seitenaufrufe gehen zurück zur Startseite.
                            if (request.getRequestURI().startsWith("/api/"))
                            {
                                response.sendError(401);
                            }
                            else
                            {
                                response.sendRedirect("/");
                            }
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            if (request.getRequestURI().startsWith("/api/"))
                            {
                                response.sendError(403);
                            }
                            else
                            {
                                response.sendRedirect("/");
                            }
                        })
                )
                .logout(logout -> logout.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(form -> form.disable())
                .build();
    }
}
