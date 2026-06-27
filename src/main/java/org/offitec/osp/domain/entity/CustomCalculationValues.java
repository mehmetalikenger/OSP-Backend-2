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

    // Faithful-engine operating inputs, persisted so report regeneration recalculates the exact same
    // point instead of falling back to defaults (50 Hz / 0 K subcooling / 10 K superheat). Nullable so
    // rows created before these columns existed still hydrate.
    @Column(name = "frequency_hz")
    private Double frequencyHz;

    @Column(name = "subcooling")
    private Double subcooling;

    @Column(name = "superheat")
    private Double superheat;

    @Column(name = "suction_gas_temp")
    private Double suctionGasTemp;
}
