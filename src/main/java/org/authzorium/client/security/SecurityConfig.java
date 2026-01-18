package org.authzorium.client.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    public static final String HMAC_SHA_256 = "HmacSHA256";
    public static final String ROLES = "roles";
    public static final String ROLE = "ROLE_";
    public static final String SCOPE = "scope";
    public static final String SCOPE_ = "SCOPE_";
    public static final String ADMIN = "ADMIN";
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    // Inject the secret from properties; fallback to the previous literal so existing behavior remains
    private final String jwtSecret;
    private final AccessDeniedHandler accessDeniedHandler;
    private final ConfigurableEnvironment env;

    private static final String LOCAL_H2_PROFILE = "localh2";

    public SecurityConfig(RestAuthenticationEntryPoint authenticationEntryPoint,
                          @Value("${security.jwt.secret:changeit-changeit-changeit-changeit}") String jwtSecret,
                          AccessDeniedHandler accessDeniedHandler,
                          ConfigurableApplicationContext ctx) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.jwtSecret = jwtSecret;
        this.accessDeniedHandler = accessDeniedHandler;
        this.env = ctx.getEnvironment();
    }

    private boolean isLocalH2Active() {
        return java.util.Arrays.asList(env.getActiveProfiles()).contains(LOCAL_H2_PROFILE);
    }

    // Dedicated, high-precedence security chain for actuator endpoints so they can be permitted for local dev/tests
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain actuatorSecurityChain(HttpSecurity http,
                                                     @Value("${management.endpoints.web.base-path:/actuator}") String managementBasePath) throws Exception {
        // If the 'localh2' profile is active we want to expose all actuator endpoints.
        boolean localH2Active = isLocalH2Active();

        if (!localH2Active) {
            // Use a path that will never be requested so this high-precedence chain is effectively inert.
            http.securityMatcher(new AntPathRequestMatcher("/__no_actuator_public__"))
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .csrf(AbstractHttpConfigurer::disable);
            return http.build();
        }

        // Normalise base path (remove trailing slash) and create a matcher that matches both the root and children
        String base = managementBasePath.endsWith("/") ? managementBasePath.substring(0, managementBasePath.length() - 1) : managementBasePath;
        AntPathRequestMatcher rootMatcher = new AntPathRequestMatcher(base);
        AntPathRequestMatcher childrenMatcher = new AntPathRequestMatcher(base + "/**");

        // Match both "/actuator" and "/actuator/**"
        http
                .securityMatcher(new OrRequestMatcher(rootMatcher, childrenMatcher))
                // Permit all actuator requests when local-h2 is active
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                // Disable CSRF for actuator endpoints
                .csrf(AbstractHttpConfigurer::disable)
                // Also disable the oauth2 resource-server for this chain so JWT filters won't trigger and cause 401s
                .oauth2ResourceServer(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    @Profile("!localh2")
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(new AntPathRequestMatcher("/actuator/health")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/actuator/info")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/actuator/endpoints")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/h2-console/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/hello")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/admin/**")).hasRole(ADMIN)
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf.ignoringRequestMatchers(new AntPathRequestMatcher("/h2-console/**")))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                );
        return http.build();
    }

    @Bean
    @Profile("localh2")
    public SecurityFilterChain securityFilterChainLocal(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(new AntPathRequestMatcher("/actuator/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/h2-console/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/hello")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/admin/**")).hasRole(ADMIN)
                        .anyRequest().authenticated())
                .csrf(csrf ->
                        csrf.ignoringRequestMatchers(new AntPathRequestMatcher("/h2-console/**")))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                );
        return http.build();
    }

    // Dev JwtDecoder: simple HMAC-based decoder so the JwtDecoder bean exists during startup.
    // For production use an OIDC/JWK configuration or a proper key management solution.
    @Bean
    public JwtDecoder jwtDecoder() {
        // Use configured secret (testable via properties)
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        SecretKey secretKey = new SecretKeySpec(keyBytes, HMAC_SHA_256);
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
        return converter;
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        // Try 'scope' (space-separated) first, fall back to 'roles' claim if present
        if (jwt.getClaims().containsKey(SCOPE)) {
            String scope = jwt.getClaimAsString(SCOPE);
            return java.util.Arrays.stream(scope.split(" "))
                    .map(s -> new SimpleGrantedAuthority(SCOPE_ + s))
                    .collect(Collectors.toList());
        }
        if (jwt.getClaims().containsKey(ROLES)) {
            Object roles = jwt.getClaim(ROLES);
            if (roles instanceof Collection) {
                return ((Collection<?>) roles).stream()
                        .map(Object::toString)
                        .map(r -> new SimpleGrantedAuthority(ROLE + r))
                        .collect(Collectors.toList());
            }
        }
        return List.of();
    }
}
