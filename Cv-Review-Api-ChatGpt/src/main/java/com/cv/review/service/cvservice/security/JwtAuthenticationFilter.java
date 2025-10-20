package com.cv.review.service.cvservice.security;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Filter that intercepts requests, extracts the Authorization header (Bearer token),
 * validates the token using JwtTokenProvider, and sets authentication in the SecurityContext.
 *
 * It is designed for easy use in dev/local. If your login service provides roles/claims
 * differently, adapt getRolesFromToken in JwtTokenProvider.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                if (tokenProvider.validateToken(token)) {
                    String username = tokenProvider.getUsernameFromToken(token);
                    List<SimpleGrantedAuthority> authorities = tokenProvider.getRolesFromToken(token)
                            .stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());

                    // Create Authentication and set in context
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(username, null, authorities);

                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else {
                    log.debug("Token no válido para la petición: {}", request.getRequestURI());
                }
            } catch (Exception ex) {
                log.warn("Error procesando JWT: {}", ex.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
