package org.offitec.osp.domain.data;

import org.offitec.osp.domain.enums.UserCategory;

public record UserRegisterData(String email, UserCategory category, String adminEmail) {
}
