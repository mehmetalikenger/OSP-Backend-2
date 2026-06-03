package org.offitec.osp.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "four_way_reversing_valve")
public class FourWayReversingValve {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "four_way_reversing_valve_seq_gen")
    @SequenceGenerator(name = "four_way_reversing_valve_gen", sequenceName = "osp_four_way_reversing_valve_sequence", allocationSize = 50)
    private Long id;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String model;
}
