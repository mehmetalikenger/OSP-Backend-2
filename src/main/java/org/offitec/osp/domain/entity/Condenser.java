package org.offitec.osp.domain.entity;

import jakarta.persistence.*;

@Entity
public class Condenser {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cond_seq_gen")
    @SequenceGenerator(name = "cond_seq_gen", sequenceName = "osp_cond_sequence")
    private Long id;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String model;
}

