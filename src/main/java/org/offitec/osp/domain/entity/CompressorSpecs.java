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

    // Variable-speed (ISCR) compressors run across a speed range. These are captured only
    // for ISCR compressors and are null otherwise.
    @Column(name = "rpm_base") private Double rpmBase;
    @Column(name = "rpm_min")  private Double rpmMin;
    @Column(name = "rpm_max")  private Double rpmMax;

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

    // Second capacity curve, filled only for ISCR compressors (nullable for all others).
    @Column(name = "q_c11") private Double qC11;
    @Column(name = "q_c12") private Double qC12;
    @Column(name = "q_c13") private Double qC13;
    @Column(name = "q_c14") private Double qC14;
    @Column(name = "q_c15") private Double qC15;
    @Column(name = "q_c16") private Double qC16;
    @Column(name = "q_c17") private Double qC17;
    @Column(name = "q_c18") private Double qC18;
    @Column(name = "q_c19") private Double qC19;
    @Column(name = "q_c20") private Double qC20;

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

    // Second power-input curve, filled only for ISCR compressors (nullable for all others).
    @Column(name = "p_c11") private Double pC11;
    @Column(name = "p_c12") private Double pC12;
    @Column(name = "p_c13") private Double pC13;
    @Column(name = "p_c14") private Double pC14;
    @Column(name = "p_c15") private Double pC15;
    @Column(name = "p_c16") private Double pC16;
    @Column(name = "p_c17") private Double pC17;
    @Column(name = "p_c18") private Double pC18;
    @Column(name = "p_c19") private Double pC19;
    @Column(name = "p_c20") private Double pC20;
}
