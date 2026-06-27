package org.offitec.osp.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.offitec.osp.domain.enums.Mod;

@Entity
@Table(name = "custom_calc_vals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomCalculationValues {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "custom_calc_val_seq_gen")
    @SequenceGenerator(name = "custom_calc_val_seq_gen", sequenceName = "osp_custom_calc_val_sequence", allocationSize = 50)
    private Long id;

    // The operating mode these inputs belong to. A heat pump's ProjectDetails holds one row per
    // mode (COOLING + HEATING); a chiller holds a single COOLING row.
    @Enumerated(EnumType.STRING)
    @Column(name = "mod")
    private Mod mod;

    @Column(nullable = false)
    private double ambient;

    @Column(nullable = false)
    private double evapIn;

    @Column(nullable = false)
    private double evapOut;

    private double condIn;
    private double condOut;

    // Glycol mixture selection, persisted so report regeneration can reuse it.
    @Column(name = "mixture_type")
    private String mixtureType;

    @Column(name = "mixture_ratio")
    private Integer mixtureRatio;
}
