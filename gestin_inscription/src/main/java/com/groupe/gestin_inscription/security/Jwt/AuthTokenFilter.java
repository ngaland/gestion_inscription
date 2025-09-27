package com.groupe.gestin_inscription.security.Jwt;

import com.groupe.gestin_inscription.security.SecurityUserService.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

public class AuthTokenFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);

    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImpl userDetailsService;

    // publics endpoints
    private static final String[] PUBLIC_URLS = {
            "/api/auth/",
            "/api/users/",  // for POST request only (accounts creation)
            "/v3/api-docs",
            "/swagger-ui"
    };

    public AuthTokenFilter(JwtUtils jwtUtils, UserDetailsServiceImpl userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        logger.info("=== AUTH FILTER DEBUG ===");
        logger.info("Request URI: {}", requestURI);
        logger.info("HTTP Method: {}", method);

        // Vérifier si l'endpoint est public
        boolean isPublicEndpoint = isPublicEndpoint(requestURI, method);
        logger.info("Is public endpoint: {}", isPublicEndpoint);

        try {
            String jwt = parseJwt(request);
            logger.info("JWT Token found: {}", jwt != null ? "YES (length: " + jwt.length() + ")" : "NO");

            if (jwt != null) {
                logger.info("Validating JWT token...");
                boolean isValidToken = jwtUtils.validateJwtToken(jwt);
                logger.info("JWT token valid: {}", isValidToken);

                if (isValidToken) {
                    String username = jwtUtils.getUserNameFromJwtToken(jwt);
                    logger.info("Username from token: {}", username);

                    try {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                        logger.info("User found in database: {}", userDetails.getUsername());
                        logger.info("User authorities: {}", userDetails.getAuthorities());

                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        logger.info("Authentication set successfully in SecurityContext");

                    } catch (Exception userLoadException) {
                        logger.error("Error loading user '{}': {}", username, userLoadException.getMessage());
                    }
                } else {
                    logger.warn("Invalid JWT token for request: {}", requestURI);
                }
            } else {
                if (isPublicEndpoint) {
                    logger.info("No JWT token, but endpoint is public - allowing access");
                } else {
                    logger.warn("No JWT token found for protected endpoint: {}", requestURI);
                }
            }

        } catch (Exception e) {
            logger.error("Cannot set user authentication for request {}: {}", requestURI, e.getMessage());
            logger.error("Full stack trace:", e);
        }

        // Log current authentication state
        var currentAuth = SecurityContextHolder.getContext().getAuthentication();
        if (currentAuth != null) {
            logger.info("Current authentication: {}", currentAuth.getName());
            logger.info("Current authorities: {}", currentAuth.getAuthorities());
        } else {
            logger.info("No authentication in SecurityContext");
        }

        logger.info("=== END AUTH FILTER DEBUG ===");

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        logger.debug("Authorization header: {}", headerAuth != null ? "Bearer ***" : "null");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            String token = headerAuth.substring(7);
            logger.debug("Extracted JWT token (first 20 chars): {}",
                    token.length() > 20 ? token.substring(0, 20) + "..." : token);
            return token;
        }
        return null;
    }

    private boolean isPublicEndpoint(String requestURI, String method) {
        // Vérification spéciale pour POST /api/users/ (création de compte)
        if ("POST".equals(method) && "/api/users/".equals(requestURI)) {
            return true;
        }

        // Autres endpoints publics
        return Arrays.stream(PUBLIC_URLS)
                .anyMatch(publicUrl -> requestURI.startsWith(publicUrl));
    }
}
