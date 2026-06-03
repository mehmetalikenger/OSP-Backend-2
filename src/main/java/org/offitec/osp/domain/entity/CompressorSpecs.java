package org.offitec.osp.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "compressor_specs")
public class CompressorSpecs {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "comp_specs_seq_gen")
    @SequenceGenerator(name = "comp_specs_seq_gen", sequenceName = "osp_comp_specs_sequence", allocationSize = 50)
    private Long id;

    @JoinColumn(name = "compressor_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Compressor compressor;

    @Column(nullable = false)
    private double capacity;

    @Column(nullable = false)
    private double powerInput;
}
