package org.offitec.osp.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "default_calc_vals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DefaultCalculationValues {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "def_calc_vals_seq_gen")
    @SequenceGenerator(name = "def_calc_vals_seq_gen", sequenceName = "osp_def_calc_vals_sequence", allocationSize = 50)
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

    @Column(nullable = false)
    private double condIn;

    @Column(nullable = false)
    private double condOut;
}
