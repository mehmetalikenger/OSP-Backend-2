package org.offitec.osp.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "four_way_reversing_valve_specs")
public class FourWayReversingValveSpecs {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "four_way_reversing_valve_specs_seq_gen")
    @SequenceGenerator(name = "osp_four_way_reversing_valve_specs_seqeuence")
    private Long id;

    @JoinColumn(name = "four_way_reversing_valve_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private FourWayReversingValve fourWayReversingValve;

    @Column(nullable = false)
    private double capacity;
}
