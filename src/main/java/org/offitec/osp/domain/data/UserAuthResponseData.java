package org.offitec.osp.domain.data;

public record UserAuthResponseData(Long id, String role, String accessToken, String refreshToken) {
}
