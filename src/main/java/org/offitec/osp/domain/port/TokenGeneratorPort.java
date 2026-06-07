package org.offitec.osp.domain.port;

public interface TokenGeneratorPort {

    public String generateAccessToken(String email, String role);
    public String generateRefreshToken(String email, String role, Boolean rememberMe);
}
