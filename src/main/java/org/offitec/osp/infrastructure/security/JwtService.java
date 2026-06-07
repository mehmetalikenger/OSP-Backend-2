package org.offitec.osp.infrastructure.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.offitec.osp.domain.port.TokenGeneratorPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService implements TokenGeneratorPort {

    private final String secretKeyString;

    @Value("${spring.security.jwt.access-token-exp-time}")
    private String accessTokenExpTime;

    @Value("${spring.security.jwt.refresh-token-exp-time}")
    private String refreshTokenExpTime;

    private SecretKey secretKey;

    public JwtService(@Value("${spring.security.jwt.secret-key}") String secretKeyString){
        this.secretKeyString = secretKeyString;
    }

    @PostConstruct
    public void init(){

        byte[] decodedKey = Base64.getDecoder().decode(secretKeyString);
        secretKey = Keys.hmacShaKeyFor(decodedKey);
    }

    @Override
    public String generateAccessToken(String email, String role) {
        return  generateToken(email, role, false, Integer.parseInt(accessTokenExpTime));
    }

    @Override
    public String generateRefreshToken(String email, String role, Boolean rememberMe) {
        return generateToken(email, role, rememberMe, Integer.parseInt(refreshTokenExpTime));
    }

    public String generateToken(String email, String role, Boolean rememberMe, int expSeconds){

        Instant now = Instant.now();

        return Jwts
                .builder()
                .subject(email)
                .claim("role", role)
                .claim("rememberMe", rememberMe)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expSeconds)))
                .signWith(secretKey)
                .compact();
    }
}
