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

    @Column(name = "q_c1", nullable = false) private double qC1;
    @Column(name = "q_c2", nullable = false) private double qC2;
    @Column(name = "q_c3", nullable = false) private double qC3;
    @Column(name = "q_c4", nullable = false) private double qC4;
    @Column(name = "q_c5", nullable = false) private double qC5;
    @Column(name = "q_c6", nullable = false) private double qC6;
    @Column(name = "q_c7", nullable = false) private double qC7;
    @Column(name = "q_c8", nullable = false) private double qC8;
    @Column(name = "q_c9", nullable = false) private double qC9;
    @Column(name = "q_c10", nullable = false) private double qC10;

    @Column(name = "p_c1", nullable = false) private double pC1;
    @Column(name = "p_c2", nullable = false) private double pC2;
    @Column(name = "p_c3", nullable = false) private double pC3;
    @Column(name = "p_c4", nullable = false) private double pC4;
    @Column(name = "p_c5", nullable = false) private double pC5;
    @Column(name = "p_c6", nullable = false) private double pC6;
    @Column(name = "p_c7", nullable = false) private double pC7;
    @Column(name = "p_c8", nullable = false) private double pC8;
    @Column(name = "p_c9", nullable = false) private double pC9;
    @Column(name = "p_c10", nullable = false) private double pC10;
}
