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

    private enum TokenType {
        ACCESS,
        REFRESH,
        ACCOUNT_ACTIVATION,
        RESET_PASSWORD,
        ACCOUNT_DELETION
    }

    private final String secretKeyString;
    private final String accountActivationSecretKeyString;
    private final String resetPasswordSecretKeyString;
    private final String accountDeletionSecretKeyString;

    @Value("${spring.security.jwt.access-token-exp-time}")
    private String accessTokenExpTime;

    @Value("${spring.security.jwt.refresh-token-exp-time}")
    private String refreshTokenExpTime;

    @Value("${spring.security.jwt.account-deletion-key-exp-time}")
    private String accountDeletionTokenExpTime;

    @Value("${spring.security.jwt.reset-password-key-exp-time}")
    private String resetPasswordTokenExpTime;

    private SecretKey secretKey;
    private SecretKey  accountActivationKey;
    private SecretKey  resetPasswordKey;
    private SecretKey  accountDeletionKey;

    public JwtService(@Value("${spring.security.jwt.secret-key}") String secretKeyString, @Value("${spring.security.jwt.account-activation-key}") String accountActivationSecretKeyString, @Value("${spring.security.jwt.reset-password-key}") String resetPasswordSecretKeyString, @Value("${spring.security.jwt.account-deletion-key}") String accountDeletionSecretKeyString){
        this.secretKeyString = secretKeyString;
        this.accountActivationSecretKeyString = accountActivationSecretKeyString;
        this.resetPasswordSecretKeyString = resetPasswordSecretKeyString;
        this.accountDeletionSecretKeyString = accountDeletionSecretKeyString;
    }

    @PostConstruct
    public void init(){

        byte[] decodedKey = Base64.getDecoder().decode(secretKeyString);
        secretKey = Keys.hmacShaKeyFor(decodedKey);

        byte[] decodedAccountActivationKey = Base64.getDecoder().decode(accountActivationSecretKeyString);
        accountActivationKey = Keys.hmacShaKeyFor(decodedAccountActivationKey);

        byte[] decodedResetPasswordKey = Base64.getDecoder().decode(resetPasswordSecretKeyString);
        resetPasswordKey = Keys.hmacShaKeyFor(decodedResetPasswordKey);

        byte[] decodedAccountDeletionKey = Base64.getDecoder().decode(accountDeletionSecretKeyString);
        accountDeletionKey = Keys.hmacShaKeyFor(decodedAccountDeletionKey);
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

    public String generateActivationToken(String email, int expSeconds) {
        return  generateGeneralToken(email, TokenType.ACCOUNT_ACTIVATION, expSeconds);
    }

    public String generateResetPasswordToken(String email, int expSeconds) {
        return  generateGeneralToken(email, TokenType.RESET_PASSWORD, expSeconds);
    }

    public String generateDeleteAccountToken(String email, int expSeconds) {
        return  generateGeneralToken(email, TokenType.ACCOUNT_DELETION, expSeconds);
    }

    public String generateGeneralToken(String email, TokenType type, int expSeconds){

        Instant now = Instant.now();

        return Jwts
                .builder()
                .subject(email)
                .claim("tokenType", type.name())
                .claim("rememberMe", false)
                .claim("role", "")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expSeconds)))
                .signWith(
                    switch (type) {
                        case ACCOUNT_ACTIVATION -> accountActivationKey;
                        case RESET_PASSWORD -> resetPasswordKey;
                        case ACCOUNT_DELETION -> accountDeletionKey;
                        default -> throw new IllegalArgumentException("Invalid token type: " + type);
                    }
                )
                .compact();
    }

    public SecretKey getSecretKey() {
        return secretKey;
    }

    public SecretKey getAccountActivationKey() {
        return accountActivationKey;
    }

    public SecretKey getResetPasswordKey() {
        return resetPasswordKey;
    }

    public SecretKey getAccountDeletionKey() {
        return accountDeletionKey;
    }

    public String getResetPasswordTokenExpTime() {
        return resetPasswordTokenExpTime;
    }

    public String getAccountDeletionTokenExpTime() {
        return accountDeletionTokenExpTime;
    }
}
