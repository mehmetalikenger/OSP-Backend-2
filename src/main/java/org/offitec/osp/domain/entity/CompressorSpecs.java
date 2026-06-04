package org.offitec.osp.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "compressor_specs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompressorSpecs {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "comp_specs_seq_gen")
    @SequenceGenerator(name = "comp_specs_seq_gen", sequenceName = "osp_comp_specs_sequence", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compressor_id", nullable = false)
    private Compressor compressor;

    @Column(nullable = false)
    private double capacity;

    @Column(name = "power_input", nullable = false)
    private double powerInput;
}
