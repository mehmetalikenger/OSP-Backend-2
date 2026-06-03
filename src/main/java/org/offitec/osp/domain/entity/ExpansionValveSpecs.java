package org.offitec.osp.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "expansion_valve_specs")
public class ExpansionValveSpecs {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "exp_valve_specs_seq_gen")
    @SequenceGenerator(name = "osp_exp_valve_specs_seqeuence")
    private Long id;

    @JoinColumn(name = "exp_valve_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private ExpansionValve expansionValve;

    @Column(nullable = false)
    private double capacity;
}
