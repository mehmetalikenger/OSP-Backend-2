package org.offitec.osp.domain.port;

public interface TokenGeneratorPort {

    public String generateAccessToken(String email);
    public String generateRefreshToken(String email, Boolean rememberMe);
}
