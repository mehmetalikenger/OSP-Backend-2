package org.offitec.osp.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "evaporator_specs")
public class EvaporatorSpecs {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "evap_specs_seq_gen")
    @SequenceGenerator(name = "osp_evap_specs_seqeuence")
    private Long id;

    @JoinColumn(name = "evap_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Evaporator evaporator;

    @Column(nullable = false)
    private double capacity;
}
