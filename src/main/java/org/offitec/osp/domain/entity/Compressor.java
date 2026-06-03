package org.offitec.osp.domain.entity;

import jakarta.persistence.*;

@Entity
public class Compressor {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "compressor_seq_gen")
    @SequenceGenerator(name = "compressor_seq_gen", sequenceName = "osp_compressor_sequence", allocationSize = 50)
    private Long id;

    @JoinColumn(name = "compressor_type_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private CompressorType type;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String model;
}
