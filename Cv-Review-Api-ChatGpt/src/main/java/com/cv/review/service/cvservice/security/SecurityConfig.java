package com.cv.review.service.cvservice.security;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Security configuration that validates JWTs using JWKS (RSA/ECDSA signature).
 *
 * - If cvreview.security.enabled=false, security is disabled (dev mode).
 * - Requires the cvreview.security.jwk-set-uri property pointing to the JWKS of your login-service.
 * - Converts common claims (roles, realm_access.roles, scope/scp) to GrantedAuthority.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final boolean securityEnabled;
    private final String jwkSetUri;

    public SecurityConfig(@Value("${cvreview.security.enabled:true}") boolean securityEnabled,
                          @Value("${cvreview.security.jwk-set-uri:}") String jwkSetUri) {
        this.securityEnabled = securityEnabled;
        this.jwkSetUri = jwkSetUri != null ? jwkSetUri.trim() : "";
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        if (!securityEnabled) {
            // Unsafe mode (useful for local testing)
            http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }

        if (jwkSetUri.isEmpty()) {
            throw new IllegalStateException("cvreview.security.jwk-set-uri no está configurado. " +
                    "Configura la URL JWKS de tu login-service (ej: https://login.example.com/.well-known/jwks.json)");
        }

        // We configure the resource server to validate JWTs using JWKS.
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // permitir salud / actuator si lo necesitas (ajusta según tu app)
                .requestMatchers("/actuator/**", "/error").permitAll()
                // permitir OPTIONS preflight
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // proteger la API de reviews
                .requestMatchers("/api/v1/reviews/**").authenticated()
                // resto público (opcional)
                .anyRequest().permitAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    // We use a custom JwtAuthenticationConverter to map roles -> authorities.
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );

        return http.build();
    }

    /**
     * Bean JwtDecoder. Spring Boot can also create one automatically if
     * ‘spring.security.oauth2.resourceserver.jwt.jwk-set-uri’ is configured, but
     * we define it explicitly for greater control and logging.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
       if (!securityEnabled) {
            log.info("JwtDecoder no configurado porque la seguridad está desactivada.");
            return token -> null; // dummy
        }

        log.info("Configurando JwtDecoder con JWK set URI: {}", jwkSetUri);
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        return jwtDecoder;
    }
    
    /**
     * JwtAuthenticationConverter personalizado: convierte un Jwt a Authentication con GrantedAuthorities.
     * Extrae roles desde:
     * - claim «roles» (lista)
     * - claim «realm_access.roles» (estilo Keycloak)
     * - claim «authorities»
     * Además mapea scopes (claim «scope» o «scp») a authorities con prefijo SCOPE_.
     */
    private Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();

        jwtConverter.setJwtGrantedAuthoritiesConverter((Jwt jwt) -> {
            Set<String> roles = new HashSet<>();

            // 1) Claim "roles"
            if (jwt.getClaims().containsKey("roles")) {
                Object claim = jwt.getClaim("roles");
                if (claim instanceof Collection) {
                    ((Collection<?>) claim).forEach(c -> roles.add(String.valueOf(c)));
                } else {
                    roles.add(String.valueOf(claim));
                }
            }

            // 2) Keycloak style: realm_access.roles
            if (jwt.getClaims().containsKey("realm_access")) {
                Object realmAccess = jwt.getClaim("realm_access");
                if (realmAccess instanceof Map) {
                    Object r = ((Map<?, ?>) realmAccess).get("roles");
                    if (r instanceof Collection) {
                        ((Collection<?>) r).forEach(c -> roles.add(String.valueOf(c)));
                    }
                }
            }

            // 3) Claim "authorities"
            if (jwt.getClaims().containsKey("authorities")) {
                Object auths = jwt.getClaim("authorities");
                if (auths instanceof Collection) {
                    ((Collection<?>) auths).forEach(c -> roles.add(String.valueOf(c)));
                }
            }

            // Convertir roles en GrantedAuthority con prefijo ROLE_
            Set<GrantedAuthority> authorities = roles.stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .map(r -> {
                        String role = r.startsWith("ROLE_") ? r : "ROLE_" + r;
                        return new SimpleGrantedAuthority(role);
                    })
                    .collect(Collectors.toSet());

            // 4) Mapear scopes (scope/scp) a authorities con prefijo SCOPE_
            if (jwt.getClaims().containsKey("scope")) {
                String scope = jwt.getClaimAsString("scope");
                if (scope != null && !scope.isEmpty()) {
                    Arrays.stream(scope.split(" ")).map(s -> "SCOPE_" + s).map(SimpleGrantedAuthority::new).forEach(authorities::add);
                }
            } else if (jwt.getClaims().containsKey("scp")) {
                Object scpObj = jwt.getClaim("scp");
                if (scpObj instanceof Collection) {
                    ((Collection<?>) scpObj).forEach(s -> authorities.add(new SimpleGrantedAuthority("SCOPE_" + s)));
                } else if (scpObj instanceof String) {
                    Arrays.stream(((String) scpObj).split(" ")).map(s -> "SCOPE_" + s).map(SimpleGrantedAuthority::new).forEach(authorities::add);
                }
            }

            return authorities;
        });

        return jwtConverter;
    }
}

