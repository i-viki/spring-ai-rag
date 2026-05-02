package com.nexus.rag.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

/**
 * Security configuration to protect the RAG API with a simple API Key.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.security.api-key:nexus-admin-key-2024}")
    private String apiKey;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/actuator/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(new ApiKeyFilter(apiKey), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Simple filter to check for the 'X-API-KEY' header.
     */
    private static class ApiKeyFilter extends OncePerRequestFilter {
        private final String validKey;

        public ApiKeyFilter(String validKey) {
            this.validKey = validKey;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            
            String requestKey = request.getHeader("X-API-KEY");

            if (validKey.equals(requestKey)) {
                // Set authentication in the security context
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        "apiKeyUser", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
                SecurityContextHolder.getContext().setAuthentication(auth);
                filterChain.doFilter(request, response);
            } else if (request.getRequestURI().startsWith("/swagger-ui") || 
                       request.getRequestURI().startsWith("/v3/api-docs") || 
                       request.getRequestURI().startsWith("/actuator")) {
                filterChain.doFilter(request, response);
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Invalid or missing API Key (X-API-KEY header)");
            }
        }
    }
}
