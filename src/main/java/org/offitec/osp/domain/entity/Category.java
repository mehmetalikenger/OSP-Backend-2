package org.offitec.osp.domain.entity;

import jakarta.persistence.*;

@Entity
public class Category {

    private enum UnitCategory {
        CHILLER,
        HEAT_PUMP
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UnitCategory category;
}
