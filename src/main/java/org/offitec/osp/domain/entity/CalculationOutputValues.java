package org.offitec.osp.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "calc_output_vals")
public class CalculationOutputValues {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "calc_output_vals_seq_gen")
    @SequenceGenerator(name = "calc_output_vals_seq_gen", sequenceName = "osp_calc_output_vals_sequence", allocationSize = 50)
    private Long Id;

    private double refrigerantCapacity;
    private double evaporatorCapacity;
    private double powerInput;
    private double condenserCapacity;
    private double current;
    private double cop_eer;
    private double massFlow;
    private double operatingFrequency;
}
