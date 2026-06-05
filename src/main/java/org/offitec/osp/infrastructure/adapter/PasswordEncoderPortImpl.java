package org.offitec.osp.infrastructure.adapter;

import org.offitec.osp.domain.port.PasswordEncoderPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordEncoderPortImpl implements PasswordEncoderPort {

    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public String encode(String rawPassword){

        return passwordEncoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String hashedPassword){

        return passwordEncoder.matches(rawPassword, hashedPassword);
    }
}
