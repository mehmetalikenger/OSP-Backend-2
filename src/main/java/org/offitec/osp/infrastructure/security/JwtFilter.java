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

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.Date;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final String secretKeyString;

    public JwtFilter(@Value("${spring.security.jwt.secret-key}") String secretKeyString){
        this.secretKeyString = secretKeyString;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String token = null;
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        if(token == null){
            filterChain.doFilter(request, response);
            return;
        }

        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKeyString));

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
        Boolean rememberMe = (Boolean) claims.get("rememberMe");

        Date exp = claims.getExpiration();

        String role = claims.get("role", String.class);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        
        java.util.List<org.springframework.security.core.GrantedAuthority> authorities = 
            role != null ? java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority(role)) 
                         : java.util.Collections.emptyList();

        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(email, rememberMe, authorities);
        context.setAuthentication(authentication);

        SecurityContextHolder.setContext(context);

        filterChain.doFilter(request, response);
    }
}
