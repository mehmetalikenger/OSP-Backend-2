package org.offitec.osp.domain.entity;

import jakarta.persistence.*;

@Entity
public class Refrigerant {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "refrigerant_seq_gen")
    @SequenceGenerator(name = "refrigerant_seq_gen", sequenceName = "osp_refrigerant_sequence", allocationSize = 50)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String code;
}
