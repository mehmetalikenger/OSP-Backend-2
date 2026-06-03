package org.offitec.osp.domain.entity;

import jakarta.persistence.*;

@Entity
public class Evaporator {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "evap_seq_gen")
    @SequenceGenerator(name = "evap_seq_gen", sequenceName = "osp_evap_sequence")
    private Long id;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String model;
}
