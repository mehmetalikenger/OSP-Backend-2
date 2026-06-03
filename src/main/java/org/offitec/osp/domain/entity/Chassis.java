package org.offitec.osp.domain.entity;

import jakarta.persistence.*;

@Entity
public class Chassis {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "chassis_seq_gen")
    @SequenceGenerator(name = "chassis_seq_gen", sequenceName = "osp_chassis_sequence", allocationSize = 50)
    private Long id;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String model;
}
