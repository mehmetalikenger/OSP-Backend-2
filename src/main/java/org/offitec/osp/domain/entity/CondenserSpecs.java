package org.offitec.osp.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "condenser_specs")
public class CondenserSpecs {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cond_specs_seq_gen")
    @SequenceGenerator(name = "osp_cond_specs_seqeuence")
    private Long id;

    @JoinColumn(name = "cond_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Condenser condenser;

    @Column(nullable = false)
    private double capacity;
}
