package org.offitec.osp.domain.data;

public record UserAuthData(String email, String password, boolean rememberMe) {
}
