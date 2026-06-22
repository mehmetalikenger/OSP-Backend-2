package org.offitec.osp.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "calc_output_vals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CalculationOutputValues {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "calc_output_vals_seq_gen")
    @SequenceGenerator(name = "calc_output_vals_seq_gen", sequenceName = "osp_calc_output_vals_sequence", allocationSize = 50)
    private Long id;

    @Column(name = "refrigerant_capacity")
    private double refrigerantCapacity;

    @Column(name = "evaporator_capacity")
    private double evaporatorCapacity;

    @Column(name = "power_input")
    private double powerInput;

    @Column(name = "condenser_capacity")
    private double condenserCapacity;

    private double current;

    @Column(name = "cop_eer")
    private double copEer;

    @Column(name = "mass_flow")
    private double massFlow;

    @Column(name = "operating_frequency")
    private double operatingFrequency;

    // Pressure drop (kPa): base 50 scaled by the glycol correction. Nullable so rows
    // created before this column existed (NULL) still hydrate.
    @Column(name = "pressure_drop")
    private Double pressureDrop;
}
