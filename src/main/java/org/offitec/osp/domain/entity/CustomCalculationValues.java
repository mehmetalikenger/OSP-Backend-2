package org.offitec.osp.domain.entity;

import jakarta.persistence.*;
import lombok.*;

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

    @Column(nullable = false)
    private double ambient;

    @Column(nullable = false)
    private double condensation;

    @Column(nullable = false)
    private double evaporation;

    @Column(nullable = false)
    private double subcooling;

    @Column(nullable = false)
    private double superheat;

    @Column(nullable = false)
    private double evapIn;

    @Column(nullable = false)
    private double evapOut;

    private double condIn;
    private double condOut;
}
