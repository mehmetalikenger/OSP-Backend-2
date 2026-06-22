package org.offitec.osp.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.offitec.osp.domain.port.UserRepositoryPort;
import org.offitec.osp.domain.entity.User;

import java.time.Instant;
import java.util.Optional;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.Date;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepositoryPort userRepositoryPort;

    public JwtFilter(JwtService jwtService, UserRepositoryPort userRepositoryPort){
        this.jwtService = jwtService;
        this.userRepositoryPort = userRepositoryPort;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String token = null;
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else if (request.getCookies() != null) {
            if (request.getRequestURI().equals("/newAccessToken")) {
                for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                    if ("refreshToken".equals(cookie.getName())) {
                        token = cookie.getValue();
                        break;
                    }
                }
            } else {
                for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                    if ("accessToken".equals(cookie.getName())) {
                        token = cookie.getValue();
                        break;
                    }

                    if (cookie.getName().equals("accountActivationToken")) {
                        token = cookie.getValue();
                        break;
                    }

                    if (cookie.getName().equals("resetPasswordToken")) {
                        token = cookie.getValue();
                        break;
                    }

                    if (cookie.getName().equals("accountDeletionToken")) {
                        token = cookie.getValue();
                        break;
                    }
                }
            }
        }

        if(token == null || token.trim().isEmpty()){
            filterChain.doFilter(request, response);
            return;
        }

        SecretKey key = jwtService.getSecretKey();
        String uri = request.getRequestURI();
        if (uri.equals("/account/activate")) {
            key = jwtService.getAccountActivationKey();
        } else if (uri.equals("/account/reset-password")) {
            key = jwtService.getResetPasswordKey();
        } else if (uri.equals("/account/delete-account")) {
            key = jwtService.getAccountDeletionKey();
        }

        Jwt<?,?> jwt = null;

        try{
            jwt = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);

        } catch (JwtException ex) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(ex.getMessage());
            return;
        }

        Claims claims = (Claims) jwt.getPayload();

        String email = claims.getSubject();

        Optional<User> dbUser = userRepositoryPort.findByEmail(email);
        if (dbUser.isEmpty() || dbUser.get().getDeletedAt() != null) {
            if (request.getRequestURI().equals("/auth/logout")) {
                filterChain.doFilter(request, response);
                return;
            }

            jakarta.servlet.http.Cookie accessTokenCookie = new jakarta.servlet.http.Cookie("accessToken", null);
            accessTokenCookie.setPath("/");
            accessTokenCookie.setHttpOnly(true);
            accessTokenCookie.setMaxAge(0);
            response.addCookie(accessTokenCookie);

            jakarta.servlet.http.Cookie refreshTokenCookie = new jakarta.servlet.http.Cookie("refreshToken", null);
            refreshTokenCookie.setPath("/");
            refreshTokenCookie.setHttpOnly(true);
            refreshTokenCookie.setMaxAge(0);
            response.addCookie(refreshTokenCookie);

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"User account is disabled or deleted.\"}");
            return;
        }

        String tokenType = (String) claims.get("tokenType");

        if (tokenType != null && tokenType.equals("ACCOUNT_ACTIVATION")) {
            
            if (!request.getRequestURI().equals("/account/activate")) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("You do not have permission to access this resource.");
                return;
            }

            if(dbUser.get().getStatus().toString().equals("ACTIVE")){
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                response.setContentType("application/json");
                response.getWriter().write("Account is already activated.");
                return;
            }
        }

        if (tokenType != null && tokenType.equals("RESET_PASSWORD")) {

            if (!request.getRequestURI().equals("/account/reset-password")) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("You do not have permission to access this resource.");
                return;
            }

            if(claims.getExpiration().toInstant().isBefore(Instant.now())){
                response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
                response.setContentType("application/json");
                response.getWriter().write("This password reset link is no longer valid.");
            }

            if (dbUser.get().getPasswordUpdateDate() != null && claims.getIssuedAt() != null) {
                java.time.LocalDateTime issuedAt = java.time.LocalDateTime.ofInstant(claims.getIssuedAt().toInstant(), java.time.ZoneId.systemDefault());

                if (dbUser.get().getPasswordUpdateDate().isAfter(issuedAt)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("This password reset link is no longer valid.");
                    return;
                }
            }
        }

        if (tokenType != null && tokenType.equals("ACCOUNT_DELETION")) {
            
            if (!request.getRequestURI().equals("/account/delete-account")) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("You do not have permission to access this resource.");
                return;
            }

            if(claims.getExpiration().toInstant().isBefore(Instant.now())){
                response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
                response.setContentType("application/json");
                response.getWriter().write("This account deletion link is no longer valid.");
            }

            if(dbUser.get().getStatus().toString().equals("DELETED")){
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                response.setContentType("application/json");
                response.getWriter().write("Account is already deleted.");
                return;
            }
        }

        Boolean rememberMe = (Boolean) claims.get("rememberMe");

        String role = claims.get("role", String.class);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        
        java.util.List<org.springframework.security.core.GrantedAuthority> authorities = 
            (role != null && !role.trim().isEmpty()) ? java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority(role)) 
                         : java.util.Collections.emptyList();

        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(email, rememberMe, authorities);
        // Stash the already-loaded user id so request-scoped lookups (e.g. the public
        // unit endpoints' currentUserId) don't have to hit the DB again for it.
        authentication.setDetails(dbUser.get().getId());
        context.setAuthentication(authentication);

        SecurityContextHolder.setContext(context);

        filterChain.doFilter(request, response);
    }
}
