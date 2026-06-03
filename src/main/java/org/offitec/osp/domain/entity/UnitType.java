package org.offitec.osp.domain.entity;

import jakarta.persistence.*;

@Entity
public class UnitType {

    private enum Type {
        AW,
        WW
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Type type;
}
