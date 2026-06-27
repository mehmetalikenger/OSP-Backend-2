package org.offitec.osp.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.offitec.osp.domain.enums.Mod;

/**
 * Admin-entered nominal (capacity, power input) of a {@link CompressorRating} for one operating
 * mode. A compressor+refrigerant coef set has one of these per mode (COOLING + HEATING) — these are
 * the compressor's rated duties, distinct from the coefficients (which the engine uses to compute
 * performance at arbitrary operating points). One {@link CompressorRating} → many of these.
 */
@Entity
@Table(name = "compressor_mode_capacity",
        uniqueConstraints = @UniqueConstraint(columnNames = {"compressor_rating_id", "mod"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompressorModeCapacity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "comp_mode_cap_seq_gen")
    @SequenceGenerator(name = "comp_mode_cap_seq_gen", sequenceName = "osp_comp_mode_cap_sequence", allocationSize = 50)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "mod", nullable = false)
    private Mod mod;

    @Column(nullable = false)
    private double capacity;

    @Column(name = "power_input", nullable = false)
    private double powerInput;

    // Upper capacity for variable-speed (ISCR) ratings; null for fixed-speed.
    @Column(name = "max_capacity")
    private Double maxCapacity;
}
