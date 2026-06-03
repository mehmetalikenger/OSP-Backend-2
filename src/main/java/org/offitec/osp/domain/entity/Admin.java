package org.offitec.osp.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class Admin extends User {

    @Column(nullable = false)
    private String surname;
}
